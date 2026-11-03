# PDF Evidence Explorer

## Objetivo

O PDF Evidence Explorer correlaciona o documento renderizado com ocorrências de uma execução específica do motor. A rota é `/documentos/:id/explorer`; os parâmetros opcionais `analise` e `ocorrencia` mantêm a seleção histórica e visual na URL.

A tela reutiliza:

- `GET /api/v1/documentos/{id}/arquivo` para os bytes do PDF;
- `GET /api/v1/documentos/{id}/analises` para o histórico;
- `GET /api/v1/analises/{id}/ocorrencias` para a execução selecionada;
- `GET /api/v1/ocorrencias/{id}/evidencias` somente após seleção.

Nenhum CRUD de resultado foi criado. Ocorrências e evidências continuam sendo produzidas exclusivamente pelo motor.

## Fluxo

1. Carregar documento, histórico e arquivo.
2. Escolher a análise indicada na URL ou a concluída mais recente.
3. Esvaziar resultados anteriores antes de buscar a nova execução.
4. Renderizar apenas a página corrente no PDF.js.
5. Sobrepor somente geometrias completas e confiáveis.
6. Sincronizar seleção da lista e do marcador e carregar evidências sob demanda.

Análises `PENDENTE`, `EXECUTANDO` ou `FALHOU` permanecem selecionáveis, mas não são tratadas como resultados completos. Trocar a execução nunca concatena ocorrências.

## Contrato de coordenadas

O backend persiste novas caixas visuais em `PDFBOX_ROTATED_CROP_TOP_LEFT`:

- `posicaoX`, `posicaoY`: canto superior esquerdo da caixa;
- `largura`, `altura`: dimensões da caixa;
- `larguraPagina`, `alturaPagina`: dimensões visuais do `CropBox` após a rotação intrínseca;
- `rotacaoPagina`: rotação intrínseca normalizada;
- `localizacaoConfiavel`: autorização explícita para desenhar o marcador.

O espaço é independente de DPI. Sem rotação adicional:

```text
left   = x / larguraPagina * larguraViewport
top    = y / alturaPagina  * alturaViewport
width  = largura / larguraPagina * larguraViewport
height = altura  / alturaPagina  * alturaViewport
```

Para rotação adicional de 90 graus:

```text
x' = alturaPagina - y - altura
y' = x
w' = altura
h' = largura
```

As transformações de 180 e 270 graus ficam em `evidence-coordinate.ts` e têm testes. Não existe constante fixa de ponto para pixel.

### CropBox, MediaBox e rotação

O extrator usa o `CropBox`, não o `MediaBox`, transforma a linha de base em topo da caixa e troca as dimensões visuais em 90/270 graus. O teste `normalizaCoordenadasParaCropBoxVisualComRotacao` cria um PDF real com origem deslocada e página rotacionada.

### Caixas externas e ausência de localização

Uma caixa confiável que excede os limites mantém suas coordenadas. O viewer mostra um indicador tracejado na borda mais próxima e a lista informa “Fora da área visível”.

Se `localizacaoConfiavel` não for `true`, o espaço for desconhecido ou algum valor necessário estiver nulo, o frontend não cria marcador. A ocorrência permanece no rail como “Sem localização visual confiável”. Isso cobre análises históricas anteriores ao contrato sem recalcular ou inventar posição.

## Visualizador

O frontend usa `pdfjs-dist` diretamente no chunk lazy do explorer:

- página única, anterior/próxima e entrada numérica;
- zoom de 25% a 400% e ajustes de largura/página;
- rotação em passos de 90 graus;
- seleção de texto e download do original;
- atalhos `PageUp`, `PageDown`, `+`, `-` e `R`.

O canvas limita a escala de saída a 2 em HiDPI. Um `ResizeObserver` recalcula o ajuste, e a renderização anterior é cancelada antes da próxima.

## Segurança

PDFs são dados hostis. O explorer:

- entrega bytes locais ao worker;
- não instancia anotações, links ou scripting manager;
- desabilita XFA, WebAssembly e fetch auxiliar do worker;
- não ativa anexos, ações ou JavaScript incorporado;
- exibe extrações por interpolação e `pre`, nunca por `innerHTML`;
- distribui o worker como asset local;
- destrói a tarefa do PDF.js ao trocar arquivo ou desmontar;
- revoga a URL temporária de download após o disparo.

O frontend evita superfícies de conteúdo ativo. PDFs adversariais ainda dependem dos limites de arquivo, páginas, CPU e memória do backend e da infraestrutura.

## Estados, acessibilidade e responsividade

Há estados para carga, erro de API, erro de renderização, análise vazia, filtro vazio, evidência sob demanda e posição ausente. Controles possuem rótulos, foco visível e alvos de 44 px em mobile. A lista aceita setas e mudanças de página usam `aria-live`.

- 1920×1080: canvas amplo e rail de 23 rem;
- 1366×768: workspace compacto com viewer e inspetor;
- 390×844: fluxo vertical sem overflow horizontal da página.

`frontend/scripts/visual-qa.mjs` registra screenshots no diretório temporário e verifica console e overflow nos três viewports.

## Validação

- Backend: PDF sintético real com `CropBox` deslocado e rotação.
- Frontend: escala proporcional, rotação adicional, caixa externa e geometria incompleta.
- Build: PDF.js carregado somente pela rota lazy.
- Browser: desktop largo, notebook e mobile.

Comandos:

```text
backend:  ./mvnw clean verify
frontend: npm run lint
frontend: npm run format:check
frontend: npm test -- --watch=false --browsers=ChromeHeadless
frontend: npm run build
frontend: node scripts/visual-qa.mjs
```
