package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.model.DetectionContext;
import java.util.*;

public interface DetectionEngine {
    List<RuleExecutionResult> execute(DetectionContext context, Set<String> activeCodes);

    String version();
}
