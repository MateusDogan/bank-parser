# Repositories

`StatementRepository` e `TransactionRepository`.

Não é preciso filtrar `deleted` nos métodos: o `@SQLRestriction` nas entidades já exclui as linhas
removidas de toda query gerada.

Filtros mais ricos (por data, valor, categoria) entram quando houver volume que justifique.
