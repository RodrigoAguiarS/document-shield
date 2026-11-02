package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;

public interface DetectionRule {
    String getCode();

    DetectionResult execute(DetectionContext context);
}
