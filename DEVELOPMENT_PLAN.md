# Bank Parser - Plano de Desenvolvimento

> Este documento guia o desenvolvimento fase por fase. Atualizar conforme progresso.

**Objetivo Final**: Transformar um parser de PDF bancário em uma plataforma escalável para múltiplos escritórios de contabilidade, começando como ferramenta interna, evoluindo para SaaS.

**Stack**: Java 17 + Spring Boot 3 | React 18 | PostgreSQL 15 | MinIO | Docker

**Data de Início**: 2026-09-16

---

## Resumo de Fases

| Fase | Nome | Duração | Objetivo |
|------|------|---------|----------|
| 0/1 | Parser em Java (isolado) | 4-5 dias | Refatorar lógica Python → Java, validar com testes "golden" |
| 2 | Infraestrutura + Multi-tenant | 2-3 dias | Docker Compose, Postgres, modelo de dados |
| 3 | Integração Parser → API + Storage | 3-4 dias | Controllers, Storage MinIO, persistência |
| 4 | API Completa | 2-3 dias | CRUD clients, statements, transactions, paginação |
| 5 | Frontend React | 3-5 dias | Upload, dashboard, download CSV/Excel |
| 6 | Testes e Deployment Local | 2-3 dias | Testes integrados, backup, documentação |

**Total Estimado**: 16-23 dias até MVP funcional no escritório

---

## Fase 0/1: Parser em Java (Isolado)

**Objetivo**: Refatorar a extração de PDF (Python → Java) com testes robustos, sem depender de API/DB/Docker.

**Por que isolado?**: Validar que a lógica de parsing está correta antes de integrar com o resto do sistema. Bugs de extração descobertos tarde custam caro.

### Tarefas

- [x] Setup Maven + dependências (PDFBox 3.0.2)
- [x] Reescrever `BankStatementParser` (interface abstrata)
- [x] Implementar `StoneParser` (heurística de coordenadas → grupos de linhas)
- [x] Implementar `TransactionExtractor` (regex de data, detecção Entrada/Saída, parsing de moeda)
- [x] Criar testes com PDFs sintéticos gerados via PDFBox (entrada conhecida = saída esperada) — ver nota abaixo
- [x] Validação robusta (erro claro quando PDF inválido/formato desconhecido — `StatementParsingException`)
- [x] DTOs para output (`ParsedTransaction`, `StatementMetadata`, `ParsingResult`)

> **Nota sobre os testes "golden"**: em vez de PDFs reais de clientes, os testes atuais geram PDFs sintéticos
> em memória (via PDFBox) reproduzindo o layout de colunas da Stone — evita commitar dados financeiros
> sensíveis no repositório. Recomenda-se adicionar 1-2 PDFs reais (idealmente anonimizados) em
> `src/test/resources/parser/` para validar contra o layout real quando disponível (ver README daquela pasta).
>
> **Descoberta durante a implementação**: ao gerar PDFs sintéticos, colunas com pouco espaçamento horizontal
> fazem o PDFBox fundir palavras adjacentes em um único trecho de texto na extração (`writeString`), quebrando
> a divisão em "palavras" — corrigido usando espaçamento generoso entre colunas nos PDFs de teste. Não afeta
> extratos reais da Stone (cujo layout já tem esse espaçamento natural), mas é relevante para quem for
> escrever novas fixtures sintéticas no futuro (ex.: para outro banco).

### Deliverables

```
backend/
├── pom.xml (com PDFBox, JUnit 5, AssertJ)
├── src/main/java/com/bankparser/
│   └── parser/
│       ├── BankStatementParser.java (interface)
│       ├── StoneParser.java (implementação)
│       ├── TransactionExtractor.java (lógica de regex/parsing)
│       └── dto/
│           ├── StatementMetadata.java
│           ├── Transaction.java
│           └── ParsingResult.java
└── src/test/java/com/bankparser/
    └── parser/
        ├── StoneParserTest.java
        ├── resources/
        │   ├── test_extrato_1.pdf
        │   ├── test_extrato_1_expected.json
        │   └── ... (2-4 mais)
```

### Checklist de Conclusão

