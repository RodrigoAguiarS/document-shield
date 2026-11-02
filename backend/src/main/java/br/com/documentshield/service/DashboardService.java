package br.com.documentshield.service;

import br.com.documentshield.domain.enums.*;
import br.com.documentshield.dto.response.DashboardResponse;
import br.com.documentshield.repository.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DocumentoRepository d;
    private final AnaliseDocumentoRepository a;
    private final OcorrenciaRepository o;

    public DashboardService(DocumentoRepository d, AnaliseDocumentoRepository a, OcorrenciaRepository o) {
        this.d = d;
        this.a = a;
        this.o = o;
    }

    public DashboardResponse summary() {
        long pending = a.countByStatus(StatusAnalise.PENDENTE) + a.countByStatus(StatusAnalise.EXECUTANDO);
        return new DashboardResponse(d.count(), a.count(), pending, a.countByStatus(StatusAnalise.CONCLUIDA),
                a.countByStatus(StatusAnalise.FALHOU), o.count(), a.countDistinctDocumentsByRisk(NivelRisco.ALTO),
                a.countDistinctDocumentsByRisk(NivelRisco.CRITICO));
    }
}
