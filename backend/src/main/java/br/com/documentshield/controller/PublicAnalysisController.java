package br.com.documentshield.controller;

import br.com.documentshield.dto.response.PublicAnalysisResponse;
import br.com.documentshield.security.PublicApiKeyFilter;
import br.com.documentshield.security.PublicApiMonitoringFilter;
import br.com.documentshield.service.PublicDocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/analises")
@Tag(name = "API pública")
@SecurityRequirement(name = "publicApiKey")
public class PublicAnalysisController {
    private final PublicDocumentAnalysisService service;

    public PublicAnalysisController(PublicDocumentAnalysisService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Analisa um PDF de forma síncrona e sem persistência")
    public ResponseEntity<PublicAnalysisResponse> analyze(
            @RequestPart("arquivo") MultipartFile file, HttpServletRequest request) {
        UUID integrationId = (UUID) request.getAttribute(PublicApiKeyFilter.CREDENTIAL_ATTRIBUTE);
        PublicAnalysisResponse result = service.analyze(file, integrationId);
        request.setAttribute(PublicApiMonitoringFilter.PROCESSING_DURATION_ATTRIBUTE, result.duracaoMilissegundos());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(result);
    }
}
