package br.com.documentshield.dto.response;

public record DashboardResponse(
        long totalDocumentos,
        long totalAnalises,
        long analisesEmAndamento,
        long analisesConcluidas,
        long analisesComFalha,
        long totalOcorrencias,
        long documentosComRiscoAlto,
        long documentosComRiscoCritico) {}
