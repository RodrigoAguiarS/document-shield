package br.com.documentshield.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Autenticação administrativa")
public class AdminAuthController {
    @GetMapping("/csrf")
    @Operation(summary = "Inicializa a proteção CSRF para o login administrativo")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "headerName", token.getHeaderName(), "parameterName", token.getParameterName(),
                "token", token.getToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna a sessão administrativa atual")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", authenticated ? authentication.getName() : "");
        body.put("authenticated", authenticated);
        body.put("roles", authenticated
                ? authentication.getAuthorities().stream().map(Object::toString).toList() : List.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
