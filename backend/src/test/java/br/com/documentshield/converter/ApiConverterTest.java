package br.com.documentshield.converter;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.documentshield.domain.entity.Ocorrencia;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiConverterTest {
    private final ApiConverter converter = new ApiConverter();

    @Test
    void expoeContratoDeLocalizacaoVisualSemConverterUnidades() {
        Ocorrencia occurrence = Ocorrencia.builder()
                .id(UUID.randomUUID())
                .analiseId(UUID.randomUUID())
                .tipo(TipoOcorrencia.FONTE_MINUSCULA)
                .nivelRisco(NivelRisco.BAIXO)
                .titulo("Fonte pequena")
                .descricao("Caixa visual")
                .posicaoX(12.5f)
                .posicaoY(24.5f)
                .largura(30f)
                .altura(10f)
                .larguraPagina(612f)
                .alturaPagina(792f)
                .rotacaoPagina(0)
                .espacoCoordenadas("PDFBOX_ROTATED_CROP_TOP_LEFT")
                .localizacaoConfiavel(true)
                .pontuacao(10)
                .build();

        var response = converter.toResponse(occurrence);

        assertThat(response.posicaoX()).isEqualTo(12.5f);
        assertThat(response.larguraPagina()).isEqualTo(612f);
        assertThat(response.alturaPagina()).isEqualTo(792f);
        assertThat(response.rotacaoPagina()).isZero();
        assertThat(response.espacoCoordenadas()).isEqualTo("PDFBOX_ROTATED_CROP_TOP_LEFT");
        assertThat(response.localizacaoConfiavel()).isTrue();
    }
}
