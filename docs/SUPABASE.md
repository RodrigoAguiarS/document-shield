# Supabase PostgreSQL

O Document Shield usa o Supabase somente como PostgreSQL gerenciado. O frontend Angular não acessa o banco diretamente e não recebe a senha do banco nem uma secret key do Supabase.

## Conexão direta

O perfil `supabase` usa o endpoint PostgreSQL direto do projeto:

- Host: `db.ljdaujhcermlqjrtpsme.supabase.co`
- Porta: `5432`
- Banco: `postgres`
- Usuário: `postgres`
- SSL: obrigatório

Esse endpoint resolve para IPv6. Se a hospedagem do backend oferecer apenas IPv4, use o Session Pooler exibido em **Supabase → Connect** e substitua `SUPABASE_DB_HOST` e `SUPABASE_DB_USER` pelos valores fornecidos no painel.

## Variáveis de ambiente

Defina as variáveis no ambiente seguro da hospedagem. Nunca grave a senha em arquivos versionados.

```text
SPRING_PROFILES_ACTIVE=supabase
SUPABASE_DB_PASSWORD=<senha do banco>
SUPABASE_DB_HOST=db.ljdaujhcermlqjrtpsme.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_POOL_SIZE=5
```

Somente `SPRING_PROFILES_ACTIVE` e `SUPABASE_DB_PASSWORD` são obrigatórias enquanto os valores padrão do projeto permanecerem válidos.

## Execução local

No PowerShell, forneça a senha apenas para o processo atual:

```powershell
$env:SPRING_PROFILES_ACTIVE = "supabase"
$env:SUPABASE_DB_PASSWORD = Read-Host "Senha do banco"
.\mvnw.cmd spring-boot:run
```

Remova a variável da sessão quando terminar:

```powershell
Remove-Item Env:SUPABASE_DB_PASSWORD
```

## Chaves da API

As variáveis `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY` e `SUPABASE_JWKS_URL` não são necessárias para a conexão JDBC. Uma secret key não substitui a senha do PostgreSQL.

Se o Supabase Auth for integrado futuramente, a publishable key poderá ser usada por um cliente público protegido por RLS, enquanto qualquer secret key deverá permanecer exclusivamente em infraestrutura de backend.
