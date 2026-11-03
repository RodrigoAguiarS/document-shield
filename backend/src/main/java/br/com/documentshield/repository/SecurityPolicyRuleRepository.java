package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.SecurityPolicyRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityPolicyRuleRepository extends JpaRepository<SecurityPolicyRule, UUID> {
    List<SecurityPolicyRule> findByPoliticaIdOrderByPontuacaoMinimaAsc(UUID politicaId);

    void deleteByPoliticaId(UUID politicaId);

    long countByPoliticaId(UUID politicaId);
}
