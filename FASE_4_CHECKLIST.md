# Fase 4-MVP: Checklist de Configurações e Arquivos

## Status: ✅ CONCLUÍDA (2026-09-16)

Este documento foi o guia de execução; ficou como registro histórico. Para o estado atual do
projeto, ver `DEVELOPMENT_PLAN.md` (seção "Fase 4-MVP") e `SETUP_MVP.md` (como rodar).

Resultado: 33 testes de backend passando, `npm run build` do frontend sem erros, GitHub Actions
configurado (test + deploy). Pendência única: teste manual end-to-end com Docker rodando (Postgres
+ MinIO reais), que depende do Docker Desktop nesta máquina.

---

## 1. Backend — Java/Spring

### ✅ Já Existe
- `backend/pom.xml` — dependências (Spring Boot, JPA, PDFBox, MinIO, etc.)
- `backend/src/main/java/com/bankparser/` — packages:
  - ✅ `entity/` (Organization, User, Client, Statement, Transaction, BaseEntity)
  - ✅ `repository/` (ClientRepository, StatementRepository, TransactionRepository, OrganizationRepository)
  - ✅ `controller/StatementController.java` (POST /upload, GET /{id}, GET /{id}/export)
  - ✅ `service/` (StatementProcessingService, CurrentOrganizationProvider)
  - ✅ `parser/` (BankStatementParser, StoneParser)
  - ✅ `storage/` (StorageService, MinIOStorageService)
  - ✅ `exception/GlobalExceptionHandler.java`
  - ✅ `dto/` (StatementResponse, ErrorResponse)
- `backend/src/main/resources/application.yml` — config (minio, multipart limits)
- `backend/src/main/resources/db/migration/` — Flyway migrations (V1, V2)
- `backend/src/test/` — 28 testes passando

### ❌ FALTA PARA MVP
1. **`controller/ClientController.java`** — POST e GET `/api/clients`
   - POST: criar cliente (ClientRequest → Client)
   - GET: listar todos os clientes (Page<ClientResponse>)
   - Validação: CNPJ, name obrigatório
   
2. **`dto/ClientRequest.java`** — input do POST
   ```java
   record ClientRequest(String name, String document) {}
   ```
   
3. **`dto/ClientResponse.java`** — output do GET
   ```java
   record ClientResponse(UUID id, String name, String document, Instant createdAt) {}
   ```

4. **`application.yml` updates**:
   - ✅ Já tem `minio` block (verificar se está completo)
   - ✅ Já tem `spring.servlet.multipart.max-file-size: 25MB`
   - Verificar se `open-in-view: false` está setado (necessário)

### Onde encontrar o ClientRepository
- `backend/src/main/java/com/bankparser/repository/ClientRepository.java` ✅
  - Métodos já existem: `findByOrganizationIdAndDocument()`, `existsByOrganizationIdAndDocument()`

---

## 2. Frontend — React + Vite

### ✅ Já Existe
- `frontend/package.json` (pode precisar updates)
- `frontend/src/README.md` (placeholder)

### ❌ FALTA PARA MVP
1. **`frontend/src/main.jsx`** — entry point React
2. **`frontend/src/App.jsx`** — componente raiz, roteamento básico (2 páginas)
3. **`frontend/src/pages/Upload.jsx`** — página de upload
   - Dropdown: lista de clientes (GET /api/clients)
   - Input/drag-drop file
   - Botão submit
   - Feedback (enviando, sucesso, erro)
   
4. **`frontend/src/pages/Statements.jsx`** — página de statements
   - Tabela: ID, Filename, Uploaded date
   - Botão download CSV para cada linha
   - Link voltar para Upload
   
5. **`frontend/src/services/api.js`** — axios client
   ```javascript
   const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
   // GET /api/clients
   // POST /api/statements/upload
   // GET /api/statements/{id}/export
   ```

6. **`frontend/src/index.css`** — estilos minimalistas (inline ok tbm)
7. **`frontend/.env.example`** — template de variáveis
   ```
   REACT_APP_API_URL=http://localhost:8080
   ```

