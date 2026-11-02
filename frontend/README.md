# Document Shield Frontend

Angular 20 standalone + PrimeNG 20. Requer Node 20.19+, 22.12+ ou 24+ e o backend em `127.0.0.1:8080`.

```powershell
cd frontend
npm install
npm start
```

Acesse `http://localhost:4200`. O proxy encaminha `/api/v1` para o backend, sem CORS aberto.

O painel de credenciais fica em `http://localhost:4200/integracoes/api-keys`. O acesso redireciona para `/login` e exige a conta administrativa configurada no backend. O token criado ou rotacionado aparece uma única vez e não é persistido pelo frontend.

Comandos:

- `npm run build` — build de produção.
- `npm test -- --watch=false --browsers=ChromeHeadless` — testes.

Configurações de API, upload e polling ficam em `src/app/core/config/app-config.ts`. A aplicação aceita apenas PDF, não calcula riscos e não simula respostas de produção. Limitações contratuais estão em `docs/frontend/BACKEND_REQUESTS.md`.
