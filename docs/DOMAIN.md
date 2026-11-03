# Domínio

As seis entidades são `Documento`, `AnaliseDocumento`, `Ocorrencia`, `Evidencia`, `RegraDeteccao` e `ExecucaoRegra`. Todas usam UUID. Não existem relacionamentos JPA: referências são colunas UUID com chaves estrangeiras declaradas em `schema.sql`.

Um documento possui histórico de análises. Cada análise possui ocorrências e execuções das regras. Uma ocorrência pode possuir evidências técnicas e, quando aplicável, referencia a regra que a gerou. A execução distingue `NAO_EXECUTADA`, `EXECUTADA_COM_SUCESSO` e `FALHOU`; na versão atual, somente regras ativas são materializadas, e todas as materializadas foram tentadas.

Estados do documento: `RECEBIDO`, `VALIDANDO`, `PRONTO`, `REJEITADO`, `ERRO`. Estados da análise: `PENDENTE`, `EXECUTANDO`, `CONCLUIDA`, `FALHOU`. Os demais enums estão integralmente publicados no OpenAPI.

## Políticas de segurança

O módulo acrescenta `SecurityPolicy`, `SecurityPolicyRule`, `IntegrationPolicy` e `PolicyAudit`. Uma política possui chave estável, versão, tipo `GLOBAL|INTEGRACAO` e estado `RASCUNHO|ATIVA|ARQUIVADA`. Suas regras mapeiam faixas inclusivas de pontuação para `ALLOW|REVIEW|BLOCK`. `IntegrationPolicy` vincula o UUID de uma credencial ao UUID de uma política específica ativa; `PolicyAudit` registra as mudanças administrativas.

Como nas entidades analíticas, não há associações JPA: as referências são UUIDs e a integridade, incluindo a unicidade do vínculo por integração, é garantida por constraints em `schema.sql`.
