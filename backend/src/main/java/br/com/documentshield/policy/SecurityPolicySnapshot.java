package br.com.documentshield.policy;

import br.com.documentshield.domain.enums.PolicyType;
import java.util.List;
import java.util.UUID;

public record SecurityPolicySnapshot(
        UUID id, UUID policyKey, String name, int version, PolicyType type, List<PolicyRule> rules) {
    public SecurityPolicySnapshot {
        rules = List.copyOf(rules);
    }
}
