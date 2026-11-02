package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.IntegracaoApi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface IntegracaoApiRepository extends JpaRepository<IntegracaoApi, UUID> {
    Optional<IntegracaoApi> findByKeyId(String keyId);
    List<IntegracaoApi> findAllByOrderByCriadaEmDesc();
}
