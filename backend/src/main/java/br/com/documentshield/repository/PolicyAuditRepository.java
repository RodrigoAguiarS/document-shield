package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.PolicyAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyAuditRepository extends JpaRepository<PolicyAudit, UUID> {
    List<PolicyAudit> findTop100ByOrderByDataEventoDesc();

    List<PolicyAudit> findByPolicyKeyOrderByDataEventoDesc(UUID policyKey);
}
