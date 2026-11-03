package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.domain.enums.StatusDocumento;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    long countByStatus(StatusDocumento status);

    Page<Documento> findByStatus(StatusDocumento status, Pageable pageable);
}
