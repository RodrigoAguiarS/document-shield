package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "regra_deteccao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegraDeteccao {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocorrencia", nullable = false)
    private TipoOcorrencia tipoOcorrencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco_padrao", nullable = false)
    private NivelRisco nivelRiscoPadrao;

    @Column(name = "pontuacao_base", nullable = false)
    private Integer pontuacaoBase;

    @Lob
    @Column(name = "parametros_json")
    private String parametrosJson;

    @Column(nullable = false)
    private Boolean ativa;

    @Column(nullable = false)
    private String versao;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant n = Instant.now();
        if (dataCriacao == null) {
            dataCriacao = n;
        }
        dataAtualizacao = n;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = Instant.now();
    }
}
