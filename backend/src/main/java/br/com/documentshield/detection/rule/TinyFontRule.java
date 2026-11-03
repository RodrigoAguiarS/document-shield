package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.DetectionContext;
import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.DetectionResult;
import br.com.documentshield.detection.model.PdfTextElement;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoEvidencia;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TinyFontRule implements DetectionRule {
    public String getCode() {
        return "DS-001";
    }

    public DetectionResult execute(DetectionContext c) {
        List<DetectionFinding> out = new ArrayList<>();
        for (PdfTextElement e : c.extraction().elements()) {
            if (e.fontSize() != null && e.fontSize() < 2f && !e.text().isBlank()) {
                out.add(RuleSupport.element(
                        getCode(),
                        TipoOcorrencia.FONTE_MINUSCULA,
                        NivelRisco.BAIXO,
                        "Fonte extremamente pequena",
                        "Texto renderizado com " + e.fontSize()
                                + " pt, abaixo do limiar de 2 pt. A anomalia isolada não caracteriza ataque.",
                        e,
                        12,
                        .85,
                        TipoEvidencia.PROPRIEDADE_VISUAL,
                        "Tamanho de fonte medido pelo PDFBox"));
                if (out.size() >= 50) {
                    break;
                }
            }
        }
        return DetectionResult.of(out);
    }
}