- [x] Todos os testes passam (10/10 — `mvn test`)
- [ ] PDFs "golden" reais adicionados (pendente — hoje só sintéticos, ver nota acima)
- [x] Erro claro quando PDF não é Stone ou inválido (não silent failure)
- [x] `CLAUDE.md` do projeto documenta como rodar (`./mvnw test` — ver seção "Setup Local")
- [x] Projeto compila sem erros (`mvn compile`)

---

## Fase 2: Infraestrutura + Multi-tenant

**Objetivo**: Montar Docker Compose, banco de dados, modelo de dados multi-tenant (Organization/Client/Statement/Transaction).

### Tarefas

- [x] Docker Compose: PostgreSQL 15 + pgAdmin (dev)
- [x] `application.yml` (base + profile `dev` com SQL logging)
- [x] Entidades JPA:
  - [x] `Organization` (seu escritório, depois mais)
  - [x] `User` (funcionários, preparado para auth futura)
  - [x] `Client` (CNPJ atendido)
  - [x] `Statement` (PDF enviado, metadata)
  - [x] `Transaction` (linha extraída)
- [x] Migrations Flyway (`V1__init_schema.sql`, `V2__seed_default_organization.sql`)
- [x] Repositories (JpaRepository para cada entidade, sempre filtrando por `organizationId`)
- [x] Configuração Spring Boot mínima (sem controllers ainda)

> **Soft-delete**: todas as entidades herdam `BaseEntity` (id, `createdAt`, `updatedAt`, `deleted`) e usam
> `@SQLDelete` + `@SQLRestriction` do Hibernate. `repository.delete(x)` vira um `UPDATE ... SET deleted = true`
> e toda query passa a ignorar a linha automaticamente — sem precisar repetir o filtro em cada método.
> O índice único de `clients` é parcial (`WHERE deleted = false`), então um CNPJ removido pode ser recadastrado.
>
> **`organization_id` denormalizado** em `statements` e `transactions`: o filtro de tenant obrigatório vira um
> predicado indexado direto, em vez de um join em cadeia até `clients`.
>
> **Testes sem Docker**: `SchemaIntegrationTest` sobe um Postgres 15 real (binário embarcado via
> `io.zonky.test:embedded-postgres`, sem daemon) e roda o contexto Spring inteiro. Como o `application.yml`
> usa `ddl-auto: validate`, o contexto só sobe se cada campo das entidades bater com a coluna criada pelo
> Flyway — divergência entre Java e SQL quebra no teste, não em produção.

### Deliverables

```
docker-compose.yml (Postgres + pgAdmin)
.env.example (configurações)
.env.local (git-ignored, local)

backend/src/main/java/com/bankparser/
├── config/
│   └── DatabaseConfig.java
├── entity/
│   ├── Organization.java
│   ├── User.java
│   ├── Client.java
│   ├── Statement.java
│   └── Transaction.java
├── repository/
│   ├── OrganizationRepository.java
│   ├── ClientRepository.java
│   ├── StatementRepository.java
│   └── TransactionRepository.java
└── resources/
    ├── application.yml
    ├── application-dev.yml
    └── db/migration/
        └── V1__init_schema.sql
```

### Checklist de Conclusão

- [ ] `docker-compose up` sobe Postgres + pgAdmin sem erros — **pendente**: Docker Desktop foi instalado
      nesta máquina mas não foi configurado (requer aceitar a tela inicial). Validar quando o ambiente estiver pronto.
- [x] Migrations rodam automaticamente (Flyway) — verificado em `SchemaIntegrationTest`
- [x] Tables criadas corretamente em Postgres — verificado contra Postgres 15.6 real
- [x] Spring Boot sobe sem erros (mesmo sem controllers) — contexto completo sobe com `ddl-auto: validate`
- [ ] pgAdmin acessível em localhost:5050 com dados visíveis — **pendente**, depende do Docker

---

## Fase 3: Integração Parser → API + Storage

**Objetivo**: Conectar o parser (Fase 1) com o banco (Fase 2) e storage MinIO. Primeira funcionalidade real.

### Tarefas

