# Policy Engine

O Policy Engine transforma a pontuação de risco já calculada em uma decisão operacional determinística: `ALLOW`, `REVIEW` ou `BLOCK`. Ele não executa novamente o extrator, as regras de detecção ou o cálculo de risco.

## Política global padrão

Na primeira inicialização, o backend garante uma política global ativa com cobertura completa:

| Pontuação | Decisão |
|---:|---|
| 0–29 | `ALLOW` |
| 30–49 | `REVIEW` |
| 50–100 | `BLOCK` |

As faixas são inclusivas. Uma política só pode ser ativada quando cobre exatamente 0–100, sem lacunas, sobreposições ou valores fora desse intervalo. A ordem das regras é persistida e a regra aplicada integra a resposta.

## Resolução e fallback

Cada API Key representa uma integração. Se existir uma política específica ativa vinculada à integração, ela é aplicada; caso contrário, vale a política global ativa. Uma referência específica inválida ou indisponível também recua de forma segura para a global. Se nenhuma política global válida puder ser resolvida, a análise pública falha com `503`, em vez de produzir uma decisão ambígua.

A rotação da API Key transfere o vínculo para a credencial substituta na mesma transação. A remoção do vínculo restaura o fallback global.

## Ciclo de vida e versionamento

Políticas possuem tipo `GLOBAL` ou `INTEGRACAO` e estado `RASCUNHO`, `ATIVA` ou `ARQUIVADA`. Editar um rascunho altera a mesma versão. Editar uma política ativa ou arquivada cria uma nova versão em rascunho, preservando o histórico. A ativação arquiva a versão ativa anterior do mesmo conjunto e usa bloqueio transacional e versão otimista para impedir ativações concorrentes inconsistentes.

Operações de criação, nova versão, ativação, arquivamento, vínculo e desvínculo geram auditoria com ator e data. Segredos, conteúdo de PDF e caminhos físicos não integram esses registros.

## Administração

Todas as rotas abaixo exigem sessão administrativa; mutações também exigem o token CSRF:

- `GET/POST /api/v1/admin/politicas`
- `GET/PUT /api/v1/admin/politicas/{id}`
- `POST /api/v1/admin/politicas/{id}/ativar`
- `POST /api/v1/admin/politicas/{id}/arquivar`
- `GET /api/v1/admin/politicas/{id}/versoes`
- `POST /api/v1/admin/politicas/{id}/simular`
- `GET /api/v1/admin/politicas/auditoria`
- `GET/PUT/DELETE /api/v1/admin/integracoes/{integracaoId}/politica`

O simulador recebe somente uma pontuação e usa o mesmo motor do fluxo público, sem analisar ou persistir PDF.

## Responsabilidade do consumidor

`BLOCK` é uma decisão recomendada pela política selecionada. O Document Shield informa a decisão, mas a integração consumidora é responsável por efetivamente bloquear, encaminhar para revisão ou permitir seu próprio fluxo. A decisão e a política aplicada não são persistidas pelo endpoint público.

## Testes

A suíte backend cobre os limites 0, 29, 30, 49, 50 e 100, determinismo concorrente, faixas inválidas, autenticação administrativa, ativação, versionamento, simulação, vínculo, fallback global, auditoria e integração com a API pública. A suíte frontend cobre contratos HTTP, validação visual do editor e apresentação semântica da decisão. O QA renderizado valida a área administrativa em 1920×1080, 1366×768 e 390×844.

## Limitações

- As regras atuais são exclusivamente faixas inteiras de 0 a 100; não há scripts, expressões ou SQL configurável.
- `REVIEW` não cria fila ou workflow de aprovação humana.
- `BLOCK` não executa ações no sistema consumidor.
- A API pública não mantém histórico individual de decisões ou simulações.
- Alterações realizadas fora da API administrativa, diretamente no banco, não produzem auditoria de aplicação.
