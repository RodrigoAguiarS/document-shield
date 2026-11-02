package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.*;
import java.util.UUID;

public record UploadResponse(UUID documentoId, UUID analiseId, StatusDocumento statusDocumento,
        StatusAnalise statusAnalise, String consultaAnalise) {
}
