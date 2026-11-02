# API Playground e Central do Desenvolvedor

O Playground fica em `Integrações → API Playground` ou em `/integracoes/playground`. A página exige uma sessão administrativa válida para ser aberta. A execução da análise tem autenticação independente: cada chamada para a API pública exige uma API Key com escopo `DOCUMENT_ANALYZE`.

## Executar uma análise

1. Gere ou rotacione uma credencial em `Integrações → API Keys` e copie o token exibido uma única vez.
2. Abra o Playground, cole a API Key e selecione um PDF de até 20 MB.
3. Clique em **Executar análise**. O painel diferencia o envio do arquivo do processamento no servidor e permite cancelar a requisição no navegador.
4. Consulte o resumo, as ocorrências e evidências ou a resposta JSON integral.

A chamada real é `POST /api/v1/public/analises`, usa `multipart/form-data` com a parte `arquivo` e envia a credencial em `Authorization: Bearer <token>`. O frontend não define manualmente o `Content-Type`, pois o navegador precisa gerar o boundary do multipart.

## Privacidade e segurança

- A API Key permanece somente na memória do componente; não é gravada em storage, URL, logs ou documentação.
- O campo é limpo depois de sucesso, erro, cancelamento e ao sair da página.
- O Playground não mantém histórico. A API pública também não persiste o PDF nem o resultado.
- Conteúdo, ocorrências e evidências extraídos do PDF são tratados como dados hostis e renderizados apenas como texto inerte, nunca com `innerHTML`.
- Cancelar a requisição interrompe a espera do navegador, mas não garante a interrupção imediata de um parser que já esteja em execução no servidor.

## Interpretação da resposta

O cabeçalho mostra separadamente o status HTTP, a duração observada pelo cliente e a duração informada pelo motor. O resumo apresenta risco, pontuação, páginas, ocorrências, regras e identificador de correlação. Campos visuais nulos significam que a propriedade não foi extraída com segurança; não equivalem a zero.

Erros seguem Problem Details. O painel preserva status, título, detalhe, tipo, correlação e `Retry-After`, quando disponíveis. Os casos mais frequentes são `400` requisição inválida, `401` chave ausente ou inválida, `403` escopo inválido, `413` limite excedido, `415` formato incorreto, `422` PDF não analisável, `429` rate limit e `503/504` indisponibilidade ou timeout.

## Central do Desenvolvedor

A própria página documenta autenticação, upload, resposta, níveis de risco, erros e limites. Exemplos copiáveis de cURL, Java 21, Python e JavaScript usam placeholders e não incorporam o segredo digitado. O contrato detalhado e as instruções de provisionamento permanecem em [PUBLIC-API.md](PUBLIC-API.md).
