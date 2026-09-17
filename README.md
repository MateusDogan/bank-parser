# Bank Parser

Plataforma para conversão de extratos bancários (PDF) em dados estruturados, com suporte a múltiplos clientes por escritório de contabilidade.

## Status

🚧 Em desenvolvimento — ver [`DEVELOPMENT_PLAN.md`](./DEVELOPMENT_PLAN.md) para o plano completo e progresso atual.

## Stack

- **Backend**: Java 17 + Spring Boot 3
- **Frontend**: React 18
- **Banco de Dados**: PostgreSQL 15
- **Storage**: MinIO (S3-compatible)
- **Extração de PDF**: Apache PDFBox

## Início Rápido

```bash
# Subir infraestrutura (Postgres + MinIO)
cp .env.example .env
docker compose up -d

# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

Acesse:
- Frontend: http://localhost:5173
- API: http://localhost:8080
- MinIO Console: http://localhost:9001
- pgAdmin: http://localhost:5050 (só com `docker compose --profile tools up -d`)

Passo a passo com troubleshooting em [`SETUP_MVP.md`](./SETUP_MVP.md).

## Documentação

- [`DEVELOPMENT_PLAN.md`](./DEVELOPMENT_PLAN.md) — plano de desenvolvimento fase a fase
- [`CLAUDE.md`](./CLAUDE.md) — guia técnico do projeto (setup, arquitetura, convenções)

## Parser Changeability (Risco Conhecido)

**Descoberta**: O parser foi testado contra um extrato real de 264 linhas. Uma análise de invariantes (saldo continuity check) revelou que **11 de 264 linhas (4%)** têm atribuição errada — o campo `tarifa` aparece na linha de boleto, não na sua tarifa associada. Isso é um **defeito real que passou silenciosamente** nos testes sintéticos (que usam layout perfeito).

**Por que importa**: Se o parser for alterado para "corrigir" essa atribuição, como saber se é realmente melhoria ou se quebrou algo? Sem rastreabilidade e golden files, a resposta é "deployou, esperou um dia, percebeu erro em produção, rollback".

**Garantias já implementadas**:
- `parser_version` em cada Statement: rastreia qual versão do parser processou aquele PDF
- `BalanceValidationService`: grava em `validation_flags` quais linhas quebram a continuidade de saldo. Sinaliza, **não bloqueia** — 4% das linhas de um extrato real legítimo quebram esse invariante
- `TransactionType` enum: o próximo banco fala "Crédito"/"Débito" e mapeia para o mesmo vocabulário, sem mudar o contrato da API
- `RealStatementRegressionTest`: fixa a baseline conhecida (264 transações, 11 divergências) e falha se qualquer um dos dois mudar

O golden file em JSON foi **descartado de propósito**: ele compararia a saída
contra um PDF sintético, de layout perfeito, que por construção não reproduz o
defeito das 11 linhas. A regressão que vale roda contra o extrato real, que nunca
é versionado — daí o teste ser pulado quando o arquivo não está na máquina.

**Procedimento para trocar o parser**:
1. Rodar `mvn test "-Dbankparser.it.pdf=<extrato real>"` **antes** de mexer, para confirmar a baseline
2. Alterar a lógica e subir `StoneParser.parserVersion()`
3. Rodar de novo: `RealStatementRegressionTest` falha e diz quais linhas divergem agora
4. Revisar o diff — melhoria ou regressão?
5. Se melhoria: atualizar a constante no teste, deliberadamente, no mesmo commit
6. `SELECT COUNT(*) FROM statements WHERE parser_version = '1.0'` diz quantos extratos já processados foram afetados
7. Deploy monitorado: se houver surpresa, rollback com os dados em mãos
