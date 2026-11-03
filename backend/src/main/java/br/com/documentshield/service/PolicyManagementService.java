package br.com.documentshield.service;

import br.com.documentshield.domain.entity.IntegrationPolicy;
import br.com.documentshield.domain.entity.PolicyAudit;
import br.com.documentshield.domain.entity.SecurityPolicy;
import br.com.documentshield.domain.entity.SecurityPolicyRule;
import br.com.documentshield.domain.enums.PolicyAuditOperation;
import br.com.documentshield.domain.enums.PolicyDecision;
import br.com.documentshield.domain.enums.PolicyStatus;
import br.com.documentshield.domain.enums.PolicyType;
import br.com.documentshield.dto.request.PolicyRequests.RuleRequest;
import br.com.documentshield.dto.request.PolicyRequests.SavePolicyRequest;
import br.com.documentshield.dto.response.PolicyResponses;
import br.com.documentshield.exception.ApiKeyConflictException;
import br.com.documentshield.exception.PolicyUnavailableException;
import br.com.documentshield.exception.ResourceNotFoundException;
import br.com.documentshield.policy.PolicyEngine;
import br.com.documentshield.policy.PolicyValidationService;
import br.com.documentshield.policy.SecurityPolicySnapshot;
import br.com.documentshield.repository.IntegracaoApiRepository;
import br.com.documentshield.repository.IntegrationPolicyRepository;
import br.com.documentshield.repository.PolicyAuditRepository;
import br.com.documentshield.repository.SecurityPolicyRepository;
import br.com.documentshield.repository.SecurityPolicyRuleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyManagementService {
    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 1_000;
    private static final int AUDIT_DETAIL_MAX_LENGTH = 2_000;
    private static final String BOOTSTRAP_ACTOR = "system-bootstrap";

    private final SecurityPolicyRepository policies;
    private final SecurityPolicyRuleRepository rules;
    private final IntegrationPolicyRepository bindings;
    private final IntegracaoApiRepository integrations;
    private final PolicyAuditRepository audits;
    private final PolicyValidationService validation;
    private final PolicyEngine engine;

    public PolicyManagementService(
            SecurityPolicyRepository policies,
            SecurityPolicyRuleRepository rules,
            IntegrationPolicyRepository bindings,
            IntegracaoApiRepository integrations,
            PolicyAuditRepository audits,
            PolicyValidationService validation,
            PolicyEngine engine) {
        this.policies = policies;
        this.rules = rules;
        this.bindings = bindings;
        this.integrations = integrations;
        this.audits = audits;
        this.validation = validation;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public List<PolicyResponses.Policy> listLatest() {
        Map<UUID, SecurityPolicy> latest = policies.findAllByOrderByDataAtualizacaoDesc().stream()
                .collect(Collectors.toMap(
                        SecurityPolicy::getPolicyKey,
                        Function.identity(),
                        (left, right) -> left.getVersao() >= right.getVersao() ? left : right,
                        LinkedHashMap::new));
        return latest.values().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponses.Policy get(UUID id) {
        return response(find(id));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponses.Policy> versions(UUID id) {
        SecurityPolicy policy = find(id);
        return policies.findByPolicyKeyOrderByVersaoDesc(policy.getPolicyKey()).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public PolicyResponses.Policy create(SavePolicyRequest request, String actor) {
        SecurityPolicy policy = SecurityPolicy.builder()
                .nome(clean(request.nome(), NAME_MAX_LENGTH))
                .descricao(cleanNullable(request.descricao(), DESCRIPTION_MAX_LENGTH))
                .tipo(request.tipo())
                .versao(1)
                .status(PolicyStatus.RASCUNHO)
                .identificadorCriador(actor(actor))
                .build();
        policies.save(policy);
        saveRules(policy.getId(), request.regras());
        audit(policy, PolicyAuditOperation.CRIADA, actor, "Política criada em rascunho.");
        return response(policy);
    }

    @Transactional
    public PolicyResponses.Policy update(UUID id, SavePolicyRequest request, String actor) {
        SecurityPolicy current = find(id);
        if (current.getTipo() != request.tipo()) {
            throw new IllegalArgumentException("O tipo de uma família de políticas não pode ser alterado.");
        }
        if (current.getStatus() == PolicyStatus.RASCUNHO) {
            return updateDraft(current, request);
        }

        return createNextVersion(current, request, actor);
    }

    private PolicyResponses.Policy updateDraft(SecurityPolicy policy, SavePolicyRequest request) {
        policy.setNome(clean(request.nome(), NAME_MAX_LENGTH));
        policy.setDescricao(cleanNullable(request.descricao(), DESCRIPTION_MAX_LENGTH));
        rules.deleteByPoliticaId(policy.getId());
        saveRules(policy.getId(), request.regras());
        return response(policies.save(policy));
    }

    private PolicyResponses.Policy createNextVersion(SecurityPolicy current, SavePolicyRequest request, String actor) {
        policies.lockVersions(current.getPolicyKey());
        SecurityPolicy next = SecurityPolicy.builder()
                .policyKey(current.getPolicyKey())
                .nome(clean(request.nome(), NAME_MAX_LENGTH))
                .descricao(cleanNullable(request.descricao(), DESCRIPTION_MAX_LENGTH))
                .tipo(current.getTipo())
                .versao(nextVersion(current))
                .status(PolicyStatus.RASCUNHO)
                .identificadorCriador(actor(actor))
                .build();
        policies.save(next);
        saveRules(next.getId(), request.regras());
        audit(
                next,
                PolicyAuditOperation.NOVA_VERSAO,
                actor,
                "Nova versão criada a partir da versão " + current.getVersao() + ".");
        return response(next);
    }

    private int nextVersion(SecurityPolicy current) {
        return policies.findFirstByPolicyKeyOrderByVersaoDesc(current.getPolicyKey())
                        .map(SecurityPolicy::getVersao)
                        .orElse(current.getVersao())
                + 1;
    }

    @Transactional
    public synchronized PolicyResponses.Policy activate(UUID id, String actor) {
        SecurityPolicy target = find(id);
        if (target.getStatus() != PolicyStatus.RASCUNHO) {
            throw new ApiKeyConflictException("Somente uma política em rascunho pode ser ativada.");
        }
        SecurityPolicySnapshot snapshot = snapshot(target);
        validation.validate(snapshot);
        policies.lockVersions(target.getPolicyKey());

        List<SecurityPolicy> replaced = activePoliciesReplacedBy(target);
        archiveReplacedPolicies(replaced, target, actor);

        target.setStatus(PolicyStatus.ATIVA);
        target.setDataAtivacao(Instant.now());
        policies.save(target);
        migrateBindings(replaced, target);
        audit(target, PolicyAuditOperation.ATIVADA, actor, "Política validada e ativada.");
        return response(target);
    }

    private List<SecurityPolicy> activePoliciesReplacedBy(SecurityPolicy target) {
        List<SecurityPolicy> replaced =
                new ArrayList<>(policies.findByPolicyKeyAndStatus(target.getPolicyKey(), PolicyStatus.ATIVA));
        if (target.getTipo() == PolicyType.GLOBAL) {
            policies.findFirstByTipoAndStatusOrderByVersaoDesc(PolicyType.GLOBAL, PolicyStatus.ATIVA)
                    .filter(active -> !active.getId().equals(target.getId()) && !replaced.contains(active))
                    .ifPresent(replaced::add);
        }
        return replaced;
    }

    private void archiveReplacedPolicies(List<SecurityPolicy> replaced, SecurityPolicy target, String actor) {
        for (SecurityPolicy active : replaced) {
            active.setStatus(PolicyStatus.ARQUIVADA);
            policies.save(active);
            audit(
                    active,
                    PolicyAuditOperation.ARQUIVADA,
                    actor,
                    "Versão arquivada pela ativação da política " + target.getId() + ".");
        }
    }

    private void migrateBindings(List<SecurityPolicy> replaced, SecurityPolicy target) {
        if (target.getTipo() != PolicyType.INTEGRACAO) {
            return;
        }
        replaced.stream()
                .flatMap(active -> bindings.findByPoliticaId(active.getId()).stream())
                .forEach(binding -> {
                    binding.setPoliticaId(target.getId());
                    bindings.save(binding);
                });
    }

    @Transactional
    public PolicyResponses.Policy archive(UUID id, String actor) {
        SecurityPolicy policy = find(id);
        if (policy.getTipo() == PolicyType.GLOBAL && policy.getStatus() == PolicyStatus.ATIVA) {
            throw new ApiKeyConflictException("A política global ativa não pode ser arquivada.");
        }
        if (bindings.existsByPoliticaId(policy.getId())) {
            throw new ApiKeyConflictException("A política está vinculada a uma integração.");
        }
        policy.setStatus(PolicyStatus.ARQUIVADA);
        policies.save(policy);
        audit(policy, PolicyAuditOperation.ARQUIVADA, actor, "Política arquivada administrativamente.");
        return response(policy);
    }

    @Transactional(readOnly = true)
    public PolicyResponses.Evaluation simulate(UUID id, int score) {
        SecurityPolicy policy = find(id);
        return PolicyMapper.evaluation(engine.evaluate(snapshot(policy), score));
    }

    @Transactional(readOnly = true)
    public PolicyResponses.Binding binding(UUID integrationId) {
        requireIntegration(integrationId);
        return bindings.findByIntegracaoId(integrationId)
                .flatMap(value -> policies.findById(value.getPoliticaId())
                        .map(policy -> bindingResponse(integrationId, policy, value.getDataVinculacao(), false)))
                .orElseGet(() -> globalBinding(integrationId));
    }

    private PolicyResponses.Binding globalBinding(UUID integrationId) {
        SecurityPolicy global = policies.findFirstByTipoAndStatusOrderByVersaoDesc(
                        PolicyType.GLOBAL, PolicyStatus.ATIVA)
                .orElseThrow(() -> new PolicyUnavailableException("A política global ativa não está disponível."));
        return bindingResponse(integrationId, global, null, true);
    }

    @Transactional
    public PolicyResponses.Binding bind(UUID integrationId, UUID policyId, String actor) {
        requireIntegration(integrationId);
        SecurityPolicy policy = find(policyId);
        if (policy.getTipo() != PolicyType.INTEGRACAO || policy.getStatus() != PolicyStatus.ATIVA) {
            throw new IllegalArgumentException("Somente uma política específica ativa pode ser vinculada.");
        }
        IntegrationPolicy binding = bindings.findByIntegracaoId(integrationId)
                .orElseGet(() ->
                        IntegrationPolicy.builder().integracaoId(integrationId).build());
        binding.setPoliticaId(policyId);
        bindings.save(binding);
        audit(policy, PolicyAuditOperation.VINCULADA, actor, "Política vinculada à integração " + integrationId + ".");
        return bindingResponse(integrationId, policy, binding.getDataVinculacao(), false);
    }

    @Transactional
    public PolicyResponses.Binding unbind(UUID integrationId, String actor) {
        requireIntegration(integrationId);
        bindings.findByIntegracaoId(integrationId).ifPresent(binding -> {
            policies.findById(binding.getPoliticaId())
                    .ifPresent(policy -> audit(
                            policy,
                            PolicyAuditOperation.DESVINCULADA,
                            actor,
                            "Política desvinculada da integração " + integrationId + "."));
            bindings.delete(binding);
        });
        return binding(integrationId);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponses.Audit> audits() {
        return audits.findTop100ByOrderByDataEventoDesc().stream()
                .map(value -> new PolicyResponses.Audit(
                        value.getId(),
                        value.getPoliticaId(),
                        value.getPolicyKey(),
                        value.getVersao(),
                        value.getOperacao(),
                        value.getAdministrador(),
                        value.getDataEvento(),
                        value.getDetalhes()))
                .toList();
    }

    @Transactional
    public void ensureDefaultPolicy() {
        if (policies.findFirstByTipoAndStatusOrderByVersaoDesc(PolicyType.GLOBAL, PolicyStatus.ATIVA)
                .isPresent()) {
            return;
        }
        SecurityPolicy policy = SecurityPolicy.builder()
                .nome("Política Padrão Document Shield")
                .descricao("Política global segura aplicada quando uma integração não possui configuração específica.")
                .tipo(PolicyType.GLOBAL)
                .versao(1)
                .status(PolicyStatus.ATIVA)
                .dataAtivacao(Instant.now())
                .identificadorCriador(BOOTSTRAP_ACTOR)
                .build();
        policies.save(policy);
        saveRules(policy.getId(), defaultRules());
        validation.validate(snapshot(policy));
        audit(policy, PolicyAuditOperation.CRIADA, BOOTSTRAP_ACTOR, "Política global padrão provisionada.");
        audit(policy, PolicyAuditOperation.ATIVADA, BOOTSTRAP_ACTOR, "Política global padrão ativada.");
    }

    private List<RuleRequest> defaultRules() {
        return List.of(
                new RuleRequest(0, 29, PolicyDecision.ALLOW, "Pontuação dentro da faixa permitida.", 1),
                new RuleRequest(30, 49, PolicyDecision.REVIEW, "Pontuação requer revisão antes do uso.", 2),
                new RuleRequest(50, 100, PolicyDecision.BLOCK, "Pontuação excede o limite permitido.", 3));
    }

    private SecurityPolicy find(UUID id) {
        return policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Política não encontrada."));
    }

    private void requireIntegration(UUID id) {
        if (!integrations.existsById(id)) {
            throw new ResourceNotFoundException("Integração não encontrada.");
        }
    }

    private void saveRules(UUID policyId, List<RuleRequest> values) {
        List<SecurityPolicyRule> entities = new ArrayList<>();
        for (RuleRequest value : values) {
            entities.add(SecurityPolicyRule.builder()
                    .politicaId(policyId)
                    .pontuacaoMinima(value.pontuacaoMinima())
                    .pontuacaoMaxima(value.pontuacaoMaxima())
                    .decisao(value.decisao())
                    .descricao(cleanNullable(value.descricao(), DESCRIPTION_MAX_LENGTH))
                    .prioridade(value.prioridade())
                    .build());
        }
        rules.saveAll(entities);
    }

    private SecurityPolicySnapshot snapshot(SecurityPolicy policy) {
        return PolicyMapper.snapshot(policy, rules.findByPoliticaIdOrderByPontuacaoMinimaAsc(policy.getId()));
    }

    private PolicyResponses.Policy response(SecurityPolicy policy) {
        List<SecurityPolicyRule> policyRules = rules.findByPoliticaIdOrderByPontuacaoMinimaAsc(policy.getId());
        return new PolicyResponses.Policy(
                policy.getId(),
                policy.getPolicyKey(),
                policy.getNome(),
                policy.getDescricao(),
                policy.getTipo(),
                policy.getVersao(),
                policy.getStatus(),
                policy.getDataCriacao(),
                policy.getDataAtualizacao(),
                policy.getDataAtivacao(),
                policy.getIdentificadorCriador(),
                policy.getEntityVersion() == null ? 0 : policy.getEntityVersion(),
                policyRules.size(),
                policyRules.stream().map(PolicyMapper::rule).toList());
    }

    private PolicyResponses.Binding bindingResponse(
            UUID integrationId, SecurityPolicy policy, Instant linkedAt, boolean global) {
        return new PolicyResponses.Binding(
                integrationId,
                policy.getId(),
                policy.getNome(),
                policy.getVersao(),
                policy.getTipo(),
                policy.getStatus(),
                policy.getDataAtivacao(),
                linkedAt,
                global);
    }

    private void audit(SecurityPolicy policy, PolicyAuditOperation operation, String actor, String detail) {
        audits.save(PolicyAudit.builder()
                .politicaId(policy.getId())
                .policyKey(policy.getPolicyKey())
                .versao(policy.getVersao())
                .operacao(operation)
                .administrador(actor(actor))
                .detalhes(cleanNullable(detail, AUDIT_DETAIL_MAX_LENGTH))
                .build());
    }

    private String actor(String value) {
        return clean(value == null ? "unknown-admin" : value, NAME_MAX_LENGTH);
    }

    private String clean(String value, int max) {
        String result = value == null ? "" : value.trim();
        if (result.isBlank()) {
            throw new IllegalArgumentException("Um campo obrigatório não foi informado.");
        }
        return result.substring(0, Math.min(max, result.length()));
    }

    private String cleanNullable(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.trim();
        return result.substring(0, Math.min(max, result.length()));
    }
}
