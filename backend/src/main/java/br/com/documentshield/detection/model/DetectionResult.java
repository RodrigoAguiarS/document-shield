package br.com.documentshield.detection.model;

import java.util.List;

public record DetectionResult(List<DetectionFinding> findings, boolean supported, String limitation) {
    public DetectionResult {
        findings = List.copyOf(findings);
    }

    public static DetectionResult of(List<DetectionFinding> f) {
        return new DetectionResult(f, true, null);
    }
}
