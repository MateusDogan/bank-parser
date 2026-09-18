# Bank Parser — Plano de Desenvolvimento

> Fases curtas, de propósito. Cada uma entrega algo que dá para usar e julgar
> antes da próxima começar. Se uma fase não cabe em poucos dias, ela está mal
> recortada.

**Objetivo**: enviar um extrato bancário em PDF, guardá-lo, e baixar as
transações em CSV.

**Stack**: Java 17 + Spring Boot 3 | React | PostgreSQL 15 | MinIO | Docker

As decisões de arquitetura que não mudam sem discussão estão no `CLAUDE.md`.

---

## A simplificação de 2026-09-18

O projeto nasceu multi-tenant: `Organization → User → Client → Statement →
Transaction`, com `organization_id` em toda tabela e conferência de CNPJ no
upload. Nada disso tinha uso — havia uma Organization fixa, nenhum User, e o
cadastro de Client era um passo a mais antes de conseguir converter um PDF.

**Removido**: `Organization`, `User`, `Client`, `CurrentOrganizationProvider`,
`ClientController`, `DocumentMismatchException`, `DuplicateClientException`,
`organization_id` de todas as tabelas, e as migrations V2–V4 (o schema virou uma
V1 nova, com `statements` e `transactions` apenas).

**Sobrou**: upload → parse → PDF no storage → transações no banco → CSV.

Se o sistema virar SaaS, multi-tenant volta como migration. Foi removido por ser
estrutura sem uso, não por ser má ideia.

---

## Mapa

| Fase | Nome | Tamanho | Depende de |
|------|------|---------|------------|
| **0** | **MVP — o que já existe** | — | ✅ concluída |
| 1 | Primeiro uso real | 1–2 dias | Docker na máquina |
| 2 | Desfazer e recuperar | 1–2 dias | 1 |
| 3 | Reenvio e duplicata | 2 dias | 2 |
| 4 | Exportação para a contabilidade | 2 dias | feedback da 1 |
| 5 | Conferência do extrato | 2–3 dias | 1 |
| 6 | Segundo banco | 3–4 dias | — |
| 7 | Operação | 2 dias | 1 |

Depois disso, só o que o uso real pedir. Autenticação e multi-tenant não estão
no mapa: entram se o sistema sair da rede interna ou virar produto.

---

## Fase 0 — MVP (concluída)

Upload de PDF Stone → extração → CSV, com o PDF guardado e rastreabilidade de
parser.

**Entregue**:
- **Parser** `StoneParser` por coordenadas (PDFBox), validado 264/264 contra a
  saída do parser Python original. `BankStatementParser` é interface; o segundo
  banco não mexe no resto do sistema.
- **Dados**: Postgres + Flyway (V1), modelo `Statement → Transaction`,
  soft-delete em tudo (histórico contábil não some).
- **Storage**: `StorageService` com implementação MinIO — trocar por S3/R2 não
  toca em código de negócio.
- **API**: `POST /api/statements/upload`, `GET /api/statements`,
  `GET /api/statements/{id}`, `GET /api/statements/{id}/export`.
- **Frontend**: arrastar PDF → **Iniciar** → lista de extratos → baixar CSV.
- **Rastreabilidade**: `parser_version` por extrato, `TransactionType` enum,
  `BalanceValidationService` sinalizando divergência de saldo sem bloquear.
- **Testes**: 32 (2 pulados por dependerem do extrato real). Postgres e S3
  embarcados — a suíte roda sem Docker.
- **CI**: testes a cada push; frontend publicado no GitHub Pages.

**O que ainda não foi verificado** — é o que a Fase 1 existe para resolver:
- `docker compose up` nunca rodou (não há Docker nesta máquina)
- Nenhum PDF real passou pela API de ponta a ponta — só pelo parser isolado
- A imagem do backend nunca foi construída
- As constantes do `RealStatementRegressionTest` (264 transações, 11
  divergências) vieram da análise da era Python e não foram confirmadas aqui

---

## Fase 1 — Primeiro uso real

**Objetivo**: alguém do escritório processa um extrato de verdade, de ponta a
ponta, e o CSV serve.

**Entrega**:
- `docker compose up -d` validado (Postgres + MinIO), migrations aplicadas
- Imagem do backend construída: `docker compose --profile full up -d`
- Um PDF real: arrasta → envia → baixa CSV
- `mvn test -Dbankparser.it.pdf=...` rodado; constantes da baseline confirmadas
  ou corrigidas
- `SETUP_MVP.md` ajustado com o que der errado no caminho

**Pronto quando**: o CSV gerado pela API bate com o que o escritório já usa.

