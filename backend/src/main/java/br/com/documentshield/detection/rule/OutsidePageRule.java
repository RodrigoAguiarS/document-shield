package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OutsidePageRule implements DetectionRule {
    public String getCode() {
        return "DS-003";
    }

    public DetectionResult execute(DetectionContext c) {
        List<DetectionFinding> o = new ArrayList<>();
        for (PdfTextElement e : c.extraction().elements()) {
            boolean outside = e.x() + e.width() < 0 || e.y() + e.height() < 0 || e.x() > e.pageWidth()
                    || e.y() > e.pageHeight();
            if (!e.text().isBlank() && outside) {
                o.add(RuleSupport.element(getCode(), TipoOcorrencia.TEXTO_FORA_DA_PAGINA, NivelRisco.MEDIO,
                        "Texto fora da área visível",
                        "A caixa do texto está fora do crop box da página; rotação registrada: " + e.rotation() + "°.",
                        e, 24, .85, TipoEvidencia.POSICIONAMENTO,
                        "Caixa: x=" + e.x() + ", y=" + e.y() + ", w=" + e.width() + ", h=" + e.height() + "; página="
                                + e.pageWidth() + "x" + e.pageHeight()));
                if (o.size() >= 50)
                    break;
            }
        }
        return DetectionResult.of(o);
    }
}
