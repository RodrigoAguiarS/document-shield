package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.PolicyDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "security_policy_rule",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_policy_rule_priority",
                        columnNames = {"politica_id", "prioridade"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityPolicyRule {
    @Id
    private UUID id;

    @Column(name = "politica_id", nullable = false)
    private UUID politicaId;

    @Column(name = "pontuacao_minima", nullable = false)
    private Integer pontuacaoMinima;

    @Column(name = "pontuacao_maxima", nullable = false)
    private Integer pontuacaoMaxima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyDecision decisao;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Integer prioridade;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
