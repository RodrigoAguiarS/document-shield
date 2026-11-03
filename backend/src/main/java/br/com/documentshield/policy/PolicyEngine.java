package br.com.documentshield.policy;

import org.springframework.stereotype.Service;

@Service
public class PolicyEngine {
    private final PolicyValidationService validation;

    public PolicyEngine(PolicyValidationService validation) {
        this.validation = validation;
    }

    public PolicyEvaluationResult evaluate(SecurityPolicySnapshot policy, int riskScore) {
        if (riskScore < 0 || riskScore > 100) {
            throw new IllegalArgumentException("A pontuação avaliada deve estar entre 0 e 100.");
        }
        validation.validate(policy);
        PolicyRule applied = policy.rules().stream()
                .filter(rule -> riskScore >= rule.minimumScore() && riskScore <= rule.maximumScore())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A política validada não produziu uma decisão."));
        String reason = applied.description() == null || applied.description().isBlank()
                ? "A pontuação " + riskScore + " está no intervalo " + applied.minimumScore() + "–"
                        + applied.maximumScore() + "."
                : applied.description();
        return new PolicyEvaluationResult(
                policy.id(),
                policy.policyKey(),
                policy.name(),
                policy.version(),
                policy.type(),
                applied.decision(),
                riskScore,
                applied,
                reason);
    }
}
