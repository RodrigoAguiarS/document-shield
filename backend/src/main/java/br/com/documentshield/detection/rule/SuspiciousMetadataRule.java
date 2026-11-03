package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.DetectionContext;
import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.DetectionResult;
import br.com.documentshield.detection.model.EvidenceDraft;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoEvidencia;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import br.com.documentshield.util.TextNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousMetadataRule implements DetectionRule {
    private static final Pattern P = Pattern.compile(
            "\\b(ignore|override|desconsidere|system prompt|developer instructions|prompt injection|you are now)\\b",
            Pattern.UNICODE_CASE);

    public String getCode() {
        return "DS-006";
    }

    public DetectionResult execute(DetectionContext c) {
        List<DetectionFinding> o = new ArrayList<>();
        c.extraction().metadata().forEach((k, v) -> {
            if (P.matcher(TextNormalizer.normalize(v)).find()) {
                o.add(new DetectionFinding(
                        getCode(),
                        TipoOcorrencia.METADADO_SUSPEITO,
                        NivelRisco.ALTO,
                        "Metadado com instrução dirigida à IA",
                        "O campo de metadado '" + k + "' contém padrão instrucional.",
                        v,
                        TextNormalizer.normalize(v),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        45,
                        .86,
                        List.of(new EvidenceDraft(
                                TipoEvidencia.METADADO,
                                "Campo de metadado suspeito",
                                k + "=" + v,
                                null,
                                "Metadado de PDDocumentInformation",
                                null))));
            }
        });
        return DetectionResult.of(o);
    }
}
