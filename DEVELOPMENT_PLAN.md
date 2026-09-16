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

- [ ] Docker Compose: PostgreSQL 15 + pgAdmin (dev)
- [ ] `application.yml` (profiles dev/prod)
- [ ] Entidades JPA:
  - [ ] `Organization` (seu escritório, depois mais)
  - [ ] `User` (funcionários, preparado para auth futura)
  - [ ] `Client` (CNPJ atendido)
  - [ ] `Statement` (PDF enviado, metadata)
  - [ ] `Transaction` (linha extraída)
- [ ] Migrations Flyway (V1_init_schema.sql)
- [ ] Repositories (JpaRepository para cada entidade)
- [ ] Configuração Spring Boot mínima (sem controllers ainda)

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

- [ ] `docker-compose up` sobe Postgres + pgAdmin sem erros
- [ ] Migrations rodam automaticamente (Flyway)
- [ ] Tables criadas corretamente em Postgres
- [ ] Spring Boot sobe sem erros (mesmo sem controllers)
- [ ] pgAdmin acessível em localhost:5050 com dados visíveis

---

## Fase 3: Integração Parser → API + Storage

**Objetivo**: Conectar o parser (Fase 1) com o banco (Fase 2) e storage MinIO. Primeira funcionalidade real.

### Tarefas

- [ ] Docker Compose: adicionar MinIO container
- [ ] Interface `StorageService` (contrato abstrato)
- [ ] Implementação `MinIOStorageService` (upload/download PDFs e CSVs)
- [ ] Serviço `StatementProcessingService`:
  - [ ] Recebe PDF + Client ID
  - [ ] Chama parser (Fase 1)
  - [ ] Persiste Statement + Transactions no Postgres
  - [ ] Armazena PDF + CSV gerado no MinIO
- [ ] Controller `StatementController`:
  - [ ] POST `/api/statements/upload` (multipart/form-data)
  - [ ] GET `/api/statements/{id}` (metadata)
  - [ ] GET `/api/statements/{id}/export?format=csv|excel`
- [ ] Tratamento de erros (arquivo não encontrado, parsing falhou, etc.)

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

- [ ] `docker-compose up` sobe Postgres + MinIO sem erros
- [ ] POST `/api/statements/upload` aceita PDF, persiste em DB + MinIO
- [ ] GET `/api/statements/{id}` retorna metadata (fileName, uploadedAt, transactionCount)
- [ ] GET `/api/statements/{id}/export?format=csv` baixa CSV
- [ ] Erro claro quando parsing falha (não salva dado errado)
- [ ] MinIO console acessível (localhost:9001)

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
| 0/1 | ✅ Concluída | Parser Java funcional, 10/10 testes passando. Falta apenas adicionar PDFs "golden" reais (opcional). |
| 2 | ⏳ Pendente | Próxima fase |
| 3 | ⏳ Pendente | |
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
