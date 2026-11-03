package br.com.documentshield.converter;

import br.com.documentshield.domain.entity.AnaliseDocumento;
import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.domain.entity.Evidencia;
import br.com.documentshield.domain.entity.ExecucaoRegra;
import br.com.documentshield.domain.entity.Ocorrencia;
import br.com.documentshield.domain.entity.RegraDeteccao;
import br.com.documentshield.dto.response.AnaliseResponse;
import br.com.documentshield.dto.response.DocumentoResponse;
import br.com.documentshield.dto.response.EvidenciaResponse;
import br.com.documentshield.dto.response.ExecucaoRegraResponse;
import br.com.documentshield.dto.response.OcorrenciaResponse;
import br.com.documentshield.dto.response.RegraResponse;
import org.springframework.stereotype.Component;

@Component
public class ApiConverter {
    public DocumentoResponse toResponse(Documento x) {
        return new DocumentoResponse(
                x.getId(),
                x.getNomeOriginal(),
                x.getTipoMime(),
                x.getExtensao(),
                x.getTamanhoBytes(),
                x.getHashSha256(),
                x.getQuantidadePaginas(),
                x.getStatus(),
                x.getDataUpload(),
                x.getDataAtualizacao(),
                x.getMensagemErro());
    }

    public AnaliseResponse toResponse(AnaliseDocumento x) {
        return new AnaliseResponse(
                x.getId(),
                x.getDocumentoId(),
                x.getStatus(),
                x.getNivelRisco(),
                x.getPontuacaoRisco(),
                x.getTotalOcorrencias(),
                x.getTotalOcorrenciasCriticas(),
                x.getTotalOcorrenciasAltas(),
                x.getTotalOcorrenciasMedias(),
                x.getTotalOcorrenciasBaixas(),
                x.getTotalRegrasExecutadas(),
                x.getVersaoMotor(),
                x.getDataInicio(),
                x.getDataFim(),
                x.getDuracaoMilissegundos(),
                x.getResumo(),
                x.getMensagemErro());
    }

    public OcorrenciaResponse toResponse(Ocorrencia x) {
        return new OcorrenciaResponse(
                x.getId(),
                x.getAnaliseId(),
                x.getRegraId(),
                x.getTipo(),
                x.getNivelRisco(),
                x.getTitulo(),
                x.getDescricao(),
                x.getConteudoSuspeito(),
                x.getConteudoNormalizado(),
                x.getNumeroPagina(),
                x.getPosicaoX(),
                x.getPosicaoY(),
                x.getLargura(),
                x.getAltura(),
                x.getFonte(),
                x.getTamanhoFonte(),
                x.getCorTexto(),
                x.getOpacidade(),
                x.getPontuacao(),
                x.getDataIdentificacao());
    }

    public EvidenciaResponse toResponse(Evidencia x) {
        return new EvidenciaResponse(
                x.getId(),
                x.getOcorrenciaId(),
                x.getTipo(),
                x.getDescricao(),
                x.getValorEncontrado(),
                x.getValorEsperado(),
                x.getDetalhesTecnicos(),
                x.getNumeroPagina(),
                x.getDataRegistro());
    }

    public RegraResponse toResponse(RegraDeteccao x) {
        return new RegraResponse(
                x.getId(),
                x.getCodigo(),
                x.getNome(),
                x.getDescricao(),
                x.getTipoOcorrencia(),
                x.getNivelRiscoPadrao(),
                x.getPontuacaoBase(),
                x.getParametrosJson(),
                x.getAtiva(),
                x.getVersao(),
                x.getDataCriacao(),
                x.getDataAtualizacao());
    }

    public ExecucaoRegraResponse toResponse(ExecucaoRegra x) {
        return new ExecucaoRegraResponse(
                x.getId(),
                x.getAnaliseId(),
                x.getRegraId(),
                x.getEstado(),
                x.getExecutada(),
                x.getQuantidadeOcorrencias(),
                x.getSucesso(),
                x.getDuracaoMilissegundos(),
                x.getMensagemErro(),
                x.getDataExecucao());
    }
}
