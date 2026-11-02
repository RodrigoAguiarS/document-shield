package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.RegraDeteccao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RegraDeteccaoRepository extends JpaRepository<RegraDeteccao, UUID> {
    Optional<RegraDeteccao> findByCodigo(String codigo);

    List<RegraDeteccao> findByAtivaTrueOrderByCodigoAsc();
}
