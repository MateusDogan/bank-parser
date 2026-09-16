# Setup: MVP Local

Como rodar o MVP (upload de PDF → download de CSV) na sua máquina.

## Pré-requisitos

- Java 17+ (JDK, não só JRE)
- Maven 3.9+
- Node.js 18+ (LTS recomendado)
- Docker + Docker Compose

## 1. Infraestrutura (Postgres + MinIO)

```bash
cp .env.example .env
# edite .env se quiser trocar as senhas padrao
docker compose up -d
```

> O arquivo precisa se chamar **`.env`** — é o único que o Docker Compose lê
> sozinho. Com outro nome (`.env.local`, por exemplo) ele sobe silenciosamente
> com as senhas padrão. Os dois nomes já estão no `.gitignore`.

Confirma que subiu:
```bash
docker compose ps
# postgres e minio devem aparecer como "healthy"
```

O pgAdmin não sobe por padrão (é ferramenta de inspeção, com senha fixa). Quando
precisar dele, em `localhost:5050`:
```bash
docker compose --profile tools up -d
```

## 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Aguarde a linha `Tomcat started on port 8080`. A primeira vez que rodar, o Flyway
aplica as migrations automaticamente (cria as tabelas + a Organization padrão).

**Teste manual rápido**:
```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Cliente Teste","document":"12345678000190"}'
```
Deve devolver `201` com o cliente criado.

## 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Acesse **http://localhost:5173**. O frontend já aponta para `http://localhost:8080`
por padrão (não precisa configurar nada — ver `frontend/.env.example` se quiser mudar).

## Testes automatizados

```bash
cd backend
mvn test
```

Esperado: 43 testes, 2 pulados. Não precisa do Docker rodando — os testes usam
Postgres e um endpoint S3 embarcados (sem daemon).

Os 2 pulados dependem de um extrato real, que nunca é versionado. Para rodá-los:
```bash
mvn test "-Dbankparser.it.pdf=C:\caminho\para\extrato.pdf"
```
Vale a pena quando você mexer no parser: `RealStatementRegressionTest` compara a
saída contra a baseline conhecida (264 transações, 11 divergências de saldo) e
falha se qualquer um dos dois mudar.

## Fluxo completo (manual)

1. Abra http://localhost:5173
2. Cadastre um cliente (nome + CNPJ/CPF)
3. Escolha o cliente no dropdown, selecione um PDF de extrato Stone, clique **Enviar**
4. Vá em **Meus Extratos** → clique **Baixar CSV**

## Troubleshooting

| Problema | Causa provável | Solução |
|---|---|---|
| Porta 8080 em uso | Outro processo usando a porta | Mude `server.port` em `application.yml`, ou mate o processo |
| MinIO não conecta | Container não subiu | `docker-compose logs minio` |
| Erro do Postgres ao subir backend | Volume corrompido de uma tentativa anterior | `docker-compose down -v` (apaga os dados locais) e suba de novo |
| Frontend mostra "Network Error" | Backend não está rodando, ou porta errada | Confirme `mvn spring-boot:run` está de pé e escutando 8080 |
| `409 Conflict` ao cadastrar cliente | CNPJ já cadastrado nesta organização | Esperado — cada CNPJ é único por organização |
| `409 Conflict` no upload do PDF | CNPJ do cabeçalho do PDF ≠ CNPJ do cliente escolhido | Confira se escolheu o cliente certo no dropdown |
| `422` no upload | PDF ilegível ou fora do layout Stone suportado | Confira se é mesmo um extrato Stone; nada é gravado quando isso acontece |

## O que NÃO está neste MVP (por escolha, não esquecimento)

- Paginação e filtros (volume de um escritório é pequeno)
- Dashboard/gráficos
- Autenticação (Fase 7)
- Export em Excel (só CSV por enquanto)
- Deploy do backend em produção — ver `GITHUB_SETUP.md` para o frontend (GitHub Pages)

---
*Atualizado: 2026-09-16*
