# Bank Parser — Plano de Desenvolvimento

> Fases curtas, de propósito. Cada uma entrega algo que dá para usar e julgar
> antes da próxima começar. Se uma fase não cabe em poucos dias, ela está mal
> recortada.

**Objetivo**: converter extratos bancários em PDF para dados estruturados que o
escritório consome, começando como ferramenta interna e podendo virar SaaS.

**Stack**: Java 17 + Spring Boot 3 | React | PostgreSQL 15 | MinIO | Docker

As decisões de arquitetura que não mudam sem discussão estão no `CLAUDE.md`.

---

## Mapa

| Fase | Nome | Tamanho | Depende de |
|------|------|---------|------------|
| **0** | **MVP — o que já existe** | — | ✅ concluída |
| 1 | Primeiro uso real | 1–2 dias | Docker na máquina |
| 2 | Desfazer e recuperar | 2 dias | 1 |
| 3 | Reenvio e duplicata | 2 dias | 2 |
| 4 | Autenticação | 3–4 dias | — |
| 5 | Frontend novo | a definir | modelo de tela |
| 6 | Conferência do extrato | 2–3 dias | 5 |
| 7 | Exportação para a contabilidade | 2 dias | feedback da 1 |
| 8 | Segundo banco | 3–4 dias | — |
| 9 | Relatórios | 3 dias | 5 |
| 10 | Operação | 2 dias | 1 |
| 11 | SaaS | — | tudo acima |

**Independentes do frontend** (dá para tocar enquanto o modelo de tela não
chega): 1, 2, 3, 4, 7, 8, 10.

O frontend React atual é **andaime**: existe para exercitar a API enquanto o
modelo novo não chega. Não vale investir nele além do mínimo — será substituído
na Fase 5.

---

## Fase 0 — MVP (concluída)

Upload de PDF Stone → extração → CSV, com isolamento multi-tenant e
rastreabilidade de parser. É o ponto de partida de tudo que vem depois.

**Entregue**:
- **Parser** `StoneParser` por coordenadas (PDFBox), validado 264/264 contra a
  saída do parser Python original. `BankStatementParser` é interface; o segundo
  banco não mexe no resto do sistema.
- **Dados**: Postgres + Flyway (V1–V4), modelo `Organization → Client →
  Statement → Transaction`, `organization_id` em toda tabela, soft-delete em
  tudo (histórico contábil não some).
- **Storage**: `StorageService` com implementação MinIO — trocar por S3/R2 não
  toca em código de negócio.
- **API**: `POST/GET /api/clients`, `POST /api/statements/upload`,
  `GET /api/statements`, `GET /api/statements/{id}`,
  `GET /api/statements/{id}/export`.
- **Rastreabilidade**: `parser_version` por extrato, `TransactionType` enum,
  `BalanceValidationService` sinalizando divergência de saldo sem bloquear.
- **Testes**: 43 (2 pulados por dependerem do extrato real). Postgres e S3
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
- Um PDF real: cadastra cliente → envia → baixa CSV
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
- `DELETE /api/clients/{id}` — só quando não há extrato ativo, senão 409

**Pronto quando**: dá para apagar um envio errado e reenviar sem SQL na mão.

**Feedback**: apagar deve ser reversível pela interface, ou some e pronto?

---

## Fase 3 — Reenvio e duplicata

**Objetivo**: o sistema perceber que aquele extrato já entrou.

**Entrega**:
- Hash do PDF gravado no `Statement` (migration + coluna)
- Upload de arquivo idêntico responde `409` com link para o existente, e um
  parâmetro explícito (`?force=true`) para enviar assim mesmo
- Aviso quando já existe extrato do mesmo cliente e período, mesmo com arquivo
  diferente

**Pronto quando**: enviar o mesmo arquivo duas vezes por engano não cria dois
extratos em silêncio.

**Feedback**: bloquear ou só avisar? Reenviar o mesmo período é erro ou rotina
(extrato parcial atualizado, por exemplo)?

