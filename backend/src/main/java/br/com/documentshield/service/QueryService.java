package br.com.documentshield.service;

import br.com.documentshield.converter.ApiConverter;
import br.com.documentshield.domain.entity.*;
import br.com.documentshield.domain.enums.*;
import br.com.documentshield.dto.response.*;
import br.com.documentshield.exception.ResourceNotFoundException;
import br.com.documentshield.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class QueryService {
    private final AnaliseDocumentoRepository analyses;
    private final OcorrenciaRepository findings;
    private final EvidenciaRepository evidence;
    private final ExecucaoRegraRepository executions;
    private final RegraDeteccaoRepository rules;
    private final ApiConverter converter;

    public QueryService(AnaliseDocumentoRepository a, OcorrenciaRepository f, EvidenciaRepository e,
            ExecucaoRegraRepository x, RegraDeteccaoRepository r, ApiConverter c) {
        analyses = a;
        findings = f;
        evidence = e;
        executions = x;
        rules = r;
        converter = c;
    }

    public Page<AnaliseDocumento> analyses(StatusAnalise status, NivelRisco risk, Pageable p) {
        if (status != null && risk != null)
            return analyses.findByStatusAndNivelRisco(status, risk, p);
        if (status != null)
            return analyses.findByStatus(status, p);
        if (risk != null)
            return analyses.findByNivelRisco(risk, p);
        return analyses.findAll(p);
    }

    public Page<AnaliseDocumento> documentAnalyses(UUID id, Pageable p) {
        return analyses.findByDocumentoId(id, p);
    }

    public AnaliseResponse analysis(UUID id) {
        return converter.toResponse(analyses.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Análise não encontrada: " + id)));
    }

    public Page<Ocorrencia> findings(UUID id, Pageable p) {
        if (!analyses.existsById(id))
            throw new ResourceNotFoundException("Análise não encontrada: " + id);
        return findings.findByAnaliseId(id, p);
    }

    public List<ExecucaoRegraResponse> executions(UUID id) {
        if (!analyses.existsById(id))
            throw new ResourceNotFoundException("Análise não encontrada: " + id);
        return executions.findByAnaliseIdOrderByDataExecucaoAsc(id).stream().map(converter::toResponse).toList();
    }

    public OcorrenciaResponse finding(UUID id) {
        return converter.toResponse(findings.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência não encontrada: " + id)));
    }

    public List<EvidenciaResponse> evidence(UUID id) {
        if (!findings.existsById(id))
            throw new ResourceNotFoundException("Ocorrência não encontrada: " + id);
        return evidence.findByOcorrenciaIdOrderByDataRegistroAsc(id).stream().map(converter::toResponse).toList();
    }

    public List<RegraResponse> rules() {
        return rules.findAll(Sort.by("codigo")).stream().map(converter::toResponse).toList();
    }

    public RegraResponse rule(UUID id) {
        return converter.toResponse(
                rules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Regra não encontrada: " + id)));
    }
}
