package br.com.documentshield.policy;

import br.com.documentshield.domain.enums.PolicyDecision;
import br.com.documentshield.domain.enums.PolicyType;
import java.util.UUID;

public record PolicyEvaluationResult(
        UUID policyId,
        UUID policyKey,
        String policyName,
        int policyVersion,
        PolicyType origin,
        PolicyDecision decision,
        int evaluatedScore,
        PolicyRule appliedRule,
        String reason) {}
