# Controllers

**Fase 3 concluída**; `ClientController` / `TransactionController` / `ReportController` entram na Fase 4.

`StatementController.java` — `/api/statements`:

| Endpoint | Retorno |
|---|---|
| `POST /upload` (multipart: `file`, `clientId`, `bankKey` opcional = `stone`) | `201` + `StatementResponse` |
| `GET /{id}` | `StatementResponse` |
| `GET /{id}/export?format=csv` | `text/csv` como anexo |

Sem autenticação no MVP: o `organizationId` vem do `CurrentOrganizationProvider`, que na Fase 7 passa
a ler do usuário autenticado sem que os controllers mudem.

Erros são padronizados em `exception/GlobalExceptionHandler` — nenhum controller trata exceção
diretamente. Os status usados: **422** (PDF ilegível — a requisição está correta, o conteúdo não),
**409** (CNPJ do extrato diferente do cliente), **404**, **413** (upload acima do limite) e **503**
(storage fora do ar).
