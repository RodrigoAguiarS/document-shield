package br.com.documentshield.controller;

import br.com.documentshield.converter.ApiConverter;
import br.com.documentshield.domain.enums.*;
import br.com.documentshield.dto.response.*;
import br.com.documentshield.service.QueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import java.util.*;

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
    public PagedResponse<AnaliseResponse> list(@RequestParam(required = false) StatusAnalise status,
            @RequestParam(required = false) NivelRisco nivelRisco,
            @PageableDefault(size = 20, sort = "dataInicio", direction = Sort.Direction.DESC) Pageable p) {
        return PagedResponse.from(query.analyses(status, nivelRisco, p), converter::toResponse);
    }

    @GetMapping("/{id}")
    public AnaliseResponse get(@PathVariable UUID id) {
        return query.analysis(id);
    }

    @GetMapping("/{id}/ocorrencias")
    public PagedResponse<OcorrenciaResponse> findings(@PathVariable UUID id,
            @PageableDefault(size = 50, sort = "dataIdentificacao") Pageable p) {
        return PagedResponse.from(query.findings(id, p), converter::toResponse);
    }

    @GetMapping("/{id}/execucoes")
    public List<ExecucaoRegraResponse> executions(@PathVariable UUID id) {
        return query.executions(id);
    }
}
