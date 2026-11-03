package br.com.documentshield.config;

import br.com.documentshield.domain.enums.StatusAnalise;
import br.com.documentshield.repository.AnaliseDocumentoRepository;
import br.com.documentshield.service.AnalysisProcessor;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AnalysisRecovery {
    private final AnaliseDocumentoRepository repo;
    private final AnalysisProcessor processor;

    public AnalysisRecovery(AnaliseDocumentoRepository r, AnalysisProcessor p) {
        repo = r;
        processor = p;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        repo.findByStatusIn(List.of(StatusAnalise.PENDENTE, StatusAnalise.EXECUTANDO))
                .forEach(a -> processor.process(a.getId()));
    }
}
