# Arquitetura

> Monitoramento operacional e política de agregação: consulte [MONITORING.md](MONITORING.md).

O Document Shield é um monólito modular local. A camada HTTP delega para serviços; serviços coordenam armazenamento e persistência; o motor de detecção depende apenas de `DetectionContext`, não de HTTP.

Fluxo: upload → cópia limitada para arquivo `.part` → validação da assinatura e estrutura via PDFBox → SHA-256 → move atômico → registro do documento/análise → executor limitado → extração → oito regras independentes → cálculo de risco → persistência → finalização.

O pool possui 2 workers e fila de 25 por padrão. A leitura/análise ocorre sem uma transação longa. As gravações de início, resultado e falha usam transações curtas. Jobs `PENDENTE` ou `EXECUTANDO` são reenfileirados ao iniciar a aplicação. Interromper uma thread não é garantia de cancelamento de parser nativo/Java; para PDFs adversariais em ambiente compartilhado, use isolamento por processo/container com limites de CPU e memória.

O H2 usa `jdbc:h2:file:./data/document-shield`. Arquivos ficam em `./data/documents`, fora de recursos públicos. Exclusões de registros respeitam a ordem evidência → ocorrência/execução → análise → documento; o arquivo é removido após o commit.

O servidor escuta somente `127.0.0.1`. As rotas internas permanecem sem autenticação e não devem ser publicadas diretamente na internet; somente a rota pública autenticada pode ser encaminhada por um gateway restritivo.

## Fluxo público transitório

`POST /api/v1/public/analises` passa primeiro pelo filtro servlet de API Key, antes da resolução multipart. Depois da autenticação e do rate limit, o arquivo é copiado com limite e SHA-256 para diretório temporário controlado. Um executor dedicado e limitado executa o mesmo `PdfDocumentExtractor`, `DetectionEngine` e `RiskCalculator` do fluxo persistente. O resultado é convertido diretamente para DTO e o temporário é removido em `finally`.

O serviço público consulta apenas credenciais e códigos de regras ativas. Ele não depende dos repositórios analíticos nem do armazenamento permanente. Timeout HTTP, concorrência e fila são independentes do executor interno. Como uma interrupção de thread não encerra PDFBox de forma garantida, tarefas expiradas podem terminar em isolamento; limite rígido requer processo/container separado. Veja `docs/PUBLIC-API.md`.

O API Playground é uma orquestração exclusivamente frontend sobre esse contrato existente. A rota Angular exige a sessão administrativa, mas não cria um atalho de autorização: o `POST /public/analises` continua protegido pelo filtro de API Key, escopo e rate limit. Arquivo, token e resposta permanecem apenas na memória da página; não há endpoint de simulação, tabela adicional ou persistência de histórico. A documentação e os exemplos usam placeholders e nunca reutilizam o token digitado.
