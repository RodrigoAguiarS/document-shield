package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.rule.DetectionRule;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class DetectionRuleRegistry {
    private final Map<String, DetectionRule> rules;

    public DetectionRuleRegistry(List<DetectionRule> all) {
        Map<String, DetectionRule> m = new TreeMap<>();
        all.forEach(r -> {
            if (m.put(r.getCode(), r) != null) {
                throw new IllegalStateException("Código de regra duplicado: " + r.getCode());
            }
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
