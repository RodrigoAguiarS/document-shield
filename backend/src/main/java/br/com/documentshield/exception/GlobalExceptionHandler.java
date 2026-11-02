package br.com.documentshield.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.net.URI;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException e) {
        return detail(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail routeNotFound(NoResourceFoundException e) {
        return detail(HttpStatus.NOT_FOUND, "Recurso não encontrado", "A rota solicitada não existe.");
    }

    @ExceptionHandler({ InvalidDocumentException.class, MethodArgumentNotValidException.class })
    ProblemDetail invalid(Exception e) {
        return detail(HttpStatus.UNPROCESSABLE_ENTITY, "Documento ou solicitação inválida", safe(e));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(Exception e) {
        return detail(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite", "O limite de upload é 20 MB.");
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    ProblemDetail tooLargePublic(PayloadTooLargeException e) {
        return detail(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite", e.getMessage());
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    ProblemDetail unsupported(UnsupportedDocumentTypeException e) {
        return detail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado", e.getMessage());
    }

    @ExceptionHandler(PublicAnalysisTimeoutException.class)
    ProblemDetail timeout(PublicAnalysisTimeoutException e) {
        return detail(HttpStatus.GATEWAY_TIMEOUT, "Tempo de análise excedido", e.getMessage());
    }

    @ExceptionHandler(ProcessingCapacityException.class)
    ProblemDetail capacity(Exception e) {
        return detail(HttpStatus.SERVICE_UNAVAILABLE, "Capacidade temporariamente esgotada", e.getMessage());
    }

    @ExceptionHandler(ApiKeyConflictException.class)
    ProblemDetail conflict(ApiKeyConflictException e) {
        return detail(HttpStatus.CONFLICT, "Operação não permitida", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail bad(Exception e) {
        return detail(HttpStatus.BAD_REQUEST, "Solicitação inválida", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail missingPart(MissingServletRequestPartException e) {
        return detail(HttpStatus.BAD_REQUEST, "Solicitação inválida", "A parte multipart 'arquivo' é obrigatória.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        return detail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "A operação não pôde ser concluída. Consulte os logs pelo identificador da requisição.");
    }

    private ProblemDetail detail(HttpStatus s, String title, String d) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(s, d);
        p.setTitle(title);
        p.setType(URI.create("urn:document-shield:error:" + s.value()));
        p.setProperty("timestamp", java.time.Instant.now());
        return p;
    }

    private String safe(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m.substring(0, Math.min(m.length(), 500));
    }
}
