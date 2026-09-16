# Storage

**Fase 3 do DEVELOPMENT_PLAN.md — concluída.**

- `StorageService.java` — interface (`upload`/`download`/`delete`). **Nenhum tipo do MinIO aparece
  nas assinaturas**: é o que permite trocar por AWS S3 ou Cloudflare R2 escrevendo só uma
  implementação nova, sem tocar em serviço nem controller (decisão de arquitetura #2)
- `MinIOStorageService.java` — implementação S3-compatible
- `StorageException.java` — falha de gravação/leitura, tratada como **503** pelo `GlobalExceptionHandler`

Configuração em `config/MinIOConfig` + `MinIOProperties` (prefixo `minio` no `application.yml`).

Layout das chaves: `organizations/{orgId}/statements/{statementId}/{filename}` — a separação por
tenant fica visível também no storage, não só no banco.

O bucket é criado sob demanda no primeiro upload, e não no start da aplicação: assim a API sobe mesmo
com o storage fora do ar, e só o upload falha.

**Teste**: `MinIOStorageServiceTest` roda contra um endpoint S3 embarcado (`s3mock-junit5`, sem
Docker). Como o MinIO fala o protocolo S3, isso exercita a integração real. Vale repetir contra o
MinIO do `docker-compose` quando ele estiver disponível.
