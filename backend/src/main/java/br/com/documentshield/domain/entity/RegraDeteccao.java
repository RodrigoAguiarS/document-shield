package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

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
        if (id == null)
            id = UUID.randomUUID();
        Instant n = Instant.now();
        if (dataCriacao == null)
            dataCriacao = n;
        dataAtualizacao = n;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = Instant.now();
    }
}
