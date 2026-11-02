package br.com.documentshield.service;

import br.com.documentshield.detection.engine.*;
import br.com.documentshield.detection.extractor.PdfDocumentExtractor;
import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.dto.response.PublicAnalysisResponse;
import br.com.documentshield.dto.response.PublicAnalysisResponse.*;
import br.com.documentshield.exception.*;
import br.com.documentshield.repository.RegraDeteccaoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PublicDocumentAnalysisService {
    private final PdfDocumentExtractor extractor;
    private final DetectionEngine engine;
    private final RiskCalculator riskCalculator;
    private final RegraDeteccaoRepository rules;
    private final ThreadPoolExecutor executor;
    private final Path tempRoot;
    private final long maxFileSize;
    private final Duration timeout;
    private final int maxOccurrences;
    private final int maxEvidence;
    private final int maxText;

    public PublicDocumentAnalysisService(PdfDocumentExtractor extractor, DetectionEngine engine,
            RiskCalculator riskCalculator, RegraDeteccaoRepository rules,
            @Qualifier("publicAnalysisExecutor") ThreadPoolExecutor executor,
            @Value("${document-shield.public-api.temp-directory:${java.io.tmpdir}/document-shield-public}") String tempDirectory,
            @Value("${document-shield.public-api.max-file-size:20971520}") long maxFileSize,
            @Value("${document-shield.public-api.timeout-seconds:30}") long timeoutSeconds,
            @Value("${document-shield.public-api.max-occurrences:200}") int maxOccurrences,
            @Value("${document-shield.public-api.max-evidence-per-occurrence:50}") int maxEvidence,
            @Value("${document-shield.public-api.max-text-length:2000}") int maxText) {
        this.extractor = extractor;
        this.engine = engine;
        this.riskCalculator = riskCalculator;
        this.rules = rules;
        this.executor = executor;
        this.tempRoot = Path.of(tempDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.maxOccurrences = Math.max(1, maxOccurrences);
        this.maxEvidence = Math.max(1, maxEvidence);
        this.maxText = Math.max(256, maxText);
    }

    @PostConstruct
    void initializeTemporaryStorage() {
        try {
            Files.createDirectories(tempRoot);
            Instant cutoff = Instant.now().minus(Duration.ofHours(1));
            try (var files = Files.list(tempRoot)) {
                files.filter(p -> p.getFileName().toString().startsWith("public-analysis-") && p.getFileName().toString().endsWith(".pdf"))
                        .filter(p -> lastModified(p).isBefore(cutoff)).forEach(this::deleteQuietly);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível preparar o armazenamento temporário da API pública", e);
        }
    }

    public PublicAnalysisResponse analyze(MultipartFile file) {
        PreparedDocument prepared = prepare(file);
        UUID correlationId = UUID.randomUUID();
        Future<PublicAnalysisResponse> future;
        try {
            future = executor.submit(() -> process(correlationId, prepared));
        } catch (RejectedExecutionException e) {
            deleteQuietly(prepared.path());
            throw new ProcessingCapacityException("A capacidade da análise pública está esgotada; tente novamente mais tarde.");
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // O worker permanece isolado e removerá o temporário ao encerrar. Interrupção não é usada como falsa garantia.
            throw new PublicAnalysisTimeoutException("A análise excedeu o limite de " + timeout.toSeconds() + " segundos.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingCapacityException("A análise foi interrompida antes da conclusão.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new InvalidDocumentException("PDF inválido, corrompido ou protegido por senha", cause);
        }
    }

    private PublicAnalysisResponse process(UUID correlationId, PreparedDocument document) {
        Instant started = Instant.now();
        try {
            PdfExtractionResult extraction = extractor.extract(document.path());
            Set<String> activeCodes = new HashSet<>();
            rules.findByAtivaTrueOrderByCodigoAsc().forEach(rule -> activeCodes.add(rule.getCodigo()));
            List<RuleExecutionResult> executions = engine.execute(
                    new DetectionContext(correlationId, document.path(), extraction), activeCodes);
            List<DetectionFinding> findings = executions.stream().flatMap(result -> result.findings().stream()).toList();
            RiskAssessment risk = riskCalculator.calculate(findings);
            boolean[] truncated = { findings.size() > maxOccurrences };
            List<PublicOccurrenceResponse> occurrences = findings.stream().limit(maxOccurrences)
                    .map(finding -> occurrence(finding, truncated)).toList();
            List<PublicRuleExecutionResponse> executionResponses = executions.stream()
                    .map(result -> new PublicRuleExecutionResponse(result.code(), result.executed(), result.success(),
                            result.durationMillis(), result.findings().size(),
                            result.success() || !result.executed() ? limit(result.error(), truncated) : "Falha na execução da regra"))
                    .toList();
            long duration = Duration.between(started, Instant.now()).toMillis();
            return new PublicAnalysisResponse("CONCLUIDA", correlationId,
                    new PublicDocumentResponse(document.name(), "application/pdf", document.size(), document.sha256()),
                    extraction.pages().size(), engine.version(), risk.level(), risk.score(), risk.summary(), findings.size(),
                    count(findings, NivelRisco.CRITICO), count(findings, NivelRisco.ALTO),
                    count(findings, NivelRisco.MEDIO), count(findings, NivelRisco.BAIXO),
                    count(findings, NivelRisco.INFORMATIVO),
                    (int) executions.stream().filter(RuleExecutionResult::executed).count(), duration,
                    truncated[0], executionResponses, occurrences);
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidDocumentException invalid) throw invalid;
            throw new InvalidDocumentException("PDF inválido, corrompido ou protegido por senha", e);
        } finally {
            deleteQuietly(document.path());
        }
    }

    private PublicOccurrenceResponse occurrence(DetectionFinding finding, boolean[] truncated) {
        List<PublicEvidenceResponse> evidence = finding.evidence().stream().limit(maxEvidence)
                .map(item -> new PublicEvidenceResponse(item.type(), limit(item.description(), truncated),
                        limit(item.found(), truncated), limit(item.expected(), truncated),
                        limit(item.technicalDetails(), truncated), item.page())).toList();
        if (finding.evidence().size() > maxEvidence) truncated[0] = true;
        return new PublicOccurrenceResponse(UUID.randomUUID(), finding.ruleCode(), finding.type(), finding.risk(),
                limit(finding.title(), truncated), limit(finding.description(), truncated),
                limit(finding.suspiciousContent(), truncated), limit(finding.normalizedContent(), truncated),
                finding.page(), finding.x(), finding.y(), finding.width(), finding.height(), finding.font(),
                finding.fontSize(), finding.color(), finding.opacity(),
                (int) Math.round(finding.baseScore() * finding.confidence()), evidence);
    }

    private PreparedDocument prepare(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("O arquivo PDF é obrigatório.");
        if (file.getSize() > maxFileSize) throw new PayloadTooLargeException("O arquivo excede o limite de 20 MB.");
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new UnsupportedDocumentTypeException("Apenas documentos PDF são suportados.");
        }
        Path temp = null;
        try {
            temp = Files.createTempFile(tempRoot, "public-analysis-", ".pdf");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            byte[] first = new byte[5];
            int firstCount = 0;
            try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > maxFileSize) throw new PayloadTooLargeException("O arquivo excede o limite de 20 MB.");
                    if (firstCount < first.length) {
                        int take = Math.min(read, first.length - firstCount);
                        System.arraycopy(buffer, 0, first, firstCount, take);
                        firstCount += take;
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            if (firstCount < 5 || first[0] != '%' || first[1] != 'P' || first[2] != 'D' || first[3] != 'F' || first[4] != '-')
                throw new InvalidDocumentException("Assinatura PDF inválida.");
            return new PreparedDocument(temp, safeName(file.getOriginalFilename()), total,
                    HexFormat.of().formatHex(digest.digest()));
        } catch (PayloadTooLargeException | InvalidDocumentException e) {
            deleteQuietly(temp);
            throw e;
        } catch (Exception e) {
            deleteQuietly(temp);
            throw new InvalidDocumentException("Não foi possível receber o PDF.", e);
        }
    }

    private int count(List<DetectionFinding> findings, NivelRisco level) {
        return (int) findings.stream().filter(finding -> finding.risk() == level).count();
    }

    private String limit(String value, boolean[] truncated) {
        if (value == null || value.length() <= maxText) return value;
        truncated[0] = true;
        return value.substring(0, maxText);
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "documento.pdf";
        String safe = name.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "_");
        return safe.substring(0, Math.min(255, safe.length()));
    }

    private Instant lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toInstant(); }
        catch (IOException e) { return Instant.MAX; }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(tempRoot)) return;
        try { Files.deleteIfExists(normalized); } catch (IOException ignored) {}
    }

    private record PreparedDocument(Path path, String name, long size, String sha256) {}
}
