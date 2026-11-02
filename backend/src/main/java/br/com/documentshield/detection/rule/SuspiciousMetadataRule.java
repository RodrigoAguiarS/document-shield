package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import br.com.documentshield.util.TextNormalizer;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

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
            if (P.matcher(TextNormalizer.normalize(v)).find())
                o.add(new DetectionFinding(getCode(), TipoOcorrencia.METADADO_SUSPEITO, NivelRisco.ALTO,
                        "Metadado com instrução dirigida à IA",
                        "O campo de metadado '" + k + "' contém padrão instrucional.", v, TextNormalizer.normalize(v),
                        null, null, null, null, null, null, null, null, null, 45, .86,
                        List.of(new EvidenceDraft(TipoEvidencia.METADADO, "Campo de metadado suspeito", k + "=" + v,
                                null, "Metadado de PDDocumentInformation", null))));
        });
        return DetectionResult.of(o);
    }
}
