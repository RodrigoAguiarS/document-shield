# API v1

## Localização visual de ocorrências

As respostas administrativas de ocorrência incluem `larguraPagina`, `alturaPagina`, `rotacaoPagina`, `espacoCoordenadas` e `localizacaoConfiavel`. Para novas caixas confiáveis, `espacoCoordenadas` vale `PDFBOX_ROTATED_CROP_TOP_LEFT`: origem no canto superior esquerdo do `CropBox`, dimensões ajustadas pela rotação intrínseca e unidade de usuário do PDF.

`localizacaoConfiavel=false` ou qualquer campo geométrico nulo proíbe o cliente de inventar um retângulo. Históricos anteriores ao contrato podem retornar os novos campos nulos e continuam válidos, apenas sem overlay. Consulte [PDF-EVIDENCE-EXPLORER.md](PDF-EVIDENCE-EXPLORER.md).

Os contratos administrativos de analytics e capacidade estão descritos em [MONITORING.md](MONITORING.md) e publicados no OpenAPI da aplicação.

Base: `http://localhost:8080/api/v1`. Swagger: `http://localhost:8080/swagger-ui/index.html`. Contrato exportado: `docs/openapi/document-shield-v1.json`.

O upload usa `multipart/form-data`, parte `arquivo`, e retorna HTTP 202:

```json
{"documentoId":"uuid","analiseId":"uuid","statusDocumento":"VALIDANDO","statusAnalise":"PENDENTE","consultaAnalise":"/api/v1/analises/uuid"}
```

Listagens paginadas aceitam `page` (base zero), `size` e `sort=campo,asc|desc`. `GET /documentos` aceita `status`; `GET /analises` aceita `status` e `nivelRisco`. Resposta: `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Datas são ISO-8601 e IDs são UUID.

Endpoints: `POST/GET /documentos`; `GET/DELETE /documentos/{id}`; `GET /documentos/{id}/arquivo`; `POST /documentos/{id}/reanalisar`; `GET /documentos/{id}/analises`; `GET /analises`; `GET /analises/{id}`; `GET /analises/{id}/ocorrencias`; `GET /analises/{id}/execucoes`; `GET /ocorrencias/{id}`; `GET /ocorrencias/{id}/evidencias`; `GET /regras`; `GET /regras/{id}`; `GET /dashboard/resumo`.

Integrações externas usam `POST /public/analises` com API Key Bearer. Esse fluxo é síncrono, não persiste PDF ou resultados e não altera o dashboard. Contrato, provisionamento e limites: [PUBLIC-API.md](PUBLIC-API.md). A interface administrativa oferece um cliente real e uma central de exemplos em `/integracoes/playground`; consulte [API-PLAYGROUND.md](API-PLAYGROUND.md).

## Administração de API Keys

As rotas `/admin/**` usam sessão Spring Security separada das API Keys públicas. O login é processado em `POST /admin/auth/login`; `GET /admin/auth/csrf` inicializa o cookie CSRF, `GET /admin/auth/me` consulta a sessão e `POST /admin/auth/logout` a encerra. A sessão usa `JSESSIONID` HttpOnly e SameSite Strict. Em HTTPS, configure `DOCUMENT_SHIELD_SESSION_COOKIE_SECURE=true`.

Endpoints autenticados: `GET/POST /admin/api-keys`; `GET /admin/api-keys/{id}`; `POST /admin/api-keys/{id}/revogar`; `POST /admin/api-keys/{id}/rotacionar`. Criação e rotação são as únicas respostas que incluem `token`, sempre com `Cache-Control: no-store`. Listagens e detalhes nunca retornam o segredo ou seu hash. Mutações exigem `X-XSRF-TOKEN`.

`GET /admin/api-keys` aceita `page`, `size`, `nome` e `status=ATIVA|EXPIRADA|REVOGADA`. O escopo criado pela interface é `DOCUMENT_ANALYZE`; credenciais legadas `PUBLIC_ANALYSIS` continuam aceitas pela API pública.

Erros seguem RFC 9457 (`application/problem+json`) com `type`, `title`, `status`, `detail` e `timestamp`. Principais status: 202 aceito, 204 excluído, 400 parâmetro inválido, 401 credencial, 403 escopo, 404 ausente, 413 tamanho, 415 formato, 422 PDF inválido/protegido, 429 rate limit, 503 fila cheia, 504 timeout e 500 falha não divulgada.

`PENDENTE` e `EXECUTANDO` significam que resultados ainda não estão completos. `FALHOU` inclui `mensagemErro`. Campos visuais nulos significam “não extraído com segurança”, não zero.

## Políticas de segurança

O endpoint público inclui `politica` com identidade e versão da política aplicada, origem `GLOBAL|INTEGRACAO`, decisão `ALLOW|REVIEW|BLOCK`, pontuação avaliada, faixa aplicada e motivo. A decisão é calculada depois do risco e não substitui `nivelRisco` ou `pontuacaoRisco`.

Endpoints administrativos autenticados: `GET/POST /admin/politicas`; `GET/PUT /admin/politicas/{id}`; `POST /admin/politicas/{id}/ativar`; `POST /admin/politicas/{id}/arquivar`; `GET /admin/politicas/{id}/versoes`; `POST /admin/politicas/{id}/simular`; `GET /admin/politicas/auditoria`; e `GET/PUT/DELETE /admin/integracoes/{integracaoId}/politica`. Mutações exigem `X-XSRF-TOKEN`. Uma configuração incompleta retorna `400`, concorrência otimista retorna `409` e ausência de política global válida retorna `503`. Consulte [POLICY-ENGINE.md](POLICY-ENGINE.md).
