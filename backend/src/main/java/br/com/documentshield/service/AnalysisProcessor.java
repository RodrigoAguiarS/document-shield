package br.com.documentshield.service;

import br.com.documentshield.detection.engine.DetectionEngine;
import br.com.documentshield.detection.engine.RiskCalculator;
import br.com.documentshield.detection.engine.RuleExecutionResult;
import br.com.documentshield.detection.extractor.PdfDocumentExtractor;
import br.com.documentshield.detection.model.DetectionContext;
import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.PdfExtractionResult;
import br.com.documentshield.domain.entity.AnaliseDocumento;
import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.repository.AnaliseDocumentoRepository;
import br.com.documentshield.repository.DocumentoRepository;
import br.com.documentshield.repository.RegraDeteccaoRepository;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    public AnalysisProcessor(
            AnalysisPersistenceService p,
            AnaliseDocumentoRepository a,
            DocumentoRepository d,
            RegraDeteccaoRepository r,
            PdfDocumentExtractor x,
            DetectionEngine e,
            RiskCalculator c) {
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
            List<RuleExecutionResult> results =
                    engine.execute(new DetectionContext(d.getId(), Path.of(d.getCaminhoArmazenamento()), ex), active);
            List<DetectionFinding> all =
                    results.stream().flatMap(r -> r.findings().stream()).toList();
            persistence.complete(analysisId, results, risk.calculate(all));
        } catch (Exception e) {
            log.error("Falha ao processar análise {}", analysisId, e);
            persistence.fail(
                    analysisId,
                    e.getClass().getSimpleName() + ": "
                            + (e.getMessage() == null ? "falha sem detalhe" : e.getMessage()));
        }
        return CompletableFuture.completedFuture(null);
    }
}
