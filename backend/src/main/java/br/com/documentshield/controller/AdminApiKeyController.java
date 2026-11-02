package br.com.documentshield.controller;

import br.com.documentshield.dto.request.CreateApiKeyRequest;
import br.com.documentshield.dto.response.*;
import br.com.documentshield.service.ApiKeyManagementService;
import br.com.documentshield.service.ApiKeyManagementService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
@Tag(name = "Administração de API Keys")
@SecurityRequirement(name = "adminSession")
public class AdminApiKeyController {
    private final ApiKeyManagementService service;

    public AdminApiKeyController(ApiKeyManagementService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista credenciais sem expor segredo ou hash")
    public PagedResponse<AdminApiKeyResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String status) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Paginação inválida.");
        String normalizedName = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        List<AdminApiKeyResponse> filtered = service.list().stream()
                .map(this::response)
                .filter(value -> normalizedName.isEmpty() || value.nome().toLowerCase(Locale.ROOT).contains(normalizedName))
                .filter(value -> normalizedStatus.isEmpty() || value.status().equals(normalizedStatus))
                .toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new PagedResponse<>(filtered.subList(from, to), page, size, filtered.size(), totalPages,
                page == 0, page >= Math.max(0, totalPages - 1));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma credencial e apresenta o token uma única vez")
    public ResponseEntity<CreatedApiKeyResponse> create(@Valid @RequestBody CreateApiKeyRequest request) {
        if (!ApiKeyManagementService.DOCUMENT_ANALYZE_SCOPE.equals(request.escopo())) {
            throw new IllegalArgumentException("O escopo permitido é DOCUMENT_ANALYZE.");
        }
        CreatedApiKey created = service.create(request.nome(), request.descricao(), request.expiraEm(),
                request.escopo(), request.requisicoesPorMinuto());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(new CreatedApiKeyResponse(response(created.credential()), created.token()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os metadados de uma credencial")
    public AdminApiKeyResponse get(@PathVariable UUID id) {
        return response(service.get(id));
    }

    @PostMapping("/{id}/revogar")
    @Operation(summary = "Revoga imediatamente uma credencial")
    public AdminApiKeyResponse revoke(@PathVariable UUID id) {
        return response(service.revoke(id));
    }

    @PostMapping("/{id}/rotacionar")
    @Operation(summary = "Substitui uma credencial e revoga a anterior na mesma transação")
    public ResponseEntity<CreatedApiKeyResponse> rotate(@PathVariable UUID id) {
        CreatedApiKey created = service.rotate(id, null, false);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CreatedApiKeyResponse(response(created.credential()), created.token()));
    }

    private AdminApiKeyResponse response(ApiKeyView value) {
        String status = !value.active() || value.revokedAt() != null ? "REVOGADA"
                : value.expiresAt() != null && !value.expiresAt().isAfter(Instant.now()) ? "EXPIRADA" : "ATIVA";
        return new AdminApiKeyResponse(value.id(), value.name(), value.description(), value.publicId(),
                "dsk_" + value.publicId().substring(0, 8) + "…", value.scope(), status,
                value.createdAt(), value.expiresAt(), value.lastUsedAt(), value.revokedAt(),
                value.requestsPerMinute());
    }
}
