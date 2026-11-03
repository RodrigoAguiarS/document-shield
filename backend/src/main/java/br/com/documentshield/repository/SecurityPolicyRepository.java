package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.SecurityPolicy;
import br.com.documentshield.domain.enums.PolicyStatus;
import br.com.documentshield.domain.enums.PolicyType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, UUID> {
    List<SecurityPolicy> findAllByOrderByDataAtualizacaoDesc();

    List<SecurityPolicy> findByPolicyKeyOrderByVersaoDesc(UUID policyKey);

    Optional<SecurityPolicy> findFirstByTipoAndStatusOrderByVersaoDesc(PolicyType type, PolicyStatus status);

    Optional<SecurityPolicy> findFirstByPolicyKeyOrderByVersaoDesc(UUID policyKey);

    List<SecurityPolicy> findByPolicyKeyAndStatus(UUID policyKey, PolicyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SecurityPolicy p where p.policyKey = :key")
    List<SecurityPolicy> lockVersions(@Param("key") UUID policyKey);
}
