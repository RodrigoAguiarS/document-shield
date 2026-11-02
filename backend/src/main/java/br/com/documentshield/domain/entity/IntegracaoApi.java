package br.com.documentshield.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integracao_api")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegracaoApi {
    @Id
    private UUID id;
    @Column(name = "key_id", nullable = false, unique = true, length = 32)
    private String keyId;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 255)
    private String nome;
    @Column(length = 500)
    private String descricao;
    @Column(nullable = false, length = 64)
    private String escopo;
    @Column(nullable = false)
    private Boolean ativa;
    @Column(name = "expira_em")
    private Instant expiraEm;
    @Column(name = "revogada_em")
    private Instant revogadaEm;
    @Column(name = "ultimo_uso_em")
    private Instant ultimoUsoEm;
    @Column(name = "requisicoes_por_minuto")
    private Integer requisicoesPorMinuto;
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (criadaEm == null) criadaEm = Instant.now();
        if (ativa == null) ativa = true;
    }
}
