package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.model.DetectionFinding;
import java.util.List;

public record RuleExecutionResult(String code, boolean executed, boolean success, long durationMillis, String error,
        List<DetectionFinding> findings) {
}
