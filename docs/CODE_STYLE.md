# Code Style do backend

Este documento é obrigatório para pessoas e agentes de IA que alterem o backend do Document Shield. A prioridade é manter código legível, previsível e seguro sem adicionar abstrações que não resolvam um problema real.

## 1. Formatação Java

- Use quatro espaços, nunca tabulações.
- Mantenha as linhas em até 120 caracteres sempre que isso não prejudicar a leitura.
- Escreva uma instrução por linha e use chaves em todas as estruturas de controle.
- Separe campos, construtores e métodos com linhas em branco.
- Não use imports curingas. Imports não utilizados devem ser removidos.
- O formato canônico é aplicado pelo Palantir Java Format por meio do Spotless.

Comandos, executados em `backend/`:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
```

Em Linux ou macOS, substitua `mvnw.cmd` por `./mvnw`. O goal `spotless:check` também é executado na fase Maven `verify`.

## 2. Nomenclatura

- Classes, records e enums: `PascalCase`.
- Métodos, parâmetros e variáveis: `camelCase`.
- Constantes: `UPPER_SNAKE_CASE`.
- Pacotes: minúsculos, sem abreviações artificiais.
- Preserve o idioma predominante do conceito. Contratos públicos existentes em português não devem ser renomeados.
- Prefira nomes que expressem intenção, como `integrationId`, `activePolicy` e `riskScore`.

## 3. Organização de pacotes

- `controller`: adaptação HTTP, validação de entrada e DTOs.
- `service`: regras de negócio, orquestração e limites transacionais.
- `repository`: persistência e consultas, sem decisões de negócio.
- `domain.entity`: estado persistido, sem consultas ou orquestração.
- `dto.request` e `dto.response`: contratos externos imutáveis, preferencialmente records.
- `detection`: extração e análise, independente de HTTP.
- `policy`: avaliação determinística, independente de HTTP e JPA.
- `security`: autenticação, autorização, filtros e rate limiting.
- `config`: composição e configuração da aplicação.

Não crie uma interface para uma implementação única sem uma fronteira real de substituição.

## 4. Responsabilidades das camadas

Controllers recebem dados, acionam validação declarativa, delegam para services e retornam DTOs. Não retornam entidades JPA e não implementam regras de negócio.

Services coordenam casos de uso e definem transações curtas. Operações de leitura usam `@Transactional(readOnly = true)` quando dependem de consistência transacional. Métodos privados extraem passos relevantes, não fragmentos triviais.

Repositories expressam acesso a dados. Queries não devem decidir políticas, permissões ou respostas HTTP.

Converters e mappers transformam representações. Não consultam banco, não abrem transações e não alteram regras de negócio.

## 5. Entidades

- Entidades usam UUID e referências entre agregados também são UUIDs.
- São proibidas associações JPA de objeto: `@OneToMany`, `@ManyToOne`, `@OneToOne` e `@ManyToMany`.
- Integridade referencial pertence às constraints de `schema.sql`.
- Não use `@Data`; exponha somente os getters, setters, builders e construtores necessários.
- Inicialização automática deve ficar em callbacks pequenos e determinísticos.
- Não implemente cascatas JPA entre agregados.

## 6. DTOs

- Prefira records para contratos imutáveis.
- Use Jakarta Validation nos campos de entrada.
- Preserve nomes JSON e compatibilidade dos contratos publicados.
- DTOs não carregam entidades nem executam lógica de persistência.
- Campos ausentes ou desconhecidos não devem receber valores inventados. Isso é especialmente importante para coordenadas e propriedades visuais extraídas de PDFs.

## 7. Services

- Use injeção por construtor e dependências `final`.
- Mantenha métodos públicos focados em um caso de uso.
- Prefira retornos explícitos, `Optional` nas fronteiras de consulta e exceções de domínio a retornos `null` ambíguos.
- Evite encadeamentos longos quando variáveis intermediárias revelarem a intenção.
- Não capture `Exception` genericamente sem uma estratégia concreta de recuperação.
- Detection Engine e Policy Engine não dependem de controllers, servlet ou entidades JPA.

## 8. Exceções

- Use exceções específicas para ausência, conflito, capacidade, autenticação e indisponibilidade.
- Converta exceções em `ProblemDetail` somente na camada HTTP.
- Não exponha stack traces, caminhos físicos, conteúdo de PDF ou detalhes internos.
- Não silencie falhas. Quando uma falha for deliberadamente isolada, registre contexto operacional seguro.

## 9. Logging

- Use SLF4J com placeholders: `log.warn("Falha ao processar análise {}", analysisId, exception)`.
- Nunca concatene valores em mensagens de log.
- Nunca registre tokens, hashes de documento, conteúdo integral, caminhos físicos ou evidências extraídas.
- Use `debug` para diagnóstico detalhado, `info` para eventos de ciclo de vida, `warn` para degradação recuperável e `error` para falhas não recuperáveis.

## 10. Testes automatizados

- Nomeie testes pelo comportamento observado.
- Use Arrange–Act–Assert sem comentários redundantes.
- Cubra limites, falhas seguras e compatibilidade de contratos.
- Testes unitários não devem subir Spring quando a classe pode ser instanciada diretamente.
- Testes de integração validam segurança, transações, SQL e contratos HTTP.
- Não reduza assertions apenas para fazer uma regressão passar.
- Antes de concluir, execute `./mvnw clean verify` dentro de `backend/`.

## 11. Exemplos

Correto:

```java
if (policy.getStatus() != PolicyStatus.ATIVA) {
    throw new PolicyUnavailableException("A política não está ativa.");
}

PolicyEvaluationResult evaluation = policyEngine.evaluate(policySnapshot, riskScore);
return policyMapper.toResponse(evaluation);
```

Incorreto:

```java
if (p.getStatus() != PolicyStatus.ATIVA) throw new RuntimeException("erro");
return mapper.toResponse(engine.evaluate(snapshot(p), score));
```

Correto:

```java
log.warn("Falha concorrente ao atualizar agregado da integração {}", integrationId);
```

Incorreto:

```java
log.warn("Falha para token " + token + " no arquivo " + physicalPath);
```

O exemplo incorreto expõe segredos, usa concatenação e registra caminho físico. Esses dados nunca devem chegar aos logs.
