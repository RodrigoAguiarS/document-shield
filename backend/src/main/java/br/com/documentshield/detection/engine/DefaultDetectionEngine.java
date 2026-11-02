package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.detection.rule.DetectionRule;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class DefaultDetectionEngine implements DetectionEngine {
    private final DetectionRuleRegistry registry;

    public DefaultDetectionEngine(DetectionRuleRegistry r) {
        registry = r;
    }

    public String version() {
        return "1.0.0";
    }

    public List<RuleExecutionResult> execute(DetectionContext c, Set<String> active) {
        List<RuleExecutionResult> out = new ArrayList<>();
        for (DetectionRule r : registry.all()) {
            if (!active.contains(r.getCode())) {
                out.add(new RuleExecutionResult(r.getCode(), false, false, 0, "Regra desativada", List.of()));
                continue;
            }
            Instant s = Instant.now();
            try {
                DetectionResult x = r.execute(c);
                out.add(new RuleExecutionResult(r.getCode(), true, true, Duration.between(s, Instant.now()).toMillis(),
                        x.limitation(), x.findings()));
            } catch (Exception e) {
                out.add(new RuleExecutionResult(r.getCode(), true, false, Duration.between(s, Instant.now()).toMillis(),
                        safe(e), List.of()));
            }
        }
        return out;
    }

    private String safe(Exception e) {
        String m = e.getMessage();
        return e.getClass().getSimpleName() + (m == null ? "" : ": " + m.substring(0, Math.min(500, m.length())));
    }
}
