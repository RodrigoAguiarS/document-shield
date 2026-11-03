package br.com.documentshield.detection.model;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.util.List;

public record DetectionFinding(
        String ruleCode,
        TipoOcorrencia type,
        NivelRisco risk,
        String title,
        String description,
        String suspiciousContent,
        String normalizedContent,
        Integer page,
        Float x,
        Float y,
        Float width,
        Float height,
        Float pageWidth,
        Float pageHeight,
        Integer pageRotation,
        String coordinateSpace,
        Boolean reliableLocation,
        String font,
        Float fontSize,
        String color,
        Float opacity,
        int baseScore,
        double confidence,
        List<EvidenceDraft> evidence) {}
