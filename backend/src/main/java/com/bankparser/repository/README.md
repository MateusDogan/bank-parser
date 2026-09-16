# Repositories

**Fase 2 do DEVELOPMENT_PLAN.md — concluída.**

`OrganizationRepository`, `UserRepository`, `ClientRepository`, `StatementRepository`,
`TransactionRepository`.

**Regra**: todo método de busca aceita `organizationId` (ex.: `findByOrganizationIdAndId(...)`),
mesmo com uma única Organization hoje. `OrganizationRepository` é a exceção — ele *é* o tenant.

Não é preciso filtrar `deleted` nos métodos: o `@SQLRestriction` nas entidades já exclui as linhas
removidas de toda query gerada.

Os métodos existentes cobrem o que as Fases 3 e 4 precisam. Filtros mais ricos (por data, valor,
categoria) entram na Fase 4.
