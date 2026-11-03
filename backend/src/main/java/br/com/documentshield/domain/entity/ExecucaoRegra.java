package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.EstadoExecucaoRegra;
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
@Table(name = "execucao_regra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecucaoRegra {
    @Id
    private UUID id;

    @Column(name = "analise_id", nullable = false)
    private UUID analiseId;

    @Column(name = "regra_id", nullable = false)
    private UUID regraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoExecucaoRegra estado;

    @Column(nullable = false)
    private Boolean executada;

    @Column(name = "quantidade_ocorrencias", nullable = false)
    private Integer quantidadeOcorrencias;

    @Column(nullable = false)
    private Boolean sucesso;

    @Column(name = "duracao_milissegundos", nullable = false)
    private Long duracaoMilissegundos;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;

    @Column(name = "data_execucao", nullable = false)
    private Instant dataExecucao;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dataExecucao == null) {
            dataExecucao = Instant.now();
        }
    }
}
