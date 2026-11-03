package br.com.documentshield.service;

import br.com.documentshield.domain.entity.IntegracaoApi;
import br.com.documentshield.exception.ApiKeyConflictException;
import br.com.documentshield.repository.IntegracaoApiRepository;
import br.com.documentshield.repository.IntegrationPolicyRepository;
import br.com.documentshield.security.ApiKeyHasher;
import br.com.documentshield.security.ApiKeyTokenGenerator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyManagementService {
    public static final String DOCUMENT_ANALYZE_SCOPE = "DOCUMENT_ANALYZE";
    public static final String PUBLIC_ANALYSIS_SCOPE = "PUBLIC_ANALYSIS";
    private final IntegracaoApiRepository repository;
    private final ApiKeyHasher hasher;
    private final ApiKeyTokenGenerator generator;
    private final IntegrationPolicyRepository policyBindings;

    public ApiKeyManagementService(
            IntegracaoApiRepository repository,
            ApiKeyHasher hasher,
            ApiKeyTokenGenerator generator,
            IntegrationPolicyRepository policyBindings) {
        this.repository = repository;
        this.hasher = hasher;
        this.generator = generator;
        this.policyBindings = policyBindings;
    }

    @Transactional
    public CreatedApiKey create(String name, Instant expiresAt, String scope, Integer rateLimit) {
        return create(name, null, expiresAt, scope, rateLimit);
    }

    @Transactional
    public CreatedApiKey create(String name, String description, Instant expiresAt, String scope, Integer rateLimit) {
        String normalizedName = requireName(name);
        String normalizedDescription = normalizeDescription(description);
        String normalizedScope = requireScope(scope);
        validateExpiration(expiresAt);
        validateRateLimit(rateLimit);
        ApiKeyTokenGenerator.GeneratedApiKey generated = generator.generate();
        IntegracaoApi credential = IntegracaoApi.builder()
                .keyId(generated.keyId())
                .tokenHash(hasher.hash(generated.token()))
                .nome(normalizedName)
                .descricao(normalizedDescription)
                .escopo(normalizedScope)
                .ativa(true)
                .expiraEm(expiresAt)
                .requisicoesPorMinuto(rateLimit)
                .build();
        repository.saveAndFlush(credential);
        return new CreatedApiKey(toView(credential), generated.token());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyView> list() {
        return repository.findAllByOrderByCriadaEmDesc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiKeyView get(UUID id) {
        return toView(find(id));
    }

    @Transactional
    public ApiKeyView revoke(UUID id) {
        IntegracaoApi credential = find(id);
        if (credential.getRevogadaEm() == null) {
            credential.setRevogadaEm(Instant.now());
        }
        credential.setAtiva(false);
        return toView(repository.save(credential));
    }

    @Transactional
    public CreatedApiKey rotate(UUID id, Instant expiresAt, boolean replaceExpiration) {
        IntegracaoApi previous = find(id);
        if (isRevoked(previous)) {
            throw new ApiKeyConflictException("Uma credencial revogada não pode ser rotacionada.");
        }
        Instant nextExpiration = replaceExpiration ? expiresAt : previous.getExpiraEm();
        if (nextExpiration != null && !nextExpiration.isAfter(Instant.now())) {
            throw new IllegalArgumentException("A nova data de expiração deve estar no futuro.");
        }
        CreatedApiKey replacement = create(
                previous.getNome(),
                previous.getDescricao(),
                nextExpiration,
                previous.getEscopo(),
                previous.getRequisicoesPorMinuto());
        policyBindings.findByIntegracaoId(previous.getId()).ifPresent(binding -> {
            binding.setIntegracaoId(replacement.credential().id());
            policyBindings.save(binding);
        });
        previous.setAtiva(false);
        previous.setRevogadaEm(Instant.now());
        repository.save(previous);
        return replacement;
    }

    @Transactional
    public AuthenticationResult authenticate(String token, String keyId, String requiredScope, Instant now) {
        IntegracaoApi credential = repository.findByKeyId(keyId).orElse(null);
        if (isInvalid(credential, token, now)) {
            return new AuthenticationResult(AuthenticationStatus.INVALID, null);
        }
        if (!scopeMatches(requiredScope, credential.getEscopo())) {
            return new AuthenticationResult(AuthenticationStatus.FORBIDDEN, null);
        }
        credential.setUltimoUsoEm(now);
        repository.save(credential);
        return new AuthenticationResult(AuthenticationStatus.VALID, credential);
    }

    private boolean isInvalid(IntegracaoApi credential, String token, Instant now) {
        return credential == null
                || !hasher.matches(token, credential.getTokenHash())
                || isRevoked(credential)
                || isExpired(credential, now);
    }

    private boolean isRevoked(IntegracaoApi credential) {
        return !Boolean.TRUE.equals(credential.getAtiva()) || credential.getRevogadaEm() != null;
    }

    private boolean isExpired(IntegracaoApi credential, Instant now) {
        return credential.getExpiraEm() != null && !credential.getExpiraEm().isAfter(now);
    }

    private IntegracaoApi find(UUID id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credencial não encontrada: " + id));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da integração.");
        }
        String normalized = name.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("O nome deve ter no máximo 255 caracteres.");
        }
        return normalized;
    }

    private String requireScope(String scope) {
        if (!PUBLIC_ANALYSIS_SCOPE.equals(scope) && !DOCUMENT_ANALYZE_SCOPE.equals(scope)) {
            throw new IllegalArgumentException("Escopo não suportado. Use DOCUMENT_ANALYZE.");
        }
        return scope;
    }

    private boolean scopeMatches(String required, String stored) {
        if (required.equals(stored)) {
            return true;
        }
        return DOCUMENT_ANALYZE_SCOPE.equals(required) && PUBLIC_ANALYSIS_SCOPE.equals(stored);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("A descrição deve ter no máximo 500 caracteres.");
        }
        return normalized;
    }

    private void validateExpiration(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("A data de expiração deve estar no futuro.");
        }
    }

    private void validateRateLimit(Integer rateLimit) {
        if (rateLimit != null && rateLimit < 1) {
            throw new IllegalArgumentException("O limite por minuto deve ser maior que zero.");
        }
    }

    private ApiKeyView toView(IntegracaoApi value) {
        return new ApiKeyView(
                value.getId(),
                value.getNome(),
                value.getDescricao(),
                value.getKeyId(),
                value.getEscopo(),
                Boolean.TRUE.equals(value.getAtiva()),
                value.getCriadaEm(),
                value.getExpiraEm(),
                value.getUltimoUsoEm(),
                value.getRevogadaEm(),
                value.getRequisicoesPorMinuto());
    }

    public enum AuthenticationStatus {
        VALID,
        INVALID,
        FORBIDDEN
    }

    public record AuthenticationResult(AuthenticationStatus status, IntegracaoApi credential) {}

    public record CreatedApiKey(ApiKeyView credential, String token) {}

    public record ApiKeyView(
            UUID id,
            String name,
            String description,
            String publicId,
            String scope,
            boolean active,
            Instant createdAt,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant revokedAt,
            Integer requestsPerMinute) {}
}
