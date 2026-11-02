package br.com.documentshield.service;

import br.com.documentshield.detection.engine.*;
import br.com.documentshield.detection.extractor.PdfDocumentExtractor;
import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.entity.*;
import br.com.documentshield.repository.*;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class AnalysisProcessor {
    private static final Logger log = LoggerFactory.getLogger(AnalysisProcessor.class);
    private final AnalysisPersistenceService persistence;
    private final AnaliseDocumentoRepository analyses;
    private final DocumentoRepository documents;
    private final RegraDeteccaoRepository rules;
    private final PdfDocumentExtractor extractor;
    private final DetectionEngine engine;
    private final RiskCalculator risk;

    public AnalysisProcessor(AnalysisPersistenceService p, AnaliseDocumentoRepository a, DocumentoRepository d,
            RegraDeteccaoRepository r, PdfDocumentExtractor x, DetectionEngine e, RiskCalculator c) {
        persistence = p;
        analyses = a;
        documents = d;
        rules = r;
        extractor = x;
        engine = e;
        risk = c;
    }

    @Async("analysisExecutor")
    public CompletableFuture<Void> process(UUID analysisId) {
        try {
            persistence.start(analysisId);
            AnaliseDocumento a = analyses.findById(analysisId).orElseThrow();
            Documento d = documents.findById(a.getDocumentoId()).orElseThrow();
            PdfExtractionResult ex = extractor.extract(Path.of(d.getCaminhoArmazenamento()));
            Set<String> active = new HashSet<>();
            rules.findByAtivaTrueOrderByCodigoAsc().forEach(r -> active.add(r.getCodigo()));
            List<RuleExecutionResult> results = engine
                    .execute(new DetectionContext(d.getId(), Path.of(d.getCaminhoArmazenamento()), ex), active);
            List<DetectionFinding> all = results.stream().flatMap(r -> r.findings().stream()).toList();
            persistence.complete(analysisId, results, risk.calculate(all));
        } catch (Exception e) {
            log.error("Falha ao processar análise {}", analysisId, e);
            persistence.fail(analysisId, e.getClass().getSimpleName() + ": "
                    + (e.getMessage() == null ? "falha sem detalhe" : e.getMessage()));
        }
        return CompletableFuture.completedFuture(null);
    }
}
