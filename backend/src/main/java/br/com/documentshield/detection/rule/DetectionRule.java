package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.DetectionContext;
import br.com.documentshield.detection.model.DetectionResult;

public interface DetectionRule {
    String getCode();

    DetectionResult execute(DetectionContext context);
}
