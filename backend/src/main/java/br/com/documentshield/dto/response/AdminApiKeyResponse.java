package br.com.documentshield.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminApiKeyResponse(UUID id, String nome, String descricao, String identificador,
        String prefixo, String escopo, String status, Instant criadaEm, Instant expiraEm,
        Instant ultimoUsoEm, Instant revogadaEm, Integer requisicoesPorMinuto) {}
