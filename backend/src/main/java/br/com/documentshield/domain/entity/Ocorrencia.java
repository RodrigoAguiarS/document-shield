package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ocorrencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ocorrencia {
    @Id
    private UUID id;
    @Column(name = "analise_id", nullable = false)
    private UUID analiseId;
    @Column(name = "regra_id")
    private UUID regraId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOcorrencia tipo;
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false)
    private NivelRisco nivelRisco;
    @Column(nullable = false)
    private String titulo;
    @Column(nullable = false, length = 2000)
    private String descricao;
    @Lob
    @Column(name = "conteudo_suspeito")
    private String conteudoSuspeito;
    @Lob
    @Column(name = "conteudo_normalizado")
    private String conteudoNormalizado;
    @Column(name = "numero_pagina")
    private Integer numeroPagina;
    @Column(name = "posicao_x")
    private Float posicaoX;
    @Column(name = "posicao_y")
    private Float posicaoY;
    private Float largura;
    private Float altura;
    private String fonte;
    @Column(name = "tamanho_fonte")
    private Float tamanhoFonte;
    @Column(name = "cor_texto")
    private String corTexto;
    private Float opacidade;
    @Column(nullable = false)
    private Integer pontuacao;
    @Column(name = "data_identificacao", nullable = false)
    private Instant dataIdentificacao;

    @PrePersist
    void prePersist() {
        if (id == null)
            id = UUID.randomUUID();
        if (dataIdentificacao == null)
            dataIdentificacao = Instant.now();
    }
}
