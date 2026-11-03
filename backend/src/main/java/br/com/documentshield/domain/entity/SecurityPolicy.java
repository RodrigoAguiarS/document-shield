package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.PolicyStatus;
import br.com.documentshield.domain.enums.PolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "security_policy",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_security_policy_version",
                        columnNames = {"policy_key", "versao"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityPolicy {
    @Id
    private UUID id;

    @Column(name = "policy_key", nullable = false)
    private UUID policyKey;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyType tipo;

    @Column(nullable = false)
    private Integer versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyStatus status;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Column(name = "data_ativacao")
    private Instant dataAtivacao;

    @Column(name = "identificador_criador", nullable = false, length = 255)
    private String identificadorCriador;

    @Version
    @Column(name = "entity_version", nullable = false)
    private Long entityVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (policyKey == null) {
            policyKey = UUID.randomUUID();
        }
        if (versao == null) {
            versao = 1;
        }
        if (status == null) {
            status = PolicyStatus.RASCUNHO;
        }
        if (dataCriacao == null) {
            dataCriacao = now;
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
