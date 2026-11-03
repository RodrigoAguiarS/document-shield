package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.TipoEvidencia;
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
@Table(name = "evidencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evidencia {
    @Id
    private UUID id;

    @Column(name = "ocorrencia_id", nullable = false)
    private UUID ocorrenciaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvidencia tipo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(name = "valor_encontrado", length = Length.LONG32)
    private String valorEncontrado;

    @Column(name = "valor_esperado", length = Length.LONG32)
    private String valorEsperado;

    @Column(name = "detalhes_tecnicos", length = Length.LONG32)
    private String detalhesTecnicos;

    @Column(name = "numero_pagina")
    private Integer numeroPagina;

    @Column(name = "data_registro", nullable = false)
    private Instant dataRegistro;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dataRegistro == null) {
            dataRegistro = Instant.now();
        }
    }
}
