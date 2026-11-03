package br.com.documentshield.policy;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PolicyValidationService {
    public void validate(SecurityPolicySnapshot policy) {
        if (policy == null || policy.id() == null || policy.policyKey() == null) {
            throw new IllegalArgumentException("A política precisa possuir identificação.");
        }
        if (policy.version() < 1) {
            throw new IllegalArgumentException("A versão da política deve ser positiva.");
        }
        if (policy.rules() == null || policy.rules().isEmpty()) {
            throw new IllegalArgumentException("A política deve possuir pelo menos uma regra.");
        }

        List<PolicyRule> ordered = policy.rules().stream()
                .sorted(Comparator.comparingInt(PolicyRule::minimumScore).thenComparingInt(PolicyRule::maximumScore))
                .toList();
        int expectedMinimum = 0;
        for (PolicyRule rule : ordered) {
            if (rule.decision() == null) {
                throw new IllegalArgumentException("Toda regra deve possuir uma decisão.");
            }
            if (rule.minimumScore() < 0 || rule.maximumScore() > 100) {
                throw new IllegalArgumentException("Os intervalos devem permanecer entre 0 e 100.");
            }
            if (rule.minimumScore() > rule.maximumScore()) {
                throw new IllegalArgumentException("O início do intervalo não pode superar o fim.");
            }
            if (rule.minimumScore() < expectedMinimum) {
                throw new IllegalArgumentException("Os intervalos da política estão sobrepostos.");
            }
            if (rule.minimumScore() > expectedMinimum) {
                throw new IllegalArgumentException("A política possui pontuações sem decisão.");
            }
            expectedMinimum = rule.maximumScore() + 1;
        }
        if (expectedMinimum != 101) {
            throw new IllegalArgumentException("A política deve cobrir todas as pontuações de 0 a 100.");
        }
    }
}
