package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.IntegrationPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationPolicyRepository extends JpaRepository<IntegrationPolicy, UUID> {
    Optional<IntegrationPolicy> findByIntegracaoId(UUID integrationId);

    List<IntegrationPolicy> findByPoliticaId(UUID policyId);

    boolean existsByPoliticaId(UUID policyId);
}
