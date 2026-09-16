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
# Subir infraestrutura
docker-compose up -d

# Backend
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

Acesse:
- Frontend: http://localhost:3000
- API: http://localhost:8080
- MinIO Console: http://localhost:9001
- pgAdmin: http://localhost:5050

## Documentação

- [`DEVELOPMENT_PLAN.md`](./DEVELOPMENT_PLAN.md) — plano de desenvolvimento fase a fase
- [`CLAUDE.md`](./CLAUDE.md) — guia técnico do projeto (setup, arquitetura, convenções)

## Parser Changeability (Risco Conhecido)

**Descoberta**: O parser foi testado contra um extrato real de 264 linhas. Uma análise de invariantes (saldo continuity check) revelou que **11 de 264 linhas (4%)** têm atribuição errada — o campo `tarifa` aparece na linha de boleto, não na sua tarifa associada. Isso é um **defeito real que passou silenciosamente** nos testes sintéticos (que usam layout perfeito).

**Por que importa**: Se o parser for alterado para "corrigir" essa atribuição, como saber se é realmente melhoria ou se quebrou algo? Sem rastreabilidade e golden files, a resposta é "deployou, esperou um dia, percebeu erro em produção, rollback".

**Garantias que virão (Fase 0.5)**:
- `parser_version` em cada Statement: rastreia qual versão do parser processou aquele PDF
- `stone-real-264-expected.json`: saída esperada do parser para aquele extrato real
- `BalanceValidationService`: marca Statements com defeitos potenciais pra revisão manual (não bloqueia)
- CI/CD com golden file diff: toda alteração no parser mostra exatamente quais linhas mudaram

**Procedimento seguro para trocar parser** (quando Fase 0.5 estiver pronta):
1. Alterar `StoneParser.parserVersion()` e lógica
2. Rodar `mvn test` — o teste de regressão batará saída atual vs `stone-real-264-expected.json`
3. Revisar o diff: melhoria ou regressão?
4. Se melhoria: atualizar o JSON, fazer merge
5. Rodar `SELECT COUNT(*) FROM statements WHERE parser_version = '1.0'` pra saber quantos Statements antigos são afetados
6. Deploy monitorado: se houver surpresa, rollback e investiga com dados em mãos

## Origem

Este projeto evoluiu de um script Python de conversão PDF→CSV para extratos Stone, sendo reconstruído em Java/Spring Boot para suportar múltiplos clientes e escalar eventualmente para um produto SaaS.
