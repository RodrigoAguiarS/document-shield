package br.com.documentshield.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "integration_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationPolicy {
    @Id
    private UUID id;

    @Column(name = "integracao_id", nullable = false, unique = true)
    private UUID integracaoId;

    @Column(name = "politica_id", nullable = false)
    private UUID politicaId;

    @Column(name = "data_vinculacao", nullable = false)
    private Instant dataVinculacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Version
    @Column(name = "entity_version", nullable = false)
    private Long entityVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dataVinculacao == null) {
            dataVinculacao = now;
        }
        if (dataAtualizacao == null) {
            dataAtualizacao = now;
        }
        if (entityVersion == null) {
            entityVersion = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = Instant.now();
    }
}
