package br.com.documentshield.detection.engine;

import br.com.documentshield.domain.enums.NivelRisco;

public record RiskAssessment(int score, NivelRisco level, String summary) {}
