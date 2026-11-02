package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class ConcealmentRule implements DetectionRule {
    private static final Pattern P = Pattern.compile(
            "\\b(hide|conceal|oculte|esconda|do not reveal|nao revele|disable|desabilite|bypass|ignore)\\b.{0,40}\\b(instructions?|commands?|validation|checks?|instrucoes?|comandos?|validacoes?|verificacoes?)\\b",
            Pattern.UNICODE_CASE);

    public String getCode() {
        return "DS-008";
    }

    public DetectionResult execute(DetectionContext c) {
        return PatternRuleSupport.scan(getCode(), c.extraction().plainText(), P, TipoOcorrencia.TEXTO_OFUSCADO,
                NivelRisco.ALTO, "Tentativa de ocultação ou evasão",
                "Instrução solicita ocultar comandos ou contornar validações/verificações.", 50);
    }
}
