# API pública sem persistência

## Endpoint e autenticação

`POST /api/v1/public/analises`, com `multipart/form-data` e parte `arquivo`. A resposta é síncrona (`200 OK`) e exige:

```http
Authorization: Bearer dsk_<key-id>.<segredo>
```

Cada chave representa uma integração. O banco guarda somente o SHA-256 do token completo; o segredo não é recuperável. Credenciais suportam ativação, expiração, revogação, rotação, limite por minuto e o escopo `DOCUMENT_ANALYZE`. O escopo legado `PUBLIC_ANALYSIS` permanece válido para chaves já provisionadas. A chave não autoriza nenhum endpoint interno ou administrativo.

## Playground administrativo

Administradores autenticados podem testar esse mesmo endpoint em `http://localhost:4200/integracoes/playground`. A tela envia uma requisição HTTP real, mostra progresso, resultado, ocorrências, evidências e JSON, e inclui exemplos de integração. A sessão administrativa autoriza somente o acesso à tela; a operação continua exigindo uma API Key válida. O token fica apenas em memória e é limpo ao concluir, falhar, cancelar ou sair. Detalhes: [API-PLAYGROUND.md](API-PLAYGROUND.md).

## Como gerar sua primeira API Key

### Pelo painel administrativo

Configure a conta administrativa antes de iniciar o backend. Gere um hash BCrypt sem gravar a senha em arquivo:

```powershell
cd backend
$secure = Read-Host "Senha administrativa (mínimo 12 caracteres)" -AsSecureString
$env:DOCUMENT_SHIELD_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $secure).Password
$hashOutput = .\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.AdminPasswordHashTool"
$env:DOCUMENT_SHIELD_ADMIN_PASSWORD_HASH = $hashOutput | Select-Object -Last 1
$env:DOCUMENT_SHIELD_ADMIN_USERNAME = 'admin'
Remove-Item Env:DOCUMENT_SHIELD_ADMIN_PASSWORD
```

Inicie backend e frontend na mesma sessão e acesse `http://localhost:4200/integracoes/api-keys`. O Angular redireciona para `/login`. Depois de autenticar, clique em **Nova API Key**, informe nome, descrição opcional, expiração e limite. O token aparece em um diálogo somente uma vez e não é salvo em `localStorage` ou `sessionStorage`.

Em produção, armazene `DOCUMENT_SHIELD_ADMIN_USERNAME` e `DOCUMENT_SHIELD_ADMIN_PASSWORD_HASH` no gerenciador de segredos do ambiente. Habilite `DOCUMENT_SHIELD_SESSION_COOKIE_SECURE=true` sob HTTPS. Para recuperar o acesso administrativo, gere outro hash com o mesmo comando, substitua a variável e reinicie o backend; não há recuperação da senha anterior.

Revogação é imediata. Rotação cria uma nova credencial e revoga a anterior na mesma transação; o novo segredo também aparece uma única vez.

### Provisionamento local alternativo pela CLI

A CLI continua disponível para manutenção no servidor e usa o mesmo banco configurado em `application.yml`. Execute-a com a aplicação parada enquanto o arquivo H2 estiver em uso exclusivo.

No PowerShell, entre na pasta do backend e gere uma credencial:

```powershell
cd backend
.\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.ApiKeyProvisioningTool" "-Dexec.args=create --name=Sistema-Financeiro --expires-at=2027-12-31T23:59:59Z --rate-limit=30"
```

`--expires-at` é opcional e usa ISO-8601 UTC. `--rate-limit` também é opcional; quando omitido, vale `document-shield.public-api.rate-limit-per-minute`. A saída contém o UUID administrativo, o identificador público e uma linha `Token: dsk_...`. O token completo é apresentado somente nessa criação. Copie-o imediatamente para um cofre de segredos; o banco armazena apenas SHA-256 e não permite recuperar o segredo.

Para criar uma chave sem expiração:

```powershell
.\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.ApiKeyProvisioningTool" "-Dexec.args=create --name=Sistema-Financeiro"
```

Para listar integrações, status, expiração e último uso, sem mostrar segredo ou hash:

```powershell
.\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.ApiKeyProvisioningTool" "-Dexec.args=list"
```

Copie o `ID` retornado pela criação ou listagem para revogar uma chave:

```powershell
.\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.ApiKeyProvisioningTool" "-Dexec.args=revoke --id=COLE-O-UUID-AQUI"
```

Se o token foi perdido, ele não pode ser recuperado. Rotacione a credencial; uma nova chave é criada e a antiga é revogada na mesma transação:

```powershell
.\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=br.com.documentshield.security.ApiKeyProvisioningTool" "-Dexec.args=rotate --id=COLE-O-UUID-AQUI"
```

A rotação preserva nome, escopo, limite e expiração. Para definir uma nova expiração, acrescente `--expires-at=2027-12-31T23:59:59Z`; para remover a expiração, use `--expires-at=never`. A nova saída mostra o token substituto uma única vez.

### Usar no Postman

1. Crie uma requisição `POST` para `http://127.0.0.1:8080/api/v1/public/analises`.
2. Em **Authorization**, selecione **Bearer Token** e cole somente o valor `dsk_...`.
3. Em **Body**, selecione **form-data**, crie a chave `arquivo`, mude o tipo para **File** e escolha um PDF.
4. Envie a requisição. Não salve nem compartilhe uma collection contendo o token real.

### Usar com cURL

