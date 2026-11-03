# Design system — Document Shield

O produto usa a linguagem de um console profissional de monitoramento de segurança: escuro, preciso e denso, sem estética sci-fi, neon ou aparência de template SaaS. Tabelas, faixas métricas e inspetores técnicos são os contêineres principais; cards decorativos e sombras amplas não fazem parte do sistema.

## Identidade e tokens

- Midnight Black: `#030407`, base dominante da aplicação, sidebar e canvas.
- Electric Cobalt Blue: `#1558C9`, ações primárias, foco, seleção e navegação ativa.
- Acid Lime: `#D4FF24`, usado apenas em sinais pontuais de identidade e destaque.
- Superfícies: `#0F131A`, `#151B24` e `#1D2530`.
- Bordas: `#293342`; borda enfatizada: `#3A4759`.
- Texto principal: `#F5F7FA`; secundário: `#AEB8C7`; desabilitado: `#778394`.
- Risco: informativo `#9AACC2`, baixo `#53D383`, médio `#FFC247`, alto `#FF8146`, crítico `#FF5868`.
- Tipografia de interface: Segoe UI Variable/Segoe UI/system; texto técnico usa Cascadia Mono/Consolas.
- Geometria: raios de 5–7 px, linhas finas e sombra praticamente ausente.

Os tokens são centralizados em `frontend/src/styles.css`. O preset PrimeNG é definido em `frontend/src/app/app.config.ts`, com dark mode ativado pela classe `app-dark`.

## Componentes e estados

`RiskBadge` e `StatusBadge` combinam cor, ícone ou ponto e rótulo textual; a severidade nunca depende apenas de cor. Tabelas são a visualização primária para documentos, análises e ocorrências. `LoadingState`, `EmptyState` e `ErrorState` têm hierarquia semântica e mensagens explícitas. O upload possui repouso, drag-active, progresso, erro e sucesso.

A seção `Integrações → API Keys` mantém a mesma anatomia: aviso de segurança compacto, filtros na cabeça do painel, tabela operacional e ações por linha. Status de credencial combina ponto, cor e texto. Criação, detalhes e segredo usam diálogos PrimeNG; o segredo fica em memória somente enquanto o diálogo está aberto. O Acid Lime marca o sucesso da geração e o prefixo técnico, sem substituir o Cobalt das ações.

`Integrações → API Playground` usa uma bancada técnica: requisição e resposta lado a lado em desktop, com a central do desenvolvedor em fluxo documental abaixo. Estados de envio, processamento, conclusão e erro substituem a mesma área de resposta, evitando pilhas de cards. Resumo, ocorrências e JSON são abas operacionais; código usa superfície monoespaçada, ação de cópia explícita e placeholders, nunca a chave informada pelo usuário.

A tela de login administrativo é um painel único e funcional, sem hero, ilustração ou cards decorativos. Em mobile, filtros se reorganizam e a tabela mantém rolagem horizontal contida, sem provocar overflow da página.

Foco usa anel azul visível por `box-shadow`. Conteúdo extraído de PDFs é renderizado como texto ou `pre`, nunca por `innerHTML`. Animações ficam abaixo de 200 ms e respeitam `prefers-reduced-motion`.

## Responsividade

Em telas abaixo de 760 px, a sidebar vira drawer com alvo de toque de 44 px e estado `aria-expanded`. Métricas formam grade 2 × 4. A tabela do dashboard mantém apenas documento, risco e ação; tabelas detalhadas preservam sua anatomia com rolagem contida. O inspetor de ocorrências cai abaixo da tabela.

No Playground, as colunas de requisição e resposta passam a uma única coluna. Resumos se reorganizam sem rolagem horizontal, código mantém rolagem contida e ações preservam alvo mínimo de toque.

Conceitos visuais vigentes: `docs/frontend/concepts/dashboard-dark.png` e `docs/frontend/concepts/analysis-detail-dark.png`.

## Políticas

A área `Políticas` preserva a anatomia operacional do console: tabela de versões correntes, editor de faixas, simulador e vínculo por integração. `PolicyDecisionBadge` combina texto e cor semântica — verde para `ALLOW`, âmbar para `REVIEW` e vermelho para `BLOCK`. A decisão aparece separada do nível de risco no Playground para não confundir diagnóstico técnico com ação configurada.

## Decisões do redesign

- O produto é tratado como console operacional, não como landing page ou dashboard de cards. Faixas métricas, tabelas, timeline e inspetor técnico formam a estrutura principal.
- O Acid Lime aparece apenas no emblema, no estado de conexão e em sinais pontuais do upload. Ações permanecem em Cobalt Blue e níveis de risco mantêm cores semânticas próprias.
- Superfícies não usam gradientes, blur ou glow. A profundidade vem da escala `#030407` → `#0F131A` → `#151B24` → `#1D2530`, com bordas discretas.
- A tipografia usa fontes do sistema para evitar download, reduzir deslocamento de layout e manter boa cobertura de português. Conteúdo técnico permanece monoespaçado.
- Controles têm altura mínima de 44 px em telas pequenas; selects usam 16 px no mobile para impedir zoom automático no iOS.
- Sidebar vira drawer abaixo de 760 px. Tabelas detalhadas preservam colunas por rolagem contida; somente a tabela resumida do dashboard reduz colunas.
- Conteúdo proveniente de PDFs é sempre exibido por interpolação textual e elementos `pre`; não se usa `innerHTML`, execução de scripts ou ativação de links extraídos.

## Contraste e estados

- Texto principal `#F5F7FA` sobre Midnight Black e superfícies escuras é o padrão de leitura.
- Botões primários usam texto branco sobre Cobalt Blue; foco usa `#76A3FF` com anel externo visível.
- Acid Lime usa texto escuro quando aparece como fundo de seleção e nunca substitui sucesso, alerta, erro ou severidade.
- Risco e status combinam texto, ícone ou ponto e cor. Estados disabled reduzem contraste e removem interação, mas nunca são a única explicação de um fluxo.

## Validação visual

O script `frontend/scripts/visual-qa.mjs` valida as rotas principais em 1920 × 1080, 1366 × 768 e 390 × 844. As capturas são gravadas no diretório temporário do sistema, fora do repositório.
