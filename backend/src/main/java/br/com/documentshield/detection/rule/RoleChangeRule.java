package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class RoleChangeRule implements DetectionRule {
    private static final Pattern P = Pattern
            .compile("(^|\\s)(system|developer|assistant|sistema|desenvolvedor|assistente)\\s*:", Pattern.UNICODE_CASE);

    public String getCode() {
        return "DS-007";
    }

    public DetectionResult execute(DetectionContext c) {
        return PatternRuleSupport.scan(getCode(), c.extraction().plainText(), P, TipoOcorrencia.INSTRUCAO_IA,
                NivelRisco.ALTO, "Simulação de papel privilegiado",
                "O documento contém marcador que simula mensagem SYSTEM, DEVELOPER ou ASSISTANT.", 48);
    }
}
