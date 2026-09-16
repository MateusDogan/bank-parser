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

## Origem

Este projeto evoluiu de um script Python de conversão PDF→CSV para extratos Stone, sendo reconstruído em Java/Spring Boot para suportar múltiplos clientes e escalar eventualmente para um produto SaaS.
