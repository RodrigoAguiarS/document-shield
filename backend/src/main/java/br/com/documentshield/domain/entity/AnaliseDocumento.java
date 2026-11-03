package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.StatusAnalise;
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
@Table(name = "analise_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseDocumento {
    @Id
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAnalise status;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco")
    private NivelRisco nivelRisco;

    @Column(name = "pontuacao_risco")
    private Integer pontuacaoRisco;

    @Builder.Default
    @Column(name = "total_ocorrencias", nullable = false)
    private Integer totalOcorrencias = 0;

    @Builder.Default
    @Column(name = "total_ocorrencias_criticas", nullable = false)
    private Integer totalOcorrenciasCriticas = 0;

    @Builder.Default
    @Column(name = "total_ocorrencias_altas", nullable = false)
    private Integer totalOcorrenciasAltas = 0;

    @Builder.Default
    @Column(name = "total_ocorrencias_medias", nullable = false)
    private Integer totalOcorrenciasMedias = 0;

    @Builder.Default
    @Column(name = "total_ocorrencias_baixas", nullable = false)
    private Integer totalOcorrenciasBaixas = 0;

    @Builder.Default
    @Column(name = "total_regras_executadas", nullable = false)
    private Integer totalRegrasExecutadas = 0;

    @Column(name = "versao_motor", nullable = false)
    private String versaoMotor;

    @Column(name = "data_inicio")
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "duracao_milissegundos")
    private Long duracaoMilissegundos;

    @Column(length = 2000)
    private String resumo;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
