package br.com.documentshield.dto.response;

import java.time.Instant;
import java.util.*;

public final class MonitoringResponses {
    private MonitoringResponses() {}

    public record Summary(Instant inicio, Instant fim, String granularidade, long totalRequisicoes,
            long totalSucesso, long totalFalhas, double taxaSucesso, double duracaoMediaMs,
            long p95Ms, long integracoesComUso) {}
    public record TemporalPoint(Instant inicio, long totalRequisicoes, long totalSucesso,
            long totalFalhas, double duracaoMediaMs) {}
    public record TimeSeries(Instant inicio, Instant fim, String granularidade, List<TemporalPoint> pontos) {}
    public record Integration(UUID id, String nome, String status, long totalRequisicoes,
            long totalSucesso, long totalFalhas, double taxaSucesso, double duracaoMediaMs,
            long p95Ms, Integer limitePorMinuto) {}
    public record IntegrationList(Instant inicio, Instant fim, List<Integration> integracoes) {}
    public record ErrorItem(String tipo, long quantidade, double percentual) {}
    public record Errors(Instant inicio, Instant fim, long totalFalhas, List<ErrorItem> erros) {}
    public record Capacity(String aplicacao, String bancoDeDados, int concorrenciaConfigurada,
            int execucoesAtivas, int workersDisponiveis, int filaEmUso, int capacidadeFila,
            int capacidadeFilaDisponivel, Instant verificadoEm) {}
}
