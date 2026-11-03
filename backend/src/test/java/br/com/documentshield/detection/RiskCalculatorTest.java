package br.com.documentshield.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.documentshield.detection.engine.RiskCalculator;
import br.com.documentshield.domain.enums.NivelRisco;
import org.junit.jupiter.api.Test;

class RiskCalculatorTest {
    private final RiskCalculator c = new RiskCalculator();

    @Test
    void classificaLimites() {
        assertThat(c.levelFor(0)).isEqualTo(NivelRisco.INFORMATIVO);
        assertThat(c.levelFor(9)).isEqualTo(NivelRisco.INFORMATIVO);
        assertThat(c.levelFor(10)).isEqualTo(NivelRisco.BAIXO);
        assertThat(c.levelFor(29)).isEqualTo(NivelRisco.BAIXO);
        assertThat(c.levelFor(30)).isEqualTo(NivelRisco.MEDIO);
        assertThat(c.levelFor(49)).isEqualTo(NivelRisco.MEDIO);
        assertThat(c.levelFor(50)).isEqualTo(NivelRisco.ALTO);
        assertThat(c.levelFor(79)).isEqualTo(NivelRisco.ALTO);
        assertThat(c.levelFor(80)).isEqualTo(NivelRisco.CRITICO);
        assertThat(c.levelFor(100)).isEqualTo(NivelRisco.CRITICO);
    }

    @Test
    void rejeitaForaDaFaixa() {
        assertThatThrownBy(() -> c.levelFor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.levelFor(101)).isInstanceOf(IllegalArgumentException.class);
    }
}
