package br.com.documentshield.service;

import br.com.documentshield.domain.entity.SecurityPolicy;
import br.com.documentshield.domain.entity.SecurityPolicyRule;
import br.com.documentshield.dto.response.PolicyResponses;
import br.com.documentshield.policy.PolicyEvaluationResult;
import br.com.documentshield.policy.PolicyRule;
import br.com.documentshield.policy.SecurityPolicySnapshot;
import java.util.List;

final class PolicyMapper {
    private PolicyMapper() {}

    static SecurityPolicySnapshot snapshot(SecurityPolicy policy, List<SecurityPolicyRule> rules) {
        return new SecurityPolicySnapshot(
                policy.getId(),
                policy.getPolicyKey(),
                policy.getNome(),
                policy.getVersao(),
                policy.getTipo(),
                rules.stream()
                        .map(rule -> new PolicyRule(
                                rule.getPontuacaoMinima(),
                                rule.getPontuacaoMaxima(),
                                rule.getDecisao(),
                                rule.getDescricao(),
                                rule.getPrioridade()))
                        .toList());
    }

    static PolicyResponses.Rule rule(SecurityPolicyRule value) {
        return new PolicyResponses.Rule(
                value.getId(),
                value.getPontuacaoMinima(),
                value.getPontuacaoMaxima(),
                value.getDecisao(),
                value.getDescricao(),
                value.getPrioridade());
    }

    static PolicyResponses.Evaluation evaluation(PolicyEvaluationResult value) {
        PolicyRule rule = value.appliedRule();
        return new PolicyResponses.Evaluation(
                value.policyId(),
                value.policyKey(),
                value.policyName(),
                value.policyVersion(),
                value.origin(),
                value.decision(),
                value.evaluatedScore(),
                new PolicyResponses.Rule(
                        null,
                        rule.minimumScore(),
                        rule.maximumScore(),
                        rule.decision(),
                        rule.description(),
                        rule.priority()),
                value.reason());
    }
}