---

## Fase 4 — Autenticação

**Objetivo**: cada pessoa entra com o próprio usuário.

**Entrega**:
- Spring Security; a entidade `User` e a tabela `users` já existem desde a V1
- `CurrentOrganizationProvider` passa a ler do `SecurityContext` — a costura foi
  feita justamente para isso, nenhum controller muda
- Cadastro de usuário pelo administrador do escritório
- Testes de isolamento: usuário de uma organização não alcança dados de outra

**Pronto quando**: derrubar a sessão bloqueia o acesso a toda a API.

**Nota de posição**: se a aplicação for ficar só na rede interna, esta fase pode
esperar. Se for acessível de fora, **ela vem antes de qualquer outra**.

---

## Fase 5 — Frontend novo

**Objetivo**: substituir o andaime pelo modelo de tela definido por você.

**Entrega**: depende do modelo. O que já está pronto do lado da API:
listagem de clientes e extratos, upload, download de CSV, e (conforme as fases
2–4 avancem) PDF original, exclusão e login.

**Pronto quando**: o andaime atual pode ser apagado do repositório.

**Bloqueio**: aguardando o modelo. Enquanto isso, as fases independentes andam.

---

## Fase 6 — Conferência do extrato

**Objetivo**: transformar `validation_flags` em trabalho de conferência de
verdade, em vez de um campo que ninguém olha.

**Entrega**:
- As divergências de saldo aparecem na tela, na linha certa
- Marcar um extrato como conferido (quem e quando)
- Corrigir uma transação manualmente, com registro de que foi editada — o valor
  original nunca é sobrescrito sem trilha

**Pronto quando**: dá para saber, olhando a lista, quais extratos precisam de
atenção humana.

**Feedback**: as divergências que o sistema aponta são as que importam, ou é
ruído?

---

## Fase 7 — Exportação para a contabilidade

**Objetivo**: exportar no formato que o sistema contábil do escritório aceita.

**Entrega**:
- Excel (`.xlsx`) além do CSV — o Apache POI foi removido do `pom.xml` quando
  virou peso morto e volta aqui, com uso de verdade
- Layout de colunas definido pelo que a Fase 1 revelar
- Se houver um formato de importação específico do sistema contábil, ele entra
  aqui

**Pronto quando**: o arquivo importa no sistema contábil sem edição manual.

---

## Fase 8 — Segundo banco

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

## Fase 9 — Relatórios

**Objetivo**: responder perguntas que hoje exigem abrir o CSV no Excel.

**Entrega**:
- Totais por cliente e período
- Evolução de saldo
- Filtros e paginação nas listagens — o volume real medido nas fases anteriores
  é que diz se isso já é necessário

**Pronto quando**: a pergunta mais frequente do escritório é respondida sem
exportar nada.

---

## Fase 10 — Operação

**Objetivo**: o sistema sobreviver a uma semana sem ninguém olhando.

**Entrega**:
- Backup automático de Postgres e MinIO, com restauração testada de verdade
- Healthcheck (`/actuator/health`) e reinício automático no Compose
- Retenção e rotação de log
- Procedimento de atualização sem perder dados

**Pronto quando**: restaurar do backup num ambiente limpo recupera tudo.

---

## Fase 11 — SaaS

Só depois de o escritório usar o sistema por um tempo e as fases acima estarem
estáveis: cadastro de novas organizações, cobrança (Stripe), onboarding,
limites por plano.

---

## Como usar este documento

- Uma fase por vez. Terminou, colhe o feedback, e só então decide a próxima —
  a ordem daqui para frente é uma proposta, não um contrato.
- Fase que não cabe em poucos dias está mal recortada: quebre.
- Ao concluir, marque aqui o que ficou **verificado** e o que ficou **suposto**.
  A Fase 0 é o exemplo: o código está pronto, mas quatro coisas nunca rodaram de
  verdade, e isso está escrito lá em vez de ficar implícito.

---
*Atualizado: 2026-09-16*
