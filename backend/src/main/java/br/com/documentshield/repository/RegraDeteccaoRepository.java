package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.RegraDeteccao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegraDeteccaoRepository extends JpaRepository<RegraDeteccao, UUID> {
    Optional<RegraDeteccao> findByCodigo(String codigo);

    List<RegraDeteccao> findByAtivaTrueOrderByCodigoAsc();
}