8. **`frontend/vite.config.js`** — config Vite (padrão, pode gerar com `npm create vite`)
9. **`frontend/index.html`** — HTML base

### Atualizar `frontend/package.json`
```json
{
  "name": "bank-parser-frontend",
  "version": "0.1.0",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.0.0",
    "vite": "^4.4.0"
  }
}
```

---

## 3. GitHub Actions (CI/CD Automático)

### ❌ FALTA (Criar)
1. **`.github/workflows/frontend-deploy.yml`**
   - Trigger: push to main (branch)
   - Steps:
     1. Checkout code
     2. npm install
     3. npm run build
     4. Deploy `frontend/dist/` to gh-pages branch
   - Resultado: Frontend em `https://username.github.io/bank-parser`

2. **`.github/workflows/backend-test.yml`** (opcional, mas bom ter)
   - Trigger: push to main
   - Steps:
     1. Checkout
     2. mvn clean test
   - Fail se testes quebram

### Exemplo `frontend-deploy.yml`
```yaml
name: Deploy Frontend

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd frontend && npm install && npm run build
      - uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./frontend/dist
```

---

## 4. Docker & Local Development

### ✅ Já Existe
- `docker-compose.yml` (Postgres + MinIO)

### ❌ FALTA (Criar/Atualizar)
1. **`.env.example`** — template de variáveis de ambiente
   ```
   # Postgres
   POSTGRES_DB=bank_parser
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=changeme
   
   # MinIO
   MINIO_ENDPOINT=http://minio:9000
   MINIO_ACCESS_KEY=minioadmin
   MINIO_SECRET_KEY=minioadmin
   MINIO_BUCKET_STATEMENTS=statements
   
   # Backend
   API_PORT=8080
   ```

2. **`.env.local`** — git-ignored, valores locais para developer
   (Copiar `.env.example` → `.env.local`, preencher)

3. **`docker-compose.yml` review**:
   - ✅ Postgres 15
   - ✅ MinIO
   - ❌ Falta: backend service (Spring Boot comentado ou não)
   - ❌ Falta: frontend service (ou instrução que roda `npm run dev` local)

### Verificar `.gitignore`
```
.env.local
.env.*.local
node_modules/
frontend/dist/
backend/target/
*.jar
.DS_Store
```

---

## 5. Documentação

### ✅ Já Existe
- `README.md` — overview
- `DEVELOPMENT_PLAN.md` — plano de fases
- `CLAUDE.md` — guia do projeto
- `ROADMAP_2026-Q3.md` — roadmap completo

### ❌ FALTA PARA MVP
1. **`SETUP_MVP.md`** — Como rodar MVP localmente
   ```markdown
   # Setup: MVP Local
   
   ## Pré-requisitos
   - Java 17 (ou latest JDK)
   - Maven 3.9+
   - Node.js 18+
   - Docker + Docker Compose
   
   ## 1. Infra (Postgres + MinIO)
   ```bash
   cp .env.example .env.local
   docker-compose up -d
   ```
   
   ## 2. Backend
   ```bash
   cd backend
   mvn spring-boot:run
   # Aguarda: "Tomcat started on port 8080"
   ```
   
   ## 3. Frontend
   ```bash
   cd frontend
   npm install
   npm run dev
   # Acessa http://localhost:5173
   ```
   
   ## Testes
   ```bash
   cd backend
   mvn test
   # Esperado: 28+ testes passando
   ```
   
   ## Troubleshooting
   - Port 8080 em uso: mudar em application.yml `server.port`
   - MinIO não conecta: `docker-compose logs minio`
   - Postgres erro: `docker-compose down -v` (limpa volumes)
   ```

