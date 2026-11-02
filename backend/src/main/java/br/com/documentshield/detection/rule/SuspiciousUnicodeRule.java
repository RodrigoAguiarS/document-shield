package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SuspiciousUnicodeRule implements DetectionRule {
    private static final Set<Integer> S = Set.of(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF, 0x202A, 0x202B, 0x202D, 0x202E,
            0x202C, 0x2066, 0x2067, 0x2068, 0x2069);

    public String getCode() {
        return "DS-004";
    }

    public DetectionResult execute(DetectionContext c) {
        List<DetectionFinding> o = new ArrayList<>();
        for (PdfTextElement e : c.extraction().elements()) {
            List<String> cps = e.text().codePoints().filter(
                    cp -> S.contains(cp) || (Character.isISOControl(cp) && cp != '\n' && cp != '\r' && cp != '\t'))
                    .mapToObj(cp -> String.format("U+%04X", cp)).toList();
            if (!cps.isEmpty()) {
                o.add(RuleSupport.element(getCode(), TipoOcorrencia.UNICODE_SUSPEITO, NivelRisco.MEDIO,
                        "Unicode invisível ou direcional",
                        "Foram encontrados caracteres de formatação invisível/controle: " + String.join(", ", cps)
                                + ". O idioma do texto, isoladamente, não é considerado suspeito.",
                        e, 22, .8, TipoEvidencia.CARACTERE_UNICODE, String.join(", ", cps)));
                if (o.size() >= 50)
                    break;
            }
        }
        return DetectionResult.of(o);
    }
}
