package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class InvisibleTextRule implements DetectionRule {
    public String getCode() {
        return "DS-002";
    }

    public DetectionResult execute(DetectionContext c) {
        List<DetectionFinding> o = new ArrayList<>();
        for (PdfTextElement e : c.extraction().elements()) {
            boolean alpha = e.opacity() != null && e.opacity() < .05f;
            if (!e.text().isBlank() && (e.renderingInvisible() || alpha)) {
                String why = e.renderingInvisible() ? "modo de renderização sem preenchimento/contorno"
                        : "opacidade inferior a 5%";
                o.add(RuleSupport.element(getCode(), TipoOcorrencia.TEXTO_INVISIVEL, NivelRisco.MEDIO,
                        "Texto potencialmente invisível",
                        "Texto usa " + why + ". Cor branca isoladamente não é tratada como invisibilidade.", e, 28, .9,
                        TipoEvidencia.PROPRIEDADE_VISUAL, why));
                if (o.size() >= 50)
                    break;
            }
        }
        return DetectionResult.of(o);
    }
}