- [x] Docker Compose: MinIO container — já existia desde a Fase 0
- [x] Interface `StorageService` (contrato abstrato, sem tipos do MinIO)
- [x] Implementação `MinIOStorageService` + `MinIOConfig`/`MinIOProperties`
- [x] Serviço `StatementProcessingService`:
  - [x] Recebe PDF + Client ID
  - [x] Chama parser (Fase 1)
  - [x] Persiste Statement + Transactions no Postgres
  - [x] Armazena o PDF no MinIO
  - [x] Confere o CNPJ do extrato contra o do cliente
- [x] Controller `StatementController`:
  - [x] POST `/api/statements/upload` (multipart/form-data)
  - [x] GET `/api/statements/{id}` (metadata)
  - [x] GET `/api/statements/{id}/export?format=csv`
- [x] Tratamento de erros (`GlobalExceptionHandler`)

> **CSV gerado sob demanda**, não guardado no MinIO: o CSV é função das linhas já persistidas, e
> materializá-lo criaria uma segunda cópia que pode divergir do banco. Só o PDF original vai para o
> storage. Excel fica para a Fase 4/5, junto dos relatórios.
>
> **Ordem de gravação**: o PDF sobe para o storage *antes* do commit. Falha no storage aborta a
> transação e no pior caso deixa um objeto órfão no bucket — lixo coletável. Na ordem inversa sobraria
> um `Statement` apontando para arquivo inexistente, que é dado corrompido.
>
> **Conferência de documento**: upload com CNPJ divergente do cliente responde **409**. Num escritório
> contábil, um extrato lançado sob o cliente errado é um erro caro e silencioso. Quando o parser não
> extrai o documento do cabeçalho, o upload segue sem conferir.
>
> **`CurrentOrganizationProvider`**: o `organizationId` sai de um único ponto, que hoje devolve a
> organização padrão e na Fase 7 passa a ler do `SecurityContext` — nenhum controller muda.
>
> **Parser por `bankKey`**: o serviço indexa os `BankStatementParser` disponíveis e o endpoint aceita
> `bankKey` (default `stone`), então incluir outro banco na Fase 8 não altera o contrato da API.

### Deliverables

```
backend/src/main/java/com/bankparser/
├── service/
│   ├── StatementProcessingService.java
│   ├── storage/
│   │   ├── StorageService.java (interface)
│   │   └── MinIOStorageService.java (implementação)
│   └── parser/
│       └── ParserFacade.java (orquestra parser + BD)
├── controller/
│   └── StatementController.java
└── dto/
    ├── UploadStatementRequest.java
    ├── StatementResponse.java
    └── ExportRequest.java

docker-compose.yml (adicionar MinIO)
```

### Checklist de Conclusão

- [ ] `docker-compose up` sobe Postgres + MinIO sem erros — **pendente**, depende do Docker
- [x] POST `/api/statements/upload` aceita PDF, persiste em DB + storage
- [x] GET `/api/statements/{id}` retorna metadata (fileName, uploadedAt, transactionCount)
- [x] GET `/api/statements/{id}/export?format=csv` baixa CSV
- [x] Erro claro quando parsing falha (422, e nada é gravado — verificado em teste)
- [ ] MinIO console acessível (localhost:9001) — **pendente**, depende do Docker

**Como isso foi verificado sem Docker** (28 testes, `mvn test`):

- `StatementControllerIntegrationTest` — contexto Spring completo + Postgres embarcado + `MockMvc`:
  upload de ponta a ponta, export CSV na ordem do PDF, PDF ilegível → 422 sem gravar nada, CNPJ
  divergente → 409, cliente de outra organização → 404. O `StorageService` aí é uma implementação em
  memória (`testsupport/InMemoryStorageService`).
- `MinIOStorageServiceTest` — o `MinIOStorageService` de verdade contra um endpoint S3 embarcado
  (`com.adobe.testing:s3mock-junit5`, também sem Docker): bucket criado sob demanda, bytes gravados e
  relidos, remoção. Como o MinIO fala S3, isso cobre o protocolo — não só a chamada de método.

Falta apenas o caminho manual contra o MinIO real (`docker-compose up`, upload, conferir em
`localhost:9001`), que depende de concluir a configuração do Docker Desktop nesta máquina.

