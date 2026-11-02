# Solicitações ao backend

## FE-BE-001 — Última análise na listagem de documentos

- Funcionalidade: tabela de documentos.
- Endpoint: `GET /api/v1/documentos`.
- Problema: `Documento` não contém risco, pontuação ou ID da última análise.
- Esperado: resumo opcional da análise mais recente, ou endpoint batch dedicado.
- Impacto: a coluna informa que o dado exige histórico; uma requisição por linha causaria N+1.

## FE-BE-002 — Distribuições do dashboard

- Endpoint: `GET /api/v1/dashboard/resumo`.
- Problema: fornece apenas totais alto/crítico e não agrega tipos.
- Esperado: contagens por todos os `NivelRisco` e `TipoOcorrencia`.
- Impacto: gráficos completos permanecem indisponíveis, sem valores fictícios.

## FE-BE-003 — Identificação do documento nas análises

- Endpoint: `GET /api/v1/analises`.
- Problema: retorna apenas `documentoId`, sem `nomeOriginal`.
- Esperado: resumo do documento ou endpoint batch por IDs.
- Impacto: a tabela exibe o ID abreviado; não realiza N+1.

## FE-BE-004 — Filtros de ocorrências

- Endpoint: `GET /api/v1/analises/{id}/ocorrencias`.
- Problema: só há paginação/ordenação, sem filtros por tipo, risco ou página.
- Esperado: filtros opcionais server-side.
- Impacto: não são apresentados filtros locais sobre uma página incompleta.

## FE-BE-005 — Pageable no OpenAPI

- Problema: o OpenAPI publica parâmetro obrigatório `p`/`Pageable`, enquanto o runtime aceita `page`, `size` e `sort`.
- Esperado: parâmetros OpenAPI explícitos e opcionais, alinhados ao runtime.
- Impacto: clientes gerados podem enviar formato incorreto; o frontend segue `docs/API.md`.
