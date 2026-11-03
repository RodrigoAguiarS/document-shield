package br.com.documentshield.policy;

import br.com.documentshield.domain.enums.PolicyDecision;

public record PolicyRule(
        int minimumScore, int maximumScore, PolicyDecision decision, String description, int priority) {}