---

## Fase 4: API Completa

**Objetivo**: Endpoints de CRUD, paginação, filtros. API pronta para consumir do frontend.

### Tarefas

- [ ] Controller `ClientController`:
  - [ ] GET `/api/clients` (lista, paginado)
  - [ ] POST `/api/clients` (criar novo CNPJ)
  - [ ] GET `/api/clients/{id}` (detalhes)
- [ ] Controller `TransactionController`:
  - [ ] GET `/api/statements/{statementId}/transactions` (paginado, filtrado)
  - [ ] Query params: page, size, filterBy (categoria, data, valor)
- [ ] Serviço de relatório (`ReportService`):
  - [ ] Saldo ao longo do tempo (por statement ou consolidado)
  - [ ] Distribuição por bandeira/categoria
  - [ ] Totais por mês
- [ ] Validação de input (CNPJ válido, datas, etc.)
- [ ] Logging estruturado (quem fez upload de qual PDF quando)

### Deliverables

```
backend/src/main/java/com/bankparser/
├── controller/
│   ├── ClientController.java
│   ├── TransactionController.java
│   └── ReportController.java
├── service/
│   ├── ClientService.java
│   ├── TransactionService.java
│   └── ReportService.java
├── dto/
│   ├── ClientRequest/Response
│   ├── TransactionResponse
│   └── ReportResponse
└── exception/
    └── GlobalExceptionHandler.java (erros padronizados)
```

### Checklist de Conclusão

- [ ] GET `/api/clients` retorna paginado
- [ ] POST `/api/clients` cria novo com validação de CNPJ
- [ ] GET `/api/statements/{id}/transactions` com filtros (categoria, data)
- [ ] GET `/api/reports/balance-timeline?statementId=...` retorna série temporal
- [ ] Todos os endpoints retornam JSON com estrutura consistente
- [ ] Documentação Swagger gerada automaticamente

---

## Fase 5: Frontend React

**Objetivo**: Interface para upload e visualização de dados. MVP de UX.

### Tarefas

- [ ] Setup Next.js (ou Vite + React)
- [ ] Página de upload (drag-drop, progress bar)
- [ ] Página de statements (tabela listando PDFs processados)
- [ ] Página de transactions (visualizar linhas, filtros básicos)
- [ ] Download (botão CSV/Excel)
- [ ] Dashboard simples (gráficos: saldo, bandeiras, categorias)
- [ ] Tratamento de erros e feedback visual

### Deliverables

```
frontend/
├── package.json
├── src/
│   ├── pages/
│   │   ├── upload.jsx
│   │   ├── statements.jsx
│   │   ├── transactions.jsx
│   │   └── dashboard.jsx
│   ├── components/
│   │   ├── UploadDropZone.jsx
│   │   ├── StatementTable.jsx
│   │   ├── TransactionTable.jsx
│   │   └── Charts.jsx
│   ├── services/
│   │   └── api.js (axios ou fetch, chamadas para /api/...)
│   └── App.jsx
└── Dockerfile
```

### Checklist de Conclusão

- [ ] Página de upload funciona (upload real)
- [ ] Página de statements lista PDFs processados
- [ ] Página de transactions mostra linhas com paginação
- [ ] Download CSV/Excel funciona
- [ ] Gráficos (Chart.js ou Recharts) renderizam dados reais
- [ ] Erros de API mostram mensagens claras

---

## Fase 6: Testes e Deployment Local

**Objetivo**: Validar tudo junto, documentar, preparar para rodar no escritório.

### Tarefas

- [ ] Testes integrados (API + Parser + DB + MinIO)
- [ ] Testes E2E (frontend + backend) com Cypress ou Playwright
- [ ] Documentação:
  - [ ] README.md (overview)
  - [ ] SETUP.md (como rodar localmente)
  - [ ] API.md (documentação de endpoints)
  - [ ] DEPLOYMENT.md (como rodar no escritório)
- [ ] Scripts:
  - [ ] `docker-compose up` com seed (Organization padrão criada)
  - [ ] Backup automático de DB + MinIO
  - [ ] Restore de backup
