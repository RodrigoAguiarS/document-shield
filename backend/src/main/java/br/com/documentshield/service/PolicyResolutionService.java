package br.com.documentshield.service;

import br.com.documentshield.domain.entity.SecurityPolicy;
import br.com.documentshield.domain.enums.PolicyStatus;
import br.com.documentshield.domain.enums.PolicyType;
import br.com.documentshield.exception.PolicyUnavailableException;
import br.com.documentshield.policy.PolicyEngine;
import br.com.documentshield.policy.PolicyEvaluationResult;
import br.com.documentshield.policy.PolicyValidationService;
import br.com.documentshield.policy.SecurityPolicySnapshot;
import br.com.documentshield.repository.IntegrationPolicyRepository;
import br.com.documentshield.repository.SecurityPolicyRepository;
import br.com.documentshield.repository.SecurityPolicyRuleRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyResolutionService {
    private final SecurityPolicyRepository policies;
    private final SecurityPolicyRuleRepository rules;
    private final IntegrationPolicyRepository bindings;
    private final PolicyValidationService validation;
    private final PolicyEngine engine;

    public PolicyResolutionService(
            SecurityPolicyRepository policies,
            SecurityPolicyRuleRepository rules,
            IntegrationPolicyRepository bindings,
            PolicyValidationService validation,
            PolicyEngine engine) {
        this.policies = policies;
        this.rules = rules;
        this.bindings = bindings;
        this.validation = validation;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public PolicyEvaluationResult evaluate(UUID integrationId, int riskScore) {
        SecurityPolicy policy = bindings.findByIntegracaoId(integrationId)
                .flatMap(binding -> policies.findById(binding.getPoliticaId()))
                .filter(candidate ->
                        candidate.getStatus() == PolicyStatus.ATIVA && candidate.getTipo() == PolicyType.INTEGRACAO)
                .orElseGet(this::globalPolicy);
        SecurityPolicySnapshot snapshot =
                PolicyMapper.snapshot(policy, rules.findByPoliticaIdOrderByPontuacaoMinimaAsc(policy.getId()));
        try {
            validation.validate(snapshot);
            return engine.evaluate(snapshot, riskScore);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            if (policy.getTipo() == PolicyType.INTEGRACAO) {
                SecurityPolicy global = globalPolicy();
                SecurityPolicySnapshot fallback =
                        PolicyMapper.snapshot(global, rules.findByPoliticaIdOrderByPontuacaoMinimaAsc(global.getId()));
                try {
                    return engine.evaluate(fallback, riskScore);
                } catch (IllegalArgumentException | IllegalStateException ignored) {
                    throw unavailable();
                }
            }
            throw unavailable();
        }
    }

    private SecurityPolicy globalPolicy() {
        return policies.findFirstByTipoAndStatusOrderByVersaoDesc(PolicyType.GLOBAL, PolicyStatus.ATIVA)
                .orElseThrow(this::unavailable);
    }

    private PolicyUnavailableException unavailable() {
        return new PolicyUnavailableException("Nenhuma política global ativa e válida está disponível.");
    }
}
