package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.ExecucaoRegra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ExecucaoRegraRepository extends JpaRepository<ExecucaoRegra, UUID> {
    List<ExecucaoRegra> findByAnaliseIdOrderByDataExecucaoAsc(UUID analiseId);

    void deleteByAnaliseId(UUID id);
}
