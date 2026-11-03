package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.ExecucaoRegra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoRegraRepository extends JpaRepository<ExecucaoRegra, UUID> {
    List<ExecucaoRegra> findByAnaliseIdOrderByDataExecucaoAsc(UUID analiseId);

    void deleteByAnaliseId(UUID id);
}
