package br.com.documentshield.detection.engine;

import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RiskCalculator {
    public RiskAssessment calculate(List<DetectionFinding> findings) {
        Map<String, Integer> strongest = new HashMap<>();
        for (DetectionFinding f : findings) {
            strongest.merge(f.ruleCode(), (int) Math.round(f.baseScore() * f.confidence()), Math::max);
        }
        int score = strongest.values().stream().mapToInt(Integer::intValue).sum();
        boolean instruction = findings.stream()
                .anyMatch(f -> f.type() == TipoOcorrencia.PROMPT_INJECTION || f.type() == TipoOcorrencia.INSTRUCAO_IA);
        boolean hidden = findings.stream()
                .anyMatch(f -> Set.of(
                                TipoOcorrencia.TEXTO_INVISIVEL,
                                TipoOcorrencia.FONTE_MINUSCULA,
                                TipoOcorrencia.TEXTO_FORA_DA_PAGINA,
                                TipoOcorrencia.UNICODE_SUSPEITO,
                                TipoOcorrencia.METADADO_SUSPEITO)
                        .contains(f.type()));
        if (instruction && hidden) {
            score += 25;
        }
        score = Math.max(0, Math.min(100, score));
        NivelRisco level = levelFor(score);
        return new RiskAssessment(
                score,
                level,
                findings.isEmpty()
                        ? "Nenhuma evidência detectada pelas regras ativas."
                        : findings.size() + " ocorrência(s), " + strongest.size() + " regra(s) com evidências; risco "
                                + level + ". Repetições da mesma regra não acumulam pontuação.");
    }

    public NivelRisco levelFor(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Pontuação deve estar entre 0 e 100");
        }
        if (score <= 9) {
            return NivelRisco.INFORMATIVO;
        }
        if (score <= 29) {
            return NivelRisco.BAIXO;
        }
        if (score <= 49) {
            return NivelRisco.MEDIO;
        }
        if (score <= 79) {
            return NivelRisco.ALTO;
        }
        return NivelRisco.CRITICO;
    }
}
