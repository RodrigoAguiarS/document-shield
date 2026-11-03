package br.com.documentshield.domain.entity;

import br.com.documentshield.domain.enums.StatusDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {
    @Id
    private UUID id;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "nome_armazenado", nullable = false, unique = true)
    private String nomeArmazenado;

    @Column(name = "tipo_mime", nullable = false)
    private String tipoMime;

    @Column(nullable = false)
    private String extensao;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "caminho_armazenamento", nullable = false, unique = true, length = 1024)
    private String caminhoArmazenamento;

    @Column(name = "quantidade_paginas")
    private Integer quantidadePaginas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDocumento status;

    @Column(name = "data_upload", nullable = false)
    private Instant dataUpload;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (dataUpload == null) {
            dataUpload = now;
        }
        dataAtualizacao = now;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = Instant.now();
    }
}
