package br.com.documentshield.controller;

import br.com.documentshield.dto.response.RegraResponse;
import br.com.documentshield.service.QueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regras")
@Tag(name = "Regras")
public class RegraController {
    private final QueryService q;

    public RegraController(QueryService q) {
        this.q = q;
    }

    @GetMapping
    public List<RegraResponse> list() {
        return q.rules();
    }

    @GetMapping("/{id}")
    public RegraResponse get(@PathVariable UUID id) {
        return q.rule(id);
    }
}
