package br.com.documentshield.controller;

import br.com.documentshield.dto.response.DashboardResponse;
import br.com.documentshield.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService s) {
        service = s;
    }

    @GetMapping("/resumo")
    public DashboardResponse summary() {
        return service.summary();
    }
}
