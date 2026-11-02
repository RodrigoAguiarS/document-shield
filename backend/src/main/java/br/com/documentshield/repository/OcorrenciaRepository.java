package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Ocorrencia;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {
    Page<Ocorrencia> findByAnaliseId(UUID analiseId, Pageable pageable);

    List<Ocorrencia> findAllByAnaliseId(UUID analiseId);

    long countByAnaliseId(UUID analiseId);

    void deleteByAnaliseId(UUID id);
}
