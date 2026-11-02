package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.*;
import java.util.*;

public record PublicAnalysisResponse(
        String status,
        UUID correlacaoId,
        PublicDocumentResponse documento,
        int quantidadePaginas,
        String versaoMotor,
        NivelRisco nivelRisco,
        int pontuacaoRisco,
        String resumo,
        int totalOcorrencias,
        int totalOcorrenciasCriticas,
        int totalOcorrenciasAltas,
        int totalOcorrenciasMedias,
        int totalOcorrenciasBaixas,
        int totalOcorrenciasInformativas,
        int totalRegrasExecutadas,
        long duracaoMilissegundos,
        boolean respostaTruncada,
        List<PublicRuleExecutionResponse> execucoes,
        List<PublicOccurrenceResponse> ocorrencias) {

    public record PublicDocumentResponse(String nome, String tipoMime, long tamanhoBytes, String hashSha256) {}

    public record PublicRuleExecutionResponse(String codigo, boolean executada, boolean sucesso,
            long duracaoMilissegundos, int quantidadeOcorrencias, String mensagem) {}

    public record PublicOccurrenceResponse(UUID id, String codigoRegra, TipoOcorrencia tipo,
            NivelRisco nivelRisco, String titulo, String descricao, String conteudoSuspeito,
            String conteudoNormalizado, Integer numeroPagina, Float posicaoX, Float posicaoY,
            Float largura, Float altura, String fonte, Float tamanhoFonte, String corTexto,
            Float opacidade, int pontuacao, List<PublicEvidenceResponse> evidencias) {}

    public record PublicEvidenceResponse(TipoEvidencia tipo, String descricao, String valorEncontrado,
            String valorEsperado, String detalhesTecnicos, Integer numeroPagina) {}
}
