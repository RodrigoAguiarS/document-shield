package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.PolicyAuditOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policy_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAudit {
    @Id
    private UUID id;

    @Column(name = "politica_id")
    private UUID politicaId;

    @Column(name = "policy_key")
    private UUID policyKey;

    private Integer versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PolicyAuditOperation operacao;

    @Column(nullable = false, length = 255)
    private String administrador;

    @Column(name = "data_evento", nullable = false)
    private Instant dataEvento;

    @Column(length = 2000)
    private String detalhes;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dataEvento == null) {
            dataEvento = Instant.now();
        }
    }
}
