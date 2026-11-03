package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.EstadoExecucaoRegra;
import java.time.Instant;
import java.util.UUID;

public record ExecucaoRegraResponse(
        UUID id,
        UUID analiseId,
        UUID regraId,
        EstadoExecucaoRegra estado,
        Boolean executada,
        Integer quantidadeOcorrencias,
        Boolean sucesso,
        Long duracaoMilissegundos,
        String mensagemErro,
        Instant dataExecucao) {}
