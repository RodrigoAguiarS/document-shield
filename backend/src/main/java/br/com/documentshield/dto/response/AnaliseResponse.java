package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.StatusAnalise;
import java.time.Instant;
import java.util.UUID;

public record AnaliseResponse(
        UUID id,
        UUID documentoId,
        StatusAnalise status,
        NivelRisco nivelRisco,
        Integer pontuacaoRisco,
        Integer totalOcorrencias,
        Integer totalOcorrenciasCriticas,
        Integer totalOcorrenciasAltas,
        Integer totalOcorrenciasMedias,
        Integer totalOcorrenciasBaixas,
        Integer totalRegrasExecutadas,
        String versaoMotor,
        Instant dataInicio,
        Instant dataFim,
        Long duracaoMilissegundos,
        String resumo,
        String mensagemErro) {}
