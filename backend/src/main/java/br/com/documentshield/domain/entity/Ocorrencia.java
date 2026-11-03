package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
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
import org.hibernate.Length;

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

    @Column(name = "conteudo_suspeito", length = Length.LONG32)
    private String conteudoSuspeito;

    @Column(name = "conteudo_normalizado", length = Length.LONG32)
    private String conteudoNormalizado;

    @Column(name = "numero_pagina")
    private Integer numeroPagina;

    @Column(name = "posicao_x")
    private Float posicaoX;

    @Column(name = "posicao_y")
    private Float posicaoY;

    private Float largura;
    private Float altura;

    @Column(name = "largura_pagina")
    private Float larguraPagina;

    @Column(name = "altura_pagina")
    private Float alturaPagina;

    @Column(name = "rotacao_pagina")
    private Integer rotacaoPagina;

    @Column(name = "espaco_coordenadas", length = 64)
    private String espacoCoordenadas;

    @Column(name = "localizacao_confiavel")
    private Boolean localizacaoConfiavel;

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
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dataIdentificacao == null) {
            dataIdentificacao = Instant.now();
        }
    }
}
