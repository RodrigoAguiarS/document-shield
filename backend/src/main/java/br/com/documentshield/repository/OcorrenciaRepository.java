package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Ocorrencia;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {
    Page<Ocorrencia> findByAnaliseId(UUID analiseId, Pageable pageable);

    List<Ocorrencia> findAllByAnaliseId(UUID analiseId);

    long countByAnaliseId(UUID analiseId);

    void deleteByAnaliseId(UUID id);
}