**Feedback a colher**: o formato do CSV atende? O que incomoda no fluxo? Quanto
tempo economiza de fato? *Tudo que vier daqui pode reordenar as fases seguintes
— é o ponto de correção de rota mais barato do projeto.*

---

## Fase 2 — Desfazer e recuperar

**Objetivo**: errar não exigir mexer no banco.

**Entrega**:
- `GET /api/statements/{id}/pdf` — baixar o original. `StorageService.download()`
  já existe e hoje não tem chamador
- `DELETE /api/statements/{id}` — soft-delete, o extrato some da lista mas fica
  no histórico
- Botão de apagar na lista de extratos

**Pronto quando**: dá para apagar um envio errado e reenviar sem SQL na mão.

**Feedback**: apagar deve ser reversível pela interface, ou some e pronto?

---

## Fase 3 — Reenvio e duplicata

**Objetivo**: o sistema perceber que aquele extrato já entrou.

**Entrega**:
- Hash do PDF gravado no `Statement` (migration + coluna)
- Upload de arquivo idêntico responde `409` com link para o existente, e um
  parâmetro explícito (`?force=true`) para enviar assim mesmo
- Aviso quando já existe extrato do mesmo período, mesmo com arquivo diferente

**Pronto quando**: enviar o mesmo arquivo duas vezes por engano não cria dois
extratos em silêncio.

**Feedback**: bloquear ou só avisar? Reenviar o mesmo período é erro ou rotina
(extrato parcial atualizado, por exemplo)?

---

## Fase 4 — Exportação para a contabilidade

**Objetivo**: exportar no formato que o sistema contábil do escritório aceita.

**Entrega**:
- Excel (`.xlsx`) além do CSV — o Apache POI foi removido do `pom.xml` quando
  virou peso morto e volta aqui, com uso de verdade
- Layout de colunas definido pelo que a Fase 1 revelar
- Se houver um formato de importação específico do sistema contábil, ele entra
  aqui

**Pronto quando**: o arquivo importa no sistema contábil sem edição manual.

---

## Fase 5 — Conferência do extrato

**Objetivo**: transformar `validation_flags` em trabalho de conferência de
verdade, em vez de um campo que ninguém olha.

**Entrega**:
- As divergências de saldo aparecem na tela, na linha certa
- Marcar um extrato como conferido, com data
- Corrigir uma transação manualmente, com registro de que foi editada — o valor
  original nunca é sobrescrito sem trilha

**Nota**: sem autenticação, "conferido" não tem *quem* — só *quando*. Para uso
interno isso basta; se não bastar, é sinal de que login virou necessidade.

**Pronto quando**: dá para saber, olhando a lista, quais extratos precisam de
atenção humana.

**Feedback**: as divergências que o sistema aponta são as que importam, ou é
ruído?

---

## Fase 6 — Segundo banco

**Objetivo**: provar que trocar/adicionar parser não mexe no resto.

**Entrega**:
- `ItauParser` (ou o banco que aparecer primeiro) implementando
  `BankStatementParser`, mapeando o vocabulário dele ("Crédito"/"Débito") para
  o `TransactionType` que já existe
- `parserVersion()` próprio, baseline de regressão própria
- Seleção de banco no upload — o parâmetro `bankKey` já está no endpoint

**Pronto quando**: o novo banco funciona sem nenhuma alteração em controller,
serviço, schema ou contrato de API. *Se algo fora do pacote `parser` precisar
mudar, a arquitetura falhou e vale parar para entender por quê.*

---

## Fase 7 — Operação

**Objetivo**: o sistema sobreviver a uma semana sem ninguém olhando.

**Entrega**:
- Backup automático de Postgres e MinIO, com restauração testada de verdade
- Healthcheck (`/actuator/health`) e reinício automático no Compose
- Retenção e rotação de log
- Procedimento de atualização sem perder dados

**Pronto quando**: restaurar do backup num ambiente limpo recupera tudo.

---

## Como usar este documento

- Uma fase por vez. Terminou, colhe o feedback, e só então decide a próxima —
  a ordem daqui para frente é uma proposta, não um contrato.
- Fase que não cabe em poucos dias está mal recortada: quebre.
- Ao concluir, marque aqui o que ficou **verificado** e o que ficou **suposto**.
  A Fase 0 é o exemplo: o código está pronto, mas quatro coisas nunca rodaram de
  verdade, e isso está escrito lá em vez de ficar implícito.
- Antes de adicionar estrutura (uma entidade, uma camada, uma abstração),
  pergunte se algo hoje a usa. A simplificação de 2026-09-18 existe porque essa
  pergunta não foi feita antes.

---
*Atualizado: 2026-09-18*
