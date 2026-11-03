package br.com.documentshield.service;

import br.com.documentshield.detection.engine.RiskAssessment;
import br.com.documentshield.detection.engine.RuleExecutionResult;
import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.EvidenceDraft;
import br.com.documentshield.domain.entity.AnaliseDocumento;
import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.domain.entity.Evidencia;
import br.com.documentshield.domain.entity.ExecucaoRegra;
import br.com.documentshield.domain.entity.Ocorrencia;
import br.com.documentshield.domain.entity.RegraDeteccao;
import br.com.documentshield.domain.enums.EstadoExecucaoRegra;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.StatusAnalise;
import br.com.documentshield.domain.enums.StatusDocumento;
import br.com.documentshield.repository.AnaliseDocumentoRepository;
import br.com.documentshield.repository.DocumentoRepository;
import br.com.documentshield.repository.EvidenciaRepository;
import br.com.documentshield.repository.ExecucaoRegraRepository;
import br.com.documentshield.repository.OcorrenciaRepository;
import br.com.documentshield.repository.RegraDeteccaoRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisPersistenceService {
    private final AnaliseDocumentoRepository analyses;
    private final DocumentoRepository documents;
    private final OcorrenciaRepository findings;
    private final EvidenciaRepository evidence;
    private final ExecucaoRegraRepository executions;
    private final RegraDeteccaoRepository rules;

    public AnalysisPersistenceService(
            AnaliseDocumentoRepository a,
            DocumentoRepository d,
            OcorrenciaRepository f,
            EvidenciaRepository e,
            ExecucaoRegraRepository x,
            RegraDeteccaoRepository r) {
        analyses = a;
        documents = d;
        findings = f;
        evidence = e;
        executions = x;
        rules = r;
    }

    @Transactional
    public AnaliseDocumento create(UUID documentId, String version) {
        return analyses.save(AnaliseDocumento.builder()
                .documentoId(documentId)
                .status(StatusAnalise.PENDENTE)
                .versaoMotor(version)
                .build());
    }

    @Transactional
    public void start(UUID id) {
        AnaliseDocumento a = get(id);
        a.setStatus(StatusAnalise.EXECUTANDO);
        a.setDataInicio(Instant.now());
        a.setMensagemErro(null);
        analyses.save(a);
    }

    @Transactional
    public void complete(UUID id, List<RuleExecutionResult> results, RiskAssessment risk) {
        AnaliseDocumento a = get(id);
        Map<String, RegraDeteccao> byCode = new HashMap<>();
        rules.findAll().forEach(r -> byCode.put(r.getCodigo(), r));
        List<DetectionFinding> all = new ArrayList<>();
        for (RuleExecutionResult rr : results) {
            RegraDeteccao rule = byCode.get(rr.code());
            if (rule == null) {
                continue;
            }
            EstadoExecucaoRegra state = !rr.executed()
                    ? EstadoExecucaoRegra.NAO_EXECUTADA
                    : (rr.success() ? EstadoExecucaoRegra.EXECUTADA_COM_SUCESSO : EstadoExecucaoRegra.FALHOU);
            executions.save(ExecucaoRegra.builder()
                    .analiseId(id)
                    .regraId(rule.getId())
                    .estado(state)
                    .executada(rr.executed())
                    .sucesso(rr.success())
                    .quantidadeOcorrencias(rr.findings().size())
                    .duracaoMilissegundos(rr.durationMillis())
                    .mensagemErro(sanitizeText(rr.error(), 2000))
                    .build());
            for (DetectionFinding f : rr.findings()) {
                all.add(f);
                Ocorrencia o = findings.save(Ocorrencia.builder()
                        .analiseId(id)
                        .regraId(rule.getId())
                        .tipo(f.type())
                        .nivelRisco(f.risk())
                        .titulo(sanitizeText(f.title(), 255))
                        .descricao(sanitizeText(f.description(), 2000))
                        .conteudoSuspeito(sanitizeText(f.suspiciousContent(), 10000))
                        .conteudoNormalizado(sanitizeText(f.normalizedContent(), 10000))
                        .numeroPagina(f.page())
                        .posicaoX(f.x())
                        .posicaoY(f.y())
                        .largura(f.width())
                        .altura(f.height())
                        .fonte(sanitizeText(f.font(), 255))
                        .tamanhoFonte(f.fontSize())
                        .corTexto(sanitizeText(f.color(), 64))
                        .opacidade(f.opacity())
                        .pontuacao((int) Math.round(f.baseScore() * f.confidence()))
                        .build());
                for (EvidenceDraft ev : f.evidence()) {
                    evidence.save(Evidencia.builder()
                            .ocorrenciaId(o.getId())
                            .tipo(ev.type())
                            .descricao(sanitizeText(ev.description(), 2000))
                            .valorEncontrado(sanitizeText(ev.found(), 10000))
                            .valorEsperado(sanitizeText(ev.expected(), 10000))
                            .detalhesTecnicos(sanitizeText(ev.technicalDetails(), 10000))
                            .numeroPagina(ev.page())
                            .build());
                }
            }
        }
        Instant end = Instant.now();
        a.setStatus(StatusAnalise.CONCLUIDA);
        a.setNivelRisco(risk.level());
        a.setPontuacaoRisco(risk.score());
        a.setTotalOcorrencias(all.size());
        a.setTotalOcorrenciasCriticas(count(all, NivelRisco.CRITICO));
        a.setTotalOcorrenciasAltas(count(all, NivelRisco.ALTO));
        a.setTotalOcorrenciasMedias(count(all, NivelRisco.MEDIO));
        a.setTotalOcorrenciasBaixas(count(all, NivelRisco.BAIXO));
        a.setTotalRegrasExecutadas(
                (int) results.stream().filter(RuleExecutionResult::executed).count());
        a.setResumo(sanitizeText(risk.summary(), 2000));
        a.setDataFim(end);
        a.setDuracaoMilissegundos(
                a.getDataInicio() == null
                        ? null
                        : Duration.between(a.getDataInicio(), end).toMillis());
        analyses.save(a);
        Documento d = documents.findById(a.getDocumentoId()).orElseThrow();
        d.setStatus(StatusDocumento.PRONTO);
        d.setMensagemErro(null);
        documents.save(d);
    }

    @Transactional
    public void fail(UUID id, String message) {
        AnaliseDocumento a = get(id);
        Instant end = Instant.now();
        a.setStatus(StatusAnalise.FALHOU);
        a.setMensagemErro(sanitizeText(message, 1900));
        a.setDataFim(end);
        a.setDuracaoMilissegundos(
                a.getDataInicio() == null
                        ? null
                        : Duration.between(a.getDataInicio(), end).toMillis());
        analyses.save(a);
        documents.findById(a.getDocumentoId()).ifPresent(d -> {
            d.setStatus(StatusDocumento.ERRO);
            d.setMensagemErro("Falha na análise; consulte o histórico.");
            documents.save(d);
        });
    }

    private AnaliseDocumento get(UUID id) {
        return analyses.findById(id).orElseThrow();
    }

    private int count(List<DetectionFinding> f, NivelRisco n) {
        return (int) f.stream().filter(x -> x.risk() == n).count();
    }

    static String sanitizeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replace('\u0000', '\uFFFD');
        return sanitized.substring(0, Math.min(maxLength, sanitized.length()));
    }
}
