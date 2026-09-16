# Storage

**Fase 3 do DEVELOPMENT_PLAN.md** — implementar aqui.

- `StorageService.java` — interface abstrata (upload, download, delete)
- `MinIOStorageService.java` — implementação usando MinIO (S3-compatible)

Mantenha a interface desacoplada de detalhes do MinIO para permitir trocar por AWS S3/Cloudflare R2 no futuro sem alterar código de negócio (apenas nova implementação + config).
