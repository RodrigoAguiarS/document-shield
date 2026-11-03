package br.com.documentshield.controller;

import br.com.documentshield.dto.request.PolicyRequests.BindPolicyRequest;
import br.com.documentshield.dto.response.PolicyResponses;
import br.com.documentshield.service.PolicyManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/integracoes/{integracaoId}/politica")
@Tag(name = "Políticas por integração")
@SecurityRequirement(name = "adminSession")
public class AdminIntegrationPolicyController {
    private final PolicyManagementService policies;

    public AdminIntegrationPolicyController(PolicyManagementService policies) {
        this.policies = policies;
    }

    @GetMapping
    public PolicyResponses.Binding get(@PathVariable UUID integracaoId) {
        return policies.binding(integracaoId);
    }

    @PutMapping
    public PolicyResponses.Binding bind(
            @PathVariable UUID integracaoId, @Valid @RequestBody BindPolicyRequest request, Principal principal) {
        return policies.bind(integracaoId, request.politicaId(), principal.getName());
    }

    @DeleteMapping
    public PolicyResponses.Binding unbind(@PathVariable UUID integracaoId, Principal principal) {
        return policies.unbind(integracaoId, principal.getName());
    }
}
