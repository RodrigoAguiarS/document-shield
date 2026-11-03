package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.IntegracaoApi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegracaoApiRepository extends JpaRepository<IntegracaoApi, UUID> {
    Optional<IntegracaoApi> findByKeyId(String keyId);

    List<IntegracaoApi> findAllByOrderByCriadaEmDesc();
}
