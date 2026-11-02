# Monitoramento da API pública

O Document Shield registra somente métricas operacionais agregadas da API pública. Não existe tabela por requisição e não são persistidos conteúdo do PDF, nome de arquivo, hash, corpo, resposta, token, segredo ou caminho físico.

## Modelo de agregação

Cada chamada atualiza buckets globais e, quando a autenticação identifica uma credencial, buckets da integração. Os mesmos eventos são agregados em duas granularidades UTC:

- `HOUR`: retenção padrão de 90 dias;
- `DAY`: retenção padrão de 365 dias.

As atualizações usam transações curtas, controle otimista de versão e retentativas para colisões concorrentes. Uma falha na coleta é isolada e nunca altera a resposta da API pública. A limpeza ocorre diariamente às 03:17 UTC e pode ser configurada por `document-shield.monitoring.*`.

O P95 é uma estimativa calculada a partir de histograma com limites de 100, 250, 500, 1.000, 2.500, 5.000, 10.000 e 30.000 ms. O valor apresentado é o limite superior do bucket; para o último bucket é usado o maior valor observado.

A taxa de sucesso usa como denominador chamadas elegíveis para análise: total menos falhas de autenticação e respostas de limite de taxa. As contagens brutas continuam disponíveis separadamente.

## Endpoints administrativos

Todos exigem sessão administrativa e retornam DTOs, nunca entidades:

- `GET /api/v1/admin/monitoramento/resumo`
- `GET /api/v1/admin/monitoramento/serie-temporal`
- `GET /api/v1/admin/monitoramento/integracoes`
- `GET /api/v1/admin/monitoramento/integracoes/{id}`
- `GET /api/v1/admin/monitoramento/erros`
- `GET /api/v1/admin/monitoramento/capacidade`

Os endpoints analíticos aceitam `periodo=24H|7D|30D|CUSTOM`, `dataInicio`, `dataFim`, `granularidade=HOUR|DAY` e, quando aplicável, `integracaoId`. Datas usam ISO-8601 com offset. Consultas personalizadas são limitadas a 366 dias.

O endpoint de capacidade verifica conexão com o banco e expõe apenas números do executor público: concorrência configurada, workers ativos/disponíveis e ocupação da fila. O Actuator não foi exposto porque ampliaria desnecessariamente a superfície operacional; o contrato administrativo dedicado fornece apenas os dados exigidos.

## Painel

O menu **Integrações > Monitoramento** consulta dados reais, possui filtros 24h/7d/30d/personalizado e atualiza a cada 30 segundos sem sobrepor chamadas. Uma falha de atualização mantém a última leitura válida identificada como desatualizada.
