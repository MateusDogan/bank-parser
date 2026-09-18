# Controllers

`StatementController.java` — `/api/statements`:

| Endpoint | Retorno |
|---|---|
| `POST /upload` (multipart: `file`, `bankKey` opcional = `stone`) | `201` + `StatementResponse` |
| `GET` | lista de `StatementResponse`, mais recente primeiro |
| `GET /{id}` | `StatementResponse` |
| `GET /{id}/export?format=csv` | `text/csv` como anexo |

Erros são padronizados em `exception/GlobalExceptionHandler` — nenhum controller trata exceção
diretamente. Os status usados: **422** (PDF ilegível — a requisição está correta, o conteúdo não),
**404**, **413** (upload acima do limite) e **503** (storage fora do ar).
