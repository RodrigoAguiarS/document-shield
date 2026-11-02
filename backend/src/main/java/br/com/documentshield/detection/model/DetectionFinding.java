package br.com.documentshield.detection.model;

import br.com.documentshield.domain.enums.*;
import java.util.List;

public record DetectionFinding(String ruleCode, TipoOcorrencia type, NivelRisco risk, String title, String description,
        String suspiciousContent, String normalizedContent, Integer page, Float x, Float y, Float width, Float height,
        String font,
        Float fontSize, String color, Float opacity, int baseScore, double confidence, List<EvidenceDraft> evidence) {
}
