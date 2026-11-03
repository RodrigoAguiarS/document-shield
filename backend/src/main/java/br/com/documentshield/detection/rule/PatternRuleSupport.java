package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.DetectionResult;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoEvidencia;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import br.com.documentshield.util.TextNormalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PatternRuleSupport {
    private PatternRuleSupport() {}

    static DetectionResult scan(
            String code,
            String text,
            Pattern pattern,
            TipoOcorrencia type,
            NivelRisco risk,
            String title,
            String description,
            int score) {
        String n = TextNormalizer.normalize(text);
        Matcher m = pattern.matcher(n);
        List<DetectionFinding> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        while (m.find() && out.size() < 20) {
            String hit = m.group();
            if (seen.add(hit)) {
                out.add(RuleSupport.text(
                        code,
                        type,
                        risk,
                        title,
                        description,
                        TextNormalizer.excerpt(n, m.start(), m.end()),
                        score,
                        .88,
                        TipoEvidencia.PADRAO_DETECTADO,
                        null));
            }
        }
        return DetectionResult.of(out);
    }
}