- [ ] `.env.example` com todas as variáveis documentadas
- [ ] `.gitignore` completo (node_modules, target/, .env.local, data/MinIO, etc.)

### Deliverables

```
├── README.md
├── SETUP.md
├── API.md
├── DEPLOYMENT.md
├── docker-compose.yml (com seed)
├── .env.example
├── .gitignore
├── scripts/
│   ├── backup.sh
│   ├── restore.sh
│   └── seed-db.sql
└── backend/src/test/java/
    └── com/bankparser/integration/
        └── StatementProcessingIntegrationTest.java
```

### Checklist de Conclusão

- [ ] `docker-compose up` sobe tudo, aplica migrations, cria Organization default
- [ ] Upload PDF → API processa → dashboard mostra dados (fluxo end-to-end)
- [ ] Testes integrados passam (90%+ cobertura)
- [ ] Backup/restore funcionam
- [ ] Documentação é clara o suficiente pra colega rodar sem ajuda
- [ ] `.gitignore` exclui dados sensíveis (PDFs reais, .env.local, volumes Docker)

---

## Decisões de Arquitetura (não mudar sem discussão)

| Decisão | Rationale | Implicação |
|---|---|---|
| **Multi-tenant desde o início** | Hoje = 1 Organization, amanhã = muitas. Evita migração de schema. | Toda query filtra por Organization_ID. Código já pronto pra escalar. |
| **Storage abstrato (interface)** | Trocar de MinIO local para AWS S3/Cloudflare R2 depois é só mudar annotation. | StorageService + 2+ implementações (MinIO, S3, etc.). |
| **Parser isolado em testes** | Bugs de extração descobertos cedo, não em produção. | Testes "golden" com PDFs reais + saída esperada. |
| **DTOs separados de Entities** | Contrato de API não muda quando BD muda. | Mais boilerplate agora, menos quebra depois. |
| **Sem autenticação (por enquanto)** | MVP rápido, adicionada na Fase 7. | Controllers já estruturados para receber User (preparado). |
| **Soft-delete em tudo** | Auditoria contábil exige que o histórico sobreviva à remoção. | `@SQLDelete` + `@SQLRestriction`; ler linhas removidas exige query nativa. |
| **Reenvio do mesmo PDF cria novo Statement** | Deduplicar por hash esconderia reenvios legítimos (ex.: extrato corrigido pelo banco). | Quem envia responde pelo reenvio; sem validação de duplicata. |
| **Logging estruturado adiado** | Sem volume real ainda, seria complexidade especulativa. | Logs simples agora; migrar quando houver produção. |

---

## Próximas Fases (futuro, não agora)

- **Fase 7**: Autenticação (Spring Security + JWT ou OAuth)
- **Fase 8**: Suporte a mais bancos (ItauParser, BradescoParser, etc.)
- **Fase 9**: Exportação para ferramentas de contabilidade (Conta Azul, QuickBooks)
- **Fase 10**: Billing (Stripe) + SaaS público

---

## Status de Progresso

**Última atualização**: 2026-09-16

| Fase | Status | Notas |
|------|--------|-------|
| 0/1 | ✅ Concluída | Parser Java funcional. Validado contra extrato Stone real: 264/264 transações idênticas ao parser Python. |
| 2 | ✅ Concluída | Schema + entidades + repositories, validados contra Postgres 15 real. Falta só validar o `docker-compose` (Docker não configurado na máquina). |
| 3 | ✅ Concluída | Upload → parse → persistência → export CSV, 28/28 testes. MinIO coberto via endpoint S3 embarcado; falta o teste manual contra o MinIO real. |
| 4 | ⏳ Pendente | |
| 5 | ⏳ Pendente | |
| 6 | ⏳ Pendente | |

---

## Como Usar Este Documento

1. **Antes de cada sessão**: Revisar a fase atual e checklist
2. **Durante desenvolvimento**: Marcar tarefas conforme completa (`- [x]`)
3. **Ao final de fase**: Validar checklist, atualizar status
4. **Mudanças no plano**: Documentar aqui com data e rationale

---

*Mantido em: `bank-parser/DEVELOPMENT_PLAN.md`*  
*Última revisão: 2026-09-16*
