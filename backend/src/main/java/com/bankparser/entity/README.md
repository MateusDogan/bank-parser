# Entities (JPA)

**Fase 2 do DEVELOPMENT_PLAN.md** — implementar aqui.

Modelo multi-tenant:
- `Organization.java` — tenant (hoje: 1 registro; futuro: múltiplos escritórios)
- `User.java` — funcionários (preparado para auth na Fase 7)
- `Client.java` — CNPJ atendido pelo escritório, pertence a uma Organization
- `Statement.java` — PDF enviado, pertence a um Client
- `Transaction.java` — linha extraída, pertence a um Statement

**Regra crítica**: toda entidade abaixo de Organization deve manter a referência (direta ou via Client) para permitir filtro por `organization_id` em todas as queries.
