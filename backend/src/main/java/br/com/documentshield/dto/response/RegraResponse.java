package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.time.Instant;
import java.util.UUID;

public record RegraResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        TipoOcorrencia tipoOcorrencia,
        NivelRisco nivelRiscoPadrao,
        Integer pontuacaoBase,
        String parametrosJson,
        Boolean ativa,
        String versao,
        Instant dataCriacao,
        Instant dataAtualizacao) {}
