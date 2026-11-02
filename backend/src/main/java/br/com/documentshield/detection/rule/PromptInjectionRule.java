package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.*;
import br.com.documentshield.domain.enums.*;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class PromptInjectionRule implements DetectionRule {
    private static final Pattern P = Pattern.compile(
            "\\b(ignore|override|disregard|desconsidere|ignore|substitua)\\b.{0,45}\\b(previous|prior|anteriores?|instrucoes?|orientacoes?)\\b|\\byou are now\\b|\\b(do not reveal|nao revele)\\b.{0,30}\\b(instructions?|instrucoes?)\\b",
            Pattern.UNICODE_CASE);

    public String getCode() {
        return "DS-005";
    }

    public DetectionResult execute(DetectionContext c) {
        return PatternRuleSupport.scan(getCode(), c.extraction().plainText(), P, TipoOcorrencia.PROMPT_INJECTION,
                NivelRisco.ALTO, "Padrão de prompt injection",
                "Sequência imperativa dirigida à substituição, omissão ou revelação de instruções de um sistema de IA.",
                55);
    }
}
