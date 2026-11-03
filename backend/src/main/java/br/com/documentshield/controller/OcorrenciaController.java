package br.com.documentshield.controller;

import br.com.documentshield.dto.response.EvidenciaResponse;
import br.com.documentshield.dto.response.OcorrenciaResponse;
import br.com.documentshield.service.QueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ocorrencias")
@Tag(name = "Ocorrências")
public class OcorrenciaController {
    private final QueryService q;

    public OcorrenciaController(QueryService q) {
        this.q = q;
    }

    @GetMapping("/{id}")
    public OcorrenciaResponse get(@PathVariable UUID id) {
        return q.finding(id);
    }

    @GetMapping("/{id}/evidencias")
    public List<EvidenciaResponse> evidence(@PathVariable UUID id) {
        return q.evidence(id);
    }
}
