# Implementação do backend

O projeto usa Spring Boot 3.5.16, Java 21, PDFBox 3.0.8, H2, JPA, Validation, springdoc, JUnit 5, Mockito e AssertJ. `schema.sql` é idempotente e cria tabelas, índices e chaves estrangeiras.

Consistência de upload: bytes vão para `.part`, o hash é calculado durante a cópia, o PDF é aberto pelo PDFBox e só então ocorre o move para o nome UUID final. Se o registro inicial falhar, o arquivo é removido; após registro, falhas ficam auditáveis no documento/análise. Na exclusão, o banco confirma primeiro e o arquivo é removido após o commit; uma falha de filesystem pode deixar um órfão inofensivo para limpeza operacional, nunca uma referência pública quebrada por rollback.

Limites padrão: 20 MB, 500 páginas, 5 milhões de caracteres, 2 workers, fila 25 e máximo de 50 achados por regra visual. Nomes fornecidos pelo cliente nunca determinam o caminho físico.

O OpenAPI é exportado por teste de integração a partir de `/v3/api-docs`, garantindo que os paths reflitam os controllers efetivos.