No PowerShell, mantenha o token apenas na sessão atual:

```powershell
$env:DOCUMENT_SHIELD_API_KEY = 'dsk_COLE_AQUI_O_TOKEN_COMPLETO'
curl.exe --fail-with-body -H "Authorization: Bearer $env:DOCUMENT_SHIELD_API_KEY" -F "arquivo=@documento.pdf;type=application/pdf" http://127.0.0.1:8080/api/v1/public/analises
Remove-Item Env:DOCUMENT_SHIELD_API_KEY
```

A CLI usa `SecureRandom` com segredo de 256 bits. O identificador público não é segredo. Nunca coloque o token em arquivos versionados, parâmetros de URL, tickets ou logs.

## Exemplo

```bash
curl --fail-with-body \
  -H "Authorization: Bearer $DOCUMENT_SHIELD_API_KEY" \
  -F "arquivo=@documento.pdf;type=application/pdf" \
  https://shield.example.com/api/v1/public/analises
```

Resposta resumida:

```json
{
  "status": "CONCLUIDA",
  "correlacaoId": "6aa09a7f-29f3-4772-819a-19e288bc9aef",
  "documento": {
    "nome": "documento.pdf",
    "tipoMime": "application/pdf",
    "tamanhoBytes": 32145,
    "hashSha256": "..."
  },
  "quantidadePaginas": 2,
  "versaoMotor": "1.0.0",
  "nivelRisco": "ALTO",
  "pontuacaoRisco": 65,
  "totalOcorrencias": 3,
  "respostaTruncada": false,
  "execucoes": [],
  "ocorrencias": []
}
```

Ocorrências incluem somente propriedades efetivamente extraídas. Coordenadas, fonte, cor e opacidade permanecem `null` quando indisponíveis. Evidências são aninhadas na ocorrência. Não há IDs persistentes; `correlacaoId` e IDs de ocorrência são efêmeros.

## Decisão da política

A resposta inclui, sem remover os campos anteriores:

```json
{
  "nivelRisco": "ALTO",
  "pontuacaoRisco": 65,
  "politica": {
    "id": "8c1ad869-323e-4cc1-a6a8-207bde63bf1a",
    "nome": "Política global padrão",
    "versao": 1,
    "origem": "GLOBAL",
    "decisao": "BLOCK",
    "pontuacaoAvaliada": 65,
    "regraAplicada": {
      "pontuacaoMinima": 50,
      "pontuacaoMaxima": 100,
      "decisao": "BLOCK",
      "descricao": "Bloquear"
    },
    "motivo": "Pontuação 65 enquadrada na faixa 50–100."
  }
}
```

`nivelRisco` descreve o resultado técnico; `politica.decisao` descreve a ação recomendada pela configuração vigente. A integração consumidora deve implementar o efeito de `BLOCK`, `REVIEW` ou `ALLOW`: o Document Shield não bloqueia sistemas externos por conta própria. Uma política específica ativa tem precedência sobre a global; sem vínculo específico, `origem` será `GLOBAL`. A avaliação não é persistida. Se não houver uma política global válida, a API responde `503`.

## Não persistência e privacidade

O fluxo público não usa repositórios de `Documento`, `AnaliseDocumento`, `Ocorrencia`, `Evidencia` ou `ExecucaoRegra`. O PDF é copiado para um diretório temporário controlado, analisado pelo mesmo extrator/motor do fluxo interno e removido pelo worker em `finally`. Resíduos com mais de uma hora são removidos na inicialização.

O multipart do servidor ou o sistema operacional também podem usar disco temporário. Portanto, não se afirma que nenhum byte toca o disco. Para requisitos mais fortes, configure `temp-directory` em volume efêmero criptografado ou RAM disk e ajuste os limites do container.

Textos de ocorrência/evidência são limitados por `max-text-length`; a quantidade de ocorrências e evidências também é limitada. `respostaTruncada=true` informa qualquer truncamento. Conteúdo de PDF nunca é executado, seguido como link ou registrado integralmente em log.

## Limites

| Propriedade | Padrão |
|---|---:|
| `document-shield.public-api.max-file-size` | 20 MB |
| `timeout-seconds` | 30 s |
| `max-concurrency` | 2 |
| `queue-capacity` | 2 |
| `rate-limit-per-minute` | 30 por credencial |
| `max-occurrences` | 200 |
| `max-evidence-per-occurrence` | 50 |
| `max-text-length` | 2000 caracteres |

O timeout limita o tempo de espera HTTP. O trabalho roda em executor isolado; se PDFBox não responder à interrupção, ele pode continuar até fechar o documento e remover o temporário. Para limite rígido de CPU/memória/tempo, execute o backend ou o parser em processo/container isolado.

## Erros

Erros usam `application/problem+json`: `400` solicitação inválida, `401` chave ausente/inválida/inativa/expirada/revogada, `403` escopo insuficiente, `413` tamanho, `415` mídia, `422` PDF inválido/protegido, `429` rate limit, `503` pool/fila esgotados e `504` timeout. Respostas não contêm stack trace, caminhos físicos, token ou conteúdo integral.

## Publicação segura

O servidor continua em `127.0.0.1`. Publique por proxy reverso/gateway com HTTPS, limite de corpo e timeout compatíveis. Permita externamente somente `POST /api/v1/public/analises`; mantenha `/api/v1/documentos`, `/api/v1/analises`, Swagger, H2 e endpoints administrativos inacessíveis pela internet.
