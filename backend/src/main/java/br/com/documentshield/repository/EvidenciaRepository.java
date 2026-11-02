package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EvidenciaRepository extends JpaRepository<Evidencia, UUID> {
    List<Evidencia> findByOcorrenciaIdOrderByDataRegistroAsc(UUID ocorrenciaId);

    void deleteByOcorrenciaIdIn(Collection<UUID> ids);
}
