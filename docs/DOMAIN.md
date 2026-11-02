# Domínio

As seis entidades são `Documento`, `AnaliseDocumento`, `Ocorrencia`, `Evidencia`, `RegraDeteccao` e `ExecucaoRegra`. Todas usam UUID. Não existem relacionamentos JPA: referências são colunas UUID com chaves estrangeiras declaradas em `schema.sql`.

Um documento possui histórico de análises. Cada análise possui ocorrências e execuções das regras. Uma ocorrência pode possuir evidências técnicas e, quando aplicável, referencia a regra que a gerou. A execução distingue `NAO_EXECUTADA`, `EXECUTADA_COM_SUCESSO` e `FALHOU`; na versão atual, somente regras ativas são materializadas, e todas as materializadas foram tentadas.

Estados do documento: `RECEBIDO`, `VALIDANDO`, `PRONTO`, `REJEITADO`, `ERRO`. Estados da análise: `PENDENTE`, `EXECUTANDO`, `CONCLUIDA`, `FALHOU`. Os demais enums estão integralmente publicados no OpenAPI.
