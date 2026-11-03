package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.model.DetectionContext;
import java.util.List;
import java.util.Set;

public interface DetectionEngine {
    List<RuleExecutionResult> execute(DetectionContext context, Set<String> activeCodes);

    String version();
}
