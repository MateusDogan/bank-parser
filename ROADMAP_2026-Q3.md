# Roadmap Atualizado — Q3 2026

> **⚠️ Documento superado (2026-09-16).** A numeração e a ordem de fases aqui
> não valem mais — a rastreabilidade do parser descrita como futura já foi
> implementada, e o plano foi refeito em fases menores.
> **O plano válido é o `DEVELOPMENT_PLAN.md`.** Este arquivo fica só como
> registro do raciocínio que levou à Fase 0.5.

## Mudança Central: Parser Changeability

O usuário levantou uma questão crítica: **"Como garanto que posso trocar o parser sem quebrar produção?"**

Resposta curta: hoje, não pode. Testes sintéticos passam, mas análise de invariantes (saldo continuity) revelou que **11 de 264 linhas (4%)** do extrato real têm atribuição errada. Isso passou silenciosamente.

## Nova Estrutura: Ordem Fase 4 → 5 → 6 → 0.5

### Próximas Fases (conforme DEVELOPMENT_PLAN.md)

| Fase | Ordem | Duração | O Quê | Impacto em Changeability |
|------|-------|---------|-------|--------------------------|
| 4 | 1º | 2-3d | CRUD API (clientes, transações, relatórios) | `Transaction.type` sai como **enum** (não String) |
| 5 | 2º | 3-5d | Frontend React (upload, dashboard) | Acumula statements em diversos `parser_version`s |
| 6 | 3º | 2-3d | E2E tests + docs + deployment | Tudo funciona end-to-end |
| **0.5** | **4º** | **1-2d** | **CI/CD + rastreabilidade + golden files** | **Truque liberado** |

### Fase 0.5: Parser Changeability (Depois de Tudo)

Quando chegar lá (provável 2026-10-05 ou depois), volta e implementa:

#### 1. `parser_version` em `Statement`

```sql
ALTER TABLE statements ADD COLUMN parser_version VARCHAR(10);
```

- `StoneParser` implementa `parserVersion() -> "1.0"` (semver)
- `StatementProcessingService` popula ao criar Statement
- Depois: query `SELECT COUNT(*) FROM statements WHERE parser_version = '1.0'` responde "quantos afetados por mudança"

#### 2. `TransactionType` Enum

```java
public enum TransactionType {
    ENTRADA("Entrada"),
    SAIDA("Saída");
    
    private final String label;
    // ...
}
```

- `ParsedTransaction.tipo` vira `TransactionType` (não String)
- `Transaction.type` vira enum (Hibernate mapeia pra VARCHAR no DB)
- `StoneParser` converte "Entrada"/"Saída" → enum
- **Motivo**: Próximo banco (Itaú) chama "Crédito"/"Débito"; enum garante contrato estável na API

#### 3. Golden File (Regressão)

```
backend/src/test/resources/parser/stone-real-264-expected.json
```

- Saída esperada de parse do extrato real (264 transações)
- JSON, não PDF (PDFs reais com dados sensíveis não entram no git)
- Teste `StoneParserRegressionTest` roda parser, serializa, compara com golden
- Se mudar: diff aparece, decide-se se é melhoria ou regressão

#### 4. `BalanceValidationService`

```java
public record ValidationReport(List<BalanceBreak> breaks) {}

public BalanceValidationService.checkBalanceContinuity(
    List<Transaction> txns) -> ValidationReport
```

- Marca Statements com saldo quebrado pra revisão manual
- **Não bloqueia** (4% das linhas do real quebram — bloquear = não funciona)
- `StatementProcessingService` chama, salva report em coluna `validation_flags`

#### 5. GitHub Actions

```
.github/workflows/
  test.yml       → mvn test, fail se cobertura < 75%
  lint.yml       → mvn spotbugs:check
  build.yml      → mvn clean package
```

- Ramo `main` rejeita merges se falhar
- Gratuito até 2000 min/mês (o suficiente)
- Nativo ao repositório GitHub

#### 6. README: Seção "Parser Changeability"

Documentar:
- O risco (11 linhas quebram, 4%)
- Como rodar contra PDF real localmente (`-Dbankparser.it.pdf=...`)
- Procedimento seguro pra trocar parser

---

## Por Quê Essa Ordem?

**Fase 4/5/6 primeiro** garante MVP funcional. Se esperar por Fase 0.5, o escritório não começa a usar.

**Fase 0.5 depois** adiciona as garantias de forma não-intrusiva:
- `Transaction.type` enum sai certo desde Fase 4 (uma linha alterada em um arquivo)
- `Statement.parserVersion` fica NULL pra Statements de Fase 3, preenchido em novos
- Nenhuma mudança quebradora na API ou DB (coluna adicionada, não alterada)
- Golden file aparece quando entra em produção (dia 0), não antes

**Timings realistas**:
- Fase 4: 2-3 dias (CRUD é standard)
- Fase 5: 3-5 dias (React, Recharts, upload)
- Fase 6: 2-3 dias (docs, E2E, deployment)
- **Subtotal: 7-11 dias, MVP pronto**
- Fase 0.5: 1-2 dias (mais um refinamento que construção)

**Impacto zero se abrir mão de 0.5**: Fase 4/5/6 funcionam autonomamente. Se nunca houver Fase 0.5, é MVP ok, sem rastreabilidade (risco aceitável para v1 interna).

---

## Checklist Imediato (Antes de Começar Fase 4)

- [ ] Revisar DEVELOPMENT_PLAN.md nova ordem (Fase 4 → 0.5)
- [ ] Revisar README nova seção "Parser Changeability"
- [ ] Decidir: quais clientes/transações aparecer em Fase 4? (CNPJ real, CPF test, mixado?)
- [ ] Qualquer simplificação óbvia que viu em Fase 0-3? (Agora é hora)

---

*Atualizado: 2026-09-16*
*Plano: C:\Users\User\.claude\plans\toasty-pondering-galaxy.md*
