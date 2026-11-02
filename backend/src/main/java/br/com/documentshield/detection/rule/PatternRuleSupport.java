package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import br.com.documentshield.util.TextNormalizer;
import java.util.*;
import java.util.regex.*;

final class PatternRuleSupport {
    private PatternRuleSupport() {
    }

    static DetectionResult scan(String code, String text, Pattern pattern, TipoOcorrencia type, NivelRisco risk,
            String title, String description, int score) {
        String n = TextNormalizer.normalize(text);
        Matcher m = pattern.matcher(n);
        List<DetectionFinding> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        while (m.find() && out.size() < 20) {
            String hit = m.group();
            if (seen.add(hit))
                out.add(RuleSupport.text(code, type, risk, title, description,
                        TextNormalizer.excerpt(n, m.start(), m.end()), score, .88, TipoEvidencia.PADRAO_DETECTADO,
                        null));
        }
        return DetectionResult.of(out);
    }
}
