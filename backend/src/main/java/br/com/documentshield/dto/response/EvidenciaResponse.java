package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.TipoEvidencia;
import java.time.Instant;
import java.util.UUID;

public record EvidenciaResponse(UUID id, UUID ocorrenciaId, TipoEvidencia tipo, String descricao,
        String valorEncontrado, String valorEsperado, String detalhesTecnicos, Integer numeroPagina,
        Instant dataRegistro) {
}
