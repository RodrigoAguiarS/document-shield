package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.rule.DetectionRule;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DetectionRuleRegistry {
    private final Map<String, DetectionRule> rules;

    public DetectionRuleRegistry(List<DetectionRule> all) {
        Map<String, DetectionRule> m = new TreeMap<>();
        all.forEach(r -> {
            if (m.put(r.getCode(), r) != null)
                throw new IllegalStateException("Código de regra duplicado: " + r.getCode());
        });
        rules = Map.copyOf(m);
    }

    public Optional<DetectionRule> find(String c) {
        return Optional.ofNullable(rules.get(c));
    }

    public Collection<DetectionRule> all() {
        return rules.values();
    }
}
