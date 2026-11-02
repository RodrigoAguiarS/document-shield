package br.com.documentshield.controller;

import br.com.documentshield.dto.response.PublicAnalysisResponse;
import br.com.documentshield.service.PublicDocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.documentshield.security.PublicApiMonitoringFilter;
import jakarta.servlet.http.HttpServletRequest;

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
    public ResponseEntity<PublicAnalysisResponse> analyze(@RequestPart("arquivo") MultipartFile file,
            HttpServletRequest request) {
        PublicAnalysisResponse result = service.analyze(file);
        request.setAttribute(PublicApiMonitoringFilter.PROCESSING_DURATION_ATTRIBUTE, result.duracaoMilissegundos());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(result);
    }
}
