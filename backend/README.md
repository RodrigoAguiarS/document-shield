# Document Shield Backend

Requisitos: Java 21. O Maven Wrapper está incluído.

No PowerShell:

```powershell
cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

No Linux/macOS: `./mvnw clean verify` e `./mvnw spring-boot:run`.

API: `http://localhost:8080/api/v1`; Swagger: `http://localhost:8080/swagger-ui/index.html`; H2 em disco: `./data/document-shield`; PDFs: `./data/documents`. Para habilitar o console H2 local, use `--spring.profiles.active=dev`.

Exemplo:

```bash
curl -F "arquivo=@documento.pdf;type=application/pdf" http://localhost:8080/api/v1/documentos
```

As rotas internas do MVP não possuem autenticação, escutam apenas em loopback e não devem ser expostas diretamente à internet. A rota síncrona `POST /api/v1/public/analises` possui autenticação própria por API Key e não persiste o PDF ou seus resultados. Provisionamento, cURL, limites, privacidade e publicação por HTTPS estão em `../docs/PUBLIC-API.md`.

O gerenciamento de API Keys em `/api/v1/admin/**` usa Spring Security, sessão no servidor e CSRF. Configure `DOCUMENT_SHIELD_ADMIN_USERNAME` e `DOCUMENT_SHIELD_ADMIN_PASSWORD_HASH` antes de iniciar. O hash BCrypt e o procedimento de recuperação estão documentados em `../docs/PUBLIC-API.md`; não há senha administrativa padrão no código.
