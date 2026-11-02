package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.AnaliseDocumento;
import br.com.documentshield.domain.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface AnaliseDocumentoRepository extends JpaRepository<AnaliseDocumento, UUID> {
    Page<AnaliseDocumento> findByDocumentoId(UUID documentoId, Pageable pageable);

    Page<AnaliseDocumento> findByStatus(StatusAnalise status, Pageable pageable);

    Page<AnaliseDocumento> findByNivelRisco(NivelRisco risk, Pageable pageable);

    Page<AnaliseDocumento> findByStatusAndNivelRisco(StatusAnalise status, NivelRisco risk, Pageable pageable);

    List<AnaliseDocumento> findByStatusIn(Collection<StatusAnalise> statuses);

    long countByStatus(StatusAnalise status);

    long countByNivelRisco(NivelRisco nivelRisco);

    boolean existsByDocumentoIdAndStatusIn(UUID documentoId, Collection<StatusAnalise> statuses);

    @Query("select count(distinct a.documentoId) from AnaliseDocumento a where a.nivelRisco = :risk")
    long countDistinctDocumentsByRisk(NivelRisco risk);
}