2. **`DEPLOY_GITHUB_PAGES.md`** — Como publicar frontend em GitHub Pages
   ```markdown
   # Deploy: Frontend em GitHub Pages
   
   ## Automático (via GitHub Actions)
   1. GitHub → Settings → Actions → Workflows
   2. Verificar `.github/workflows/frontend-deploy.yml` ativo
   3. Push pra main em `frontend/` folder → auto-deploy
   4. Acessa `https://USERNAME.github.io/bank-parser`
   
   ## Manual (se Actions desativado)
   ```bash
   cd frontend
   npm run build
   git checkout gh-pages
   cp -r dist/* .
   git add . && git commit -m "Deploy frontend"
   git push origin gh-pages
   ```
   ```

---

## 6. GitHub Setup (Primeira Vez)

### ❌ FALTA (Fazer no GitHub)
1. **Enable GitHub Pages**
   - Repository → Settings → Pages
   - Source: `gh-pages` branch
   - (ou `Deploy from a branch` → `gh-pages`)
   
2. **Enable GitHub Actions**
   - Repository → Settings → Actions → General
   - Workflows: Allow all actions and reusable workflows
   
3. **Branch Protection** (opcional, bom ter)
   - Repository → Settings → Branches
   - Add rule: `main`
   - Require status checks to pass (backend tests)

---

## 7. Arquivos de Configuração: Checklist Final

### Backend
- ✅ `backend/pom.xml`
- ✅ `backend/src/main/resources/application.yml`
- ✅ `backend/src/main/resources/db/migration/V1__*.sql`
- ❌ **ClientController.java**
- ❌ **ClientRequest.java**
- ❌ **ClientResponse.java**

### Frontend
- ❌ **frontend/vite.config.js**
- ❌ **frontend/index.html**
- ❌ **frontend/src/main.jsx**
- ❌ **frontend/src/App.jsx**
- ❌ **frontend/src/pages/Upload.jsx**
- ❌ **frontend/src/pages/Statements.jsx**
- ❌ **frontend/src/services/api.js**
- ❌ **frontend/src/index.css**
- ⚠️ `frontend/package.json` (update scripts)

### GitHub
- ❌ **.github/workflows/frontend-deploy.yml**
- ❌ **.github/workflows/backend-test.yml** (opcional)

### Docker/Env
- ✅ `docker-compose.yml` (verificar MinIO + Postgres)
- ❌ **.env.example**
- ❌ **.gitignore** (verificar .env.local ignorado)

### Docs
- ✅ `README.md`
- ✅ `DEVELOPMENT_PLAN.md`
- ❌ **SETUP_MVP.md**
- ❌ **DEPLOY_GITHUB_PAGES.md**

---

## 8. Ordem de Criação (Recomendada)

### Dia 1: Backend (1 dia)
1. Criar `ClientController.java`
2. Criar `ClientRequest.java` + `ClientResponse.java`
3. Testar: `mvn test` (28+ testes passando)
4. Manual: `curl -X POST http://localhost:8080/api/clients -H "Content-Type: application/json" -d '{"name":"Test","document":"12345678000190"}'`

### Dia 2: Frontend (1.5 dias)
1. Setup Vite: `cd frontend && npm init -y && npm install react react-dom axios vite @vitejs/plugin-react`
2. Criar estrutura (main.jsx, App.jsx, pages/, services/)
3. Testar local: `npm run dev`
4. Build: `npm run build` → `frontend/dist/`

### Dia 3: GitHub + Deploy (0.5 dias)
1. Criar `.github/workflows/frontend-deploy.yml`
2. Push pra main → Actions roda → Deploy automático
3. Atualizar docs: `SETUP_MVP.md`, `DEPLOY_GITHUB_PAGES.md`
4. `.env.example`, verificar `.gitignore`

---

## 9. Pronto? Checklist Final

Antes de começar Fase 4, confirme:

- [ ] Backend compila: `mvn clean compile`
- [ ] Testes passam: `mvn test` (28+)
- [ ] docker-compose funciona: `docker-compose up -d` → servicos rodando
- [ ] `.env.example` existe com variaveis
- [ ] `.gitignore` ignora `.env.local`, `node_modules/`, `target/`, `dist/`
- [ ] GitHub repo criado e vazio (ou com apenas README.md)
- [ ] GitHub Actions habilitado (Settings → Actions)
- [ ] GitHub Pages habilitado (Settings → Pages)

Se tudo ok: **Pronto para implementar Fase 4 em outro modelo**

---

*Atualizado: 2026-09-16*
