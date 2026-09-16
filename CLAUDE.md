# Bank Parser - Guia do Projeto

## Visão Geral
Plataforma para converter extratos bancários em PDF (formato Stone, expansível para outros bancos) em dados estruturados, com suporte a múltiplos clientes/CNPJs por escritório. Começa como ferramenta interna para um escritório de contabilidade, arquitetado para eventualmente virar SaaS multi-tenant.

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

## Modelo de Dados (Multi-tenant)
```
Organization (tenant)     → hoje: 1 registro (o escritório). Amanhã: múltiplos.
  └── User                → funcionários (tabela existe; auth só na Fase 10)
  └── Client              → CNPJ atendido pelo escritório
        └── Statement     → PDF enviado
              └── Transaction → linha extraída
```

**Regra crítica**: toda query de dados filtra por `organization_id`. Nunca remover esse filtro, mesmo com uma única Organization hoje — é o que evita reescrever o modelo de dados quando virar SaaS.

## Decisões de Arquitetura (não mudar sem discussão explícita)

1. **Multi-tenant desde o início** — mesmo com 1 Organization hoje
2. **Storage abstrato** (`StorageService` interface) — implementação MinIO hoje, trocável por S3/R2 depois sem mudar código de negócio
3. **Parser isolado com testes "golden"** — PDFs reais de teste com output esperado documentado, antes de integrar com API/DB
4. **DTOs separados de Entities JPA** — contrato de API não quebra quando o schema do banco muda
5. **Sem autenticação por enquanto** — `CurrentOrganizationProvider` é a costura única: na Fase 10 ele passa a ler do `SecurityContext` e nenhum controller muda. Vale só porque o sistema roda na rede interna do escritório; **expor fora dela obriga a fazer a Fase 10 antes** (ver `DEVELOPMENT_PLAN.md`)

## Setup Local

Passo a passo detalhado (com troubleshooting) em `SETUP_MVP.md`.

```bash
# 1. Subir infraestrutura (Postgres + MinIO)
cp .env.example .env      # esse nome exato: e o unico que o Compose le sozinho
docker compose up -d

# 2. Rodar backend (Spring Boot)
cd backend
mvn spring-boot:run

# 3. Rodar frontend (React) — andaime, sera substituido na Fase 4
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

Esperado: 43 testes, 2 pulados. Não precisa de Docker — Postgres e um endpoint S3
rodam embarcados. Os 2 pulados dependem de um extrato real, que nunca é
versionado:

```bash
mvn test "-Dbankparser.it.pdf=C:\caminho\para\extrato.pdf"
```

O frontend tem `vitest` configurado, mas nenhum teste escrito — não vale
investir enquanto ele for andaime.

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
*Última atualização: 2026-09-16*
