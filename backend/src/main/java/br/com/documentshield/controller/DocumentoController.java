package br.com.documentshield.controller;

import br.com.documentshield.converter.ApiConverter;
import br.com.documentshield.dto.response.*;
import br.com.documentshield.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos")
public class DocumentoController {
    private final DocumentService service;
    private final QueryService query;
    private final ApiConverter c;

    public DocumentoController(DocumentService s, QueryService q, ApiConverter c) {
        service = s;
        query = q;
        this.c = c;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envia e enfileira um PDF")
    public UploadResponse upload(@RequestPart("arquivo") MultipartFile file) {
        return service.upload(file);
    }

    @GetMapping
    public PagedResponse<DocumentoResponse> list(
            @RequestParam(required = false) br.com.documentshield.domain.enums.StatusDocumento status,
            @PageableDefault(size = 20, sort = "dataUpload", direction = Sort.Direction.DESC) Pageable p) {
        return PagedResponse.from(service.list(status, p), c::toResponse);
    }

    @GetMapping("/{id}")
    public DocumentoResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID id) {
        DocumentService.FileDownload f = service.download(id);
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(f.filename(), java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(f.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString()).header("X-Content-Type-Options", "nosniff")
                .body(f.resource());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reanalisar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UploadResponse reanalyze(@PathVariable UUID id) {
        return service.reanalyze(id);
    }

    @GetMapping("/{id}/analises")
    public PagedResponse<AnaliseResponse> analyses(@PathVariable UUID id,
            @PageableDefault(size = 20, sort = "dataInicio", direction = Sort.Direction.DESC) Pageable p) {
        service.get(id);
        return PagedResponse.from(query.documentAnalyses(id, p), c::toResponse);
    }
}
