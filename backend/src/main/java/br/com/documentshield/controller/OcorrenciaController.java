package br.com.documentshield.controller;

import br.com.documentshield.dto.response.*;
import br.com.documentshield.service.QueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;

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
