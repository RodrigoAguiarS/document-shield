# Arquitetura frontend

Angular 20 com componentes standalone, rotas lazy e HttpClient. `core/` contém configuração, contratos e serviços tipados; `shared/` contém componentes sem regra de negócio; `layout/` contém o shell; `features/` contém dashboard, documentos, análises, ocorrências e regras.

A API usa base `/api/v1`; no desenvolvimento, `proxy.conf.json` encaminha `/api` para `127.0.0.1:8080`. Paginação, ordenação e filtros são executados pelo backend. Não há estado global artificial: cada página mantém apenas seu estado de visualização e recarrega fontes oficiais.

O polling agenda a próxima consulta somente depois que a atual termina, para impedir sobreposição. Ele encerra em `CONCLUIDA`/`FALHOU`, no destroy do componente ou após quatro falhas consecutivas; então oferece retomada manual.

O upload usa `multipart/form-data` e eventos de progresso HTTP. O limite de 20 MB e intervalo de polling ficam em `APP_CONFIG`. Respostas RFC 9457 são convertidas em `ApiProblem`, sem autenticação/JWT.
