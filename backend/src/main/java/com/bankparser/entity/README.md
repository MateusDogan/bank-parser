# Entities (JPA)

**Fase 2 do DEVELOPMENT_PLAN.md — concluída.**

- `BaseEntity.java` — mapped superclass com `id` (UUID gerado na aplicação), `createdAt`/`updatedAt`
  (preenchidos pelo auditing do Spring Data, ver `config/JpaAuditingConfig`) e a flag `deleted`
- `Organization.java` — tenant. `Organization.DEFAULT_ID` é a organização criada pela migration `V2`
- `User.java` — funcionários. Sem credenciais: a autenticação (Fase 7) adiciona os campos sobre esta entidade
- `Client.java` — CNPJ/CPF atendido. `document` é normalizado para só dígitos, porque o mesmo CNPJ
  chega com máscaras diferentes conforme a origem (digitado à mão vs. lido do cabeçalho do PDF)
- `Statement.java` — PDF enviado. `storageKey` fica nulo até a Fase 3 (MinIO)
- `Transaction.java` — linha extraída. `lineNumber` preserva a ordem do PDF, que a data sozinha não
  reconstrói (várias transações compartilham o mesmo dia)

## Multi-tenant

`organizationId` é uma coluna direta em `User`, `Client`, `Statement` e `Transaction` — inclusive onde
daria para chegar na Organization via join. Isso mantém o filtro de tenant como um predicado indexado
simples em qualquer query, sem join em cadeia.

## Soft-delete

Cada entidade declara `@SQLDelete` (transforma o delete em `UPDATE ... SET deleted = true`) e
`@SQLRestriction("deleted = false")` (esconde a linha de todas as queries, inclusive em joins).
O histórico precisa sobreviver para auditoria contábil, então nada é removido fisicamente.

Consequência: para ler linhas removidas — recuperação ou auditoria — é preciso query nativa. Não há
caso de uso para isso ainda; quando houver, um método `@Query(nativeQuery = true)` no repository resolve.

O índice único de `clients` é parcial (`WHERE deleted = false`), permitindo recadastrar um CNPJ cujo
cliente anterior foi removido.
