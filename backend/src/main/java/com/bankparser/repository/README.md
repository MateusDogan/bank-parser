# Repositories

**Fase 2 do DEVELOPMENT_PLAN.md** — implementar aqui.

`JpaRepository` para cada entidade em `entity/`: `OrganizationRepository`, `ClientRepository`, `StatementRepository`, `TransactionRepository`.

Métodos de busca devem sempre aceitar `organizationId` como filtro (ex.: `findByOrganizationIdAndClientId(...)`), mesmo com uma única Organization hoje.
