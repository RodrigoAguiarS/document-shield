package br.com.documentshield.controller;

import br.com.documentshield.converter.ApiConverter;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.StatusAnalise;
import br.com.documentshield.dto.response.AnaliseResponse;
import br.com.documentshield.dto.response.ExecucaoRegraResponse;
import br.com.documentshield.dto.response.OcorrenciaResponse;
import br.com.documentshield.dto.response.PagedResponse;
import br.com.documentshield.service.QueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analises")
@Tag(name = "Análises")
public class AnaliseController {
    private final QueryService query;
    private final ApiConverter converter;

    public AnaliseController(QueryService q, ApiConverter c) {
        query = q;
        converter = c;
    }

    @GetMapping
    public PagedResponse<AnaliseResponse> list(
            @RequestParam(required = false) StatusAnalise status,
            @RequestParam(required = false) NivelRisco nivelRisco,
            @PageableDefault(size = 20, sort = "dataInicio", direction = Sort.Direction.DESC) Pageable p) {
        return PagedResponse.from(query.analyses(status, nivelRisco, p), converter::toResponse);
    }

    @GetMapping("/{id}")
    public AnaliseResponse get(@PathVariable UUID id) {
        return query.analysis(id);
    }

    @GetMapping("/{id}/ocorrencias")
    public PagedResponse<OcorrenciaResponse> findings(
            @PathVariable UUID id, @PageableDefault(size = 50, sort = "dataIdentificacao") Pageable p) {
        return PagedResponse.from(query.findings(id, p), converter::toResponse);
    }

    @GetMapping("/{id}/execucoes")
    public List<ExecucaoRegraResponse> executions(@PathVariable UUID id) {
        return query.executions(id);
    }
}
