package br.com.documentshield.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalysisPersistenceServiceTest {
    @Test
    void substituiCaractereNuloAntesDaPersistencia() {
        assertThat(AnalysisPersistenceService.sanitizeText("antes\u0000depois", 100))
                .isEqualTo("antes\uFFFDdepois");
    }

    @Test
    void preservaNuloJavaELimitaTextoSanitizado() {
        assertThat(AnalysisPersistenceService.sanitizeText(null, 10)).isNull();
        assertThat(AnalysisPersistenceService.sanitizeText("12\u0000345", 4)).isEqualTo("12\uFFFD3");
    }
}
