package br.com.documentshield.detection.model;

import br.com.documentshield.domain.enums.TipoEvidencia;

public record EvidenceDraft(TipoEvidencia type, String description, String found, String expected,
        String technicalDetails, Integer page) {
}
