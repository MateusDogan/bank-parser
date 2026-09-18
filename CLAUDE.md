# Bank Parser - Guia do Projeto

## Visão Geral
Ferramenta para converter extratos bancários em PDF (formato Stone, expansível para outros bancos) em CSV. O PDF enviado fica guardado no backend e as transações extraídas ficam no banco, disponíveis para exportar. Uso interno de um escritório de contabilidade.

**Escopo deliberadamente enxuto** (decisão de 2026-09-18): não há cadastro de clientes/CNPJ, nem multi-tenant, nem autenticação. O extrato é identificado pelo próprio arquivo. Se um dia virar SaaS, `organization_id` volta como migration — foi removido justamente por ser andaime sem uso.

**Plano de desenvolvimento completo**: ver [`DEVELOPMENT_PLAN.md`](./DEVELOPMENT_PLAN.md) — sempre consultar antes de começar uma nova fase.

## Tech Stack
- **Backend**: Java 17 + Spring Boot 3
- **Frontend**: React 18 (Vite ou Next.js)
- **Banco de Dados**: PostgreSQL 15
- **Storage**: MinIO (S3-compatible)
- **Extração de PDF**: Apache PDFBox
- **Build**: Maven (backend), npm/pnpm (frontend)
- **Containerização**: Docker + Docker Compose

## Estrutura do Projeto
```
bank-parser/
├── DEVELOPMENT_PLAN.md      # Plano fase por fase — CONSULTAR SEMPRE
├── CLAUDE.md                # Este arquivo
├── README.md                # Overview para humanos
├── docker-compose.yml       # Postgres + MinIO + (API depois)
├── .env.example
├── backend/                 # Spring Boot
│   ├── pom.xml
│   └── src/
└── frontend/                # React
    ├── package.json
    └── src/
```

## Modelo de Dados
```
Statement            → PDF enviado (arquivo no storage + metadados do cabeçalho)
  └── Transaction    → linha extraída
```

## Decisões de Arquitetura (não mudar sem discussão explícita)

1. **Escopo mínimo** — só o que o fluxo "PDF entra, CSV sai" exige. Cliente, tenant e usuário foram removidos por serem estrutura sem uso; não voltam sem uma necessidade concreta
2. **Storage abstrato** (`StorageService` interface) — implementação MinIO hoje, trocável por S3/R2 depois sem mudar código de negócio
3. **Parser isolado com teste de regressão contra extrato real** — o PDF real nunca é versionado, então o teste é pulado quando ele não está na máquina
4. **DTOs separados de Entities JPA** — contrato de API não quebra quando o schema do banco muda
5. **Sem autenticação** — o sistema roda na rede interna do escritório. **Expor fora dela obriga a adicionar login antes**: sem ele, qualquer pessoa na rede lê os dados financeiros
6. **Soft-delete em tudo** — histórico contábil não some; nada é removido fisicamente

## Setup Local

Passo a passo detalhado (com troubleshooting) em `SETUP_MVP.md`.

```bash
# 1. Subir infraestrutura (Postgres + MinIO)
cp .env.example .env      # esse nome exato: e o unico que o Compose le sozinho
docker compose up -d

# 2. Rodar backend (Spring Boot)
cd backend
mvn spring-boot:run

# 3. Rodar frontend (React)
cd frontend
npm install
npm run dev
```

Não há Maven wrapper neste projeto: use o `mvn` instalado na máquina.

## Rodando Testes

```bash
cd backend
mvn test
```

Esperado: 32 testes, 2 pulados. Não precisa de Docker — Postgres e um endpoint S3
rodam embarcados. Os 2 pulados dependem de um extrato real, que nunca é
versionado:

```bash
mvn test "-Dbankparser.it.pdf=C:\caminho\para\extrato.pdf"
```

O frontend tem `vitest` configurado, mas nenhum teste escrito.

## Antes de Commitar
- [ ] Testes passam (`mvn test` no backend)
- [ ] Sem PDFs reais de clientes commitados (verificar `.gitignore`)
- [ ] Sem credenciais/senhas hardcoded
- [ ] `DEVELOPMENT_PLAN.md` atualizado se a fase mudou de status
- [ ] Mensagem de commit descritiva (formato: `feat(parser): ...`, `fix(api): ...`)

## Dados Sensíveis
- PDFs de extratos reais **nunca** são commitados (git-ignored)
- `.env` com credenciais nunca é commitado
- Dados de teste usam PDFs sintéticos ou anonimizados quando possível

---
*Última atualização: 2026-09-18*
