package br.com.documentshield.dto.request;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 500) String descricao,
        @Future Instant expiraEm,
        @NotBlank String escopo,
        @Min(1) @Max(10000) Integer requisicoesPorMinuto) {}
