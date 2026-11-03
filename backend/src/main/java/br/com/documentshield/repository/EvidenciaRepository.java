package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Evidencia;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaRepository extends JpaRepository<Evidencia, UUID> {
    List<Evidencia> findByOcorrenciaIdOrderByDataRegistroAsc(UUID ocorrenciaId);

    void deleteByOcorrenciaIdIn(Collection<UUID> ids);
}
