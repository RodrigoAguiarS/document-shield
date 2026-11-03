package br.com.documentshield.controller;

import br.com.documentshield.dto.response.MonitoringResponses.Capacity;
import br.com.documentshield.dto.response.MonitoringResponses.Errors;
import br.com.documentshield.dto.response.MonitoringResponses.Integration;
import br.com.documentshield.dto.response.MonitoringResponses.IntegrationList;
import br.com.documentshield.dto.response.MonitoringResponses.Summary;
import br.com.documentshield.dto.response.MonitoringResponses.TimeSeries;
import br.com.documentshield.service.MonitoringQueryService;
import br.com.documentshield.service.MonitoringQueryService.Window;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/monitoramento")
@Tag(name = "Monitoramento administrativo")
public class AdminMonitoringController {
    private final MonitoringQueryService service;

    public AdminMonitoringController(MonitoringQueryService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Retorna os indicadores agregados da API pública")
    public Summary summary(
            @RequestParam(defaultValue = "24H") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String granularidade,
            @RequestParam(required = false) UUID integracaoId) {
        return service.summary(window(periodo, dataInicio, dataFim, granularidade), integracaoId);
    }

    @GetMapping("/serie-temporal")
    @Operation(summary = "Retorna a série temporal agregada da API pública")
    public TimeSeries timeSeries(
            @RequestParam(defaultValue = "24H") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String granularidade,
            @RequestParam(required = false) UUID integracaoId) {
        return service.timeSeries(window(periodo, dataInicio, dataFim, granularidade), integracaoId);
    }

    @GetMapping("/integracoes")
    @Operation(summary = "Compara o uso agregado por integração")
    public IntegrationList integrations(
            @RequestParam(defaultValue = "24H") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String granularidade) {
        return service.integrations(window(periodo, dataInicio, dataFim, granularidade));
    }

    @GetMapping("/integracoes/{id}")
    @Operation(summary = "Retorna métricas agregadas de uma integração")
    public Integration integration(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "24H") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String granularidade) {
        return service.integration(window(periodo, dataInicio, dataFim, granularidade), id);
    }

    @GetMapping("/erros")
    @Operation(summary = "Retorna a distribuição agregada dos erros")
    public Errors errors(
            @RequestParam(defaultValue = "24H") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String granularidade,
            @RequestParam(required = false) UUID integracaoId) {
        return service.errors(window(periodo, dataInicio, dataFim, granularidade), integracaoId);
    }

    @GetMapping("/capacidade")
    @Operation(summary = "Retorna saúde e capacidade operacional da API pública")
    public Capacity capacity() {
        return service.capacity();
    }

    private Window window(String period, Instant start, Instant end, String granularity) {
        return service.resolve(period, start, end, granularity);
    }
}
