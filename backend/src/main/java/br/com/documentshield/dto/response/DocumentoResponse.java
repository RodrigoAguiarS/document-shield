package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.StatusDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "Documento")
public record DocumentoResponse(UUID id, String nomeOriginal, String tipoMime, String extensao, Long tamanhoBytes,
        String hashSha256, Integer quantidadePaginas, StatusDocumento status, Instant dataUpload,
        Instant dataAtualizacao, String mensagemErro) {
}
