package br.com.documentshield.controller;

import br.com.documentshield.dto.request.PolicyRequests.SavePolicyRequest;
import br.com.documentshield.dto.request.PolicyRequests.SimulatePolicyRequest;
import br.com.documentshield.dto.response.PolicyResponses;
import br.com.documentshield.service.PolicyManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/politicas")
@Tag(name = "Políticas de segurança")
@SecurityRequirement(name = "adminSession")
public class AdminPolicyController {
    private final PolicyManagementService policies;

    public AdminPolicyController(PolicyManagementService policies) {
        this.policies = policies;
    }

    @GetMapping
    @Operation(summary = "Lista a versão mais recente de cada política")
    public List<PolicyResponses.Policy> list() {
        return policies.listLatest();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma política em rascunho")
    public PolicyResponses.Policy create(@Valid @RequestBody SavePolicyRequest request, Principal principal) {
        return policies.create(request, principal.getName());
    }

    @GetMapping("/auditoria")
    @Operation(summary = "Consulta os eventos administrativos recentes")
    public List<PolicyResponses.Audit> audits() {
        return policies.audits();
    }

    @GetMapping("/{id}")
    public PolicyResponses.Policy get(@PathVariable UUID id) {
        return policies.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um rascunho ou cria nova versão de uma política ativada")
    public PolicyResponses.Policy update(
            @PathVariable UUID id, @Valid @RequestBody SavePolicyRequest request, Principal principal) {
        return policies.update(id, request, principal.getName());
    }

    @PostMapping("/{id}/ativar")
    public PolicyResponses.Policy activate(@PathVariable UUID id, Principal principal) {
        return policies.activate(id, principal.getName());
    }

    @PostMapping("/{id}/arquivar")
    public PolicyResponses.Policy archive(@PathVariable UUID id, Principal principal) {
        return policies.archive(id, principal.getName());
    }

    @GetMapping("/{id}/versoes")
    public List<PolicyResponses.Policy> versions(@PathVariable UUID id) {
        return policies.versions(id);
    }

    @PostMapping("/{id}/simular")
    public PolicyResponses.Evaluation simulate(
            @PathVariable UUID id, @Valid @RequestBody SimulatePolicyRequest request) {
        return policies.simulate(id, request.pontuacao());
    }
}
