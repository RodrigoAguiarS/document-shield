package br.com.documentshield.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.documentshield.domain.entity.IntegracaoApi;
import br.com.documentshield.repository.IntegracaoApiRepository;
import br.com.documentshield.service.ApiKeyManagementService;
import br.com.documentshield.service.ApiKeyManagementService.AuthenticationStatus;
import br.com.documentshield.service.ApiKeyManagementService.CreatedApiKey;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:api-key-management;DB_CLOSE_DELAY=-1",
            "document-shield.storage-directory=./target/api-key-test-data"
        })
class ApiKeyManagementServiceTest {
    @Autowired
    ApiKeyManagementService service;

    @Autowired
    IntegracaoApiRepository repository;

    @Autowired
    ApiKeyHasher hasher;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void generatesUniqueTokensWithAtLeast256BitsOfSecret() {
        CreatedApiKey first = create("Primeira");
        CreatedApiKey second = create("Segunda");

        assertThat(first.token()).startsWith("dsk_" + first.credential().publicId() + ".");
        assertThat(first.token().substring(first.token().lastIndexOf('.') + 1)).hasSizeGreaterThanOrEqualTo(43);
        assertThat(second.token()).isNotEqualTo(first.token());
        assertThat(second.credential().publicId())
                .isNotEqualTo(first.credential().publicId());
    }

    @Test
    void storesOnlyHashAndNeverReturnsItInListView() {
        CreatedApiKey created = create("Financeiro");
        IntegracaoApi stored = repository.findById(created.credential().id()).orElseThrow();

        assertThat(stored.getTokenHash()).isEqualTo(hasher.hash(created.token()));
        assertThat(stored.getTokenHash()).doesNotContain(created.token());
        assertThat(service.list()).singleElement().satisfies(view -> {
            assertThat(view.name()).isEqualTo("Financeiro");
            assertThat(view.toString()).doesNotContain(created.token()).doesNotContain(stored.getTokenHash());
        });
    }

    @Test
    void validatesTokenAndRecordsLastUse() {
        CreatedApiKey created = create("Consumidor");
        Instant usedAt = Instant.parse("2026-11-02T12:00:00Z");

        var result = service.authenticate(
                created.token(),
                created.credential().publicId(),
                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                usedAt);

        assertThat(result.status()).isEqualTo(AuthenticationStatus.VALID);
        assertThat(repository.findById(created.credential().id()).orElseThrow().getUltimoUsoEm())
                .isEqualTo(usedAt);
    }

    @Test
    void rejectsInvalidAndExpiredTokens() {
        CreatedApiKey created = create("Consumidor");
        var invalid = service.authenticate(
                created.token() + "x",
                created.credential().publicId(),
                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                Instant.now());
        assertThat(invalid.status()).isEqualTo(AuthenticationStatus.INVALID);

        IntegracaoApi stored = repository.findById(created.credential().id()).orElseThrow();
        stored.setExpiraEm(Instant.now().minusSeconds(1));
        repository.saveAndFlush(stored);
        var expired = service.authenticate(
                created.token(),
                created.credential().publicId(),
                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                Instant.now());
        assertThat(expired.status()).isEqualTo(AuthenticationStatus.INVALID);
    }

    @Test
    void revokesCredentialAndPreventsFurtherUse() {
        CreatedApiKey created = create("Consumidor");
        service.revoke(created.credential().id());

        IntegracaoApi stored = repository.findById(created.credential().id()).orElseThrow();
        assertThat(stored.getAtiva()).isFalse();
        assertThat(stored.getRevogadaEm()).isNotNull();
        assertThat(service.authenticate(
                                created.token(),
                                created.credential().publicId(),
                                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                                Instant.now())
                        .status())
                .isEqualTo(AuthenticationStatus.INVALID);
    }

    @Test
    void rotatesCredentialAtomicallyAndOnlyNewTokenRemainsValid() {
        CreatedApiKey previous = create("Consumidor");
        CreatedApiKey replacement = service.rotate(previous.credential().id(), null, false);

        assertThat(replacement.token()).isNotEqualTo(previous.token());
        assertThat(repository.findById(previous.credential().id()).orElseThrow().getRevogadaEm())
                .isNotNull();
        assertThat(service.authenticate(
                                previous.token(),
                                previous.credential().publicId(),
                                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                                Instant.now())
                        .status())
                .isEqualTo(AuthenticationStatus.INVALID);
        assertThat(service.authenticate(
                                replacement.token(),
                                replacement.credential().publicId(),
                                ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE,
                                Instant.now())
                        .status())
                .isEqualTo(AuthenticationStatus.VALID);
    }

    private CreatedApiKey create(String name) {
        return service.create(name, Instant.now().plusSeconds(3600), ApiKeyManagementService.PUBLIC_ANALYSIS_SCOPE, 30);
    }
}
