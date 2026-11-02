package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.domain.enums.StatusDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    long countByStatus(StatusDocumento status);

    Page<Documento> findByStatus(StatusDocumento status, Pageable pageable);
}
