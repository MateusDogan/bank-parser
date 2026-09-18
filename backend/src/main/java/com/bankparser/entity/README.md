# Entities (JPA)

- `BaseEntity.java` — mapped superclass com `id` (UUID gerado na aplicação), `createdAt`/`updatedAt`
  (preenchidos pelo auditing do Spring Data, ver `config/JpaAuditingConfig`) e a flag `deleted`
- `Statement.java` — PDF enviado, com os metadados lidos do cabeçalho e a chave do arquivo no storage
- `Transaction.java` — linha extraída. `lineNumber` preserva a ordem do PDF, que a data sozinha não
  reconstrói (várias transações compartilham o mesmo dia)

## Soft-delete

Cada entidade declara `@SQLDelete` (transforma o delete em `UPDATE ... SET deleted = true`) e
`@SQLRestriction("deleted = false")` (esconde a linha de todas as queries, inclusive em joins).
O histórico precisa sobreviver para auditoria contábil, então nada é removido fisicamente.

Consequência: para ler linhas removidas — recuperação ou auditoria — é preciso query nativa. Não há
caso de uso para isso ainda; quando houver, um método `@Query(nativeQuery = true)` no repository resolve.
