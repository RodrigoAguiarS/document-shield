package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.time.Instant;
import java.util.UUID;

public record OcorrenciaResponse(
        UUID id,
        UUID analiseId,
        UUID regraId,
        TipoOcorrencia tipo,
        NivelRisco nivelRisco,
        String titulo,
        String descricao,
        String conteudoSuspeito,
        String conteudoNormalizado,
        Integer numeroPagina,
        Float posicaoX,
        Float posicaoY,
        Float largura,
        Float altura,
        Float larguraPagina,
        Float alturaPagina,
        Integer rotacaoPagina,
        String espacoCoordenadas,
        Boolean localizacaoConfiavel,
        String fonte,
        Float tamanhoFonte,
        String corTexto,
        Float opacidade,
        Integer pontuacao,
        Instant dataIdentificacao) {}
