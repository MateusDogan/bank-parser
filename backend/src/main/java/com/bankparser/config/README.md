# Config

- `JpaAuditingConfig.java` (Fase 2) — ativa o preenchimento de `createdAt`/`updatedAt` em `BaseEntity`
- `MinIOConfig.java` + `MinIOProperties.java` (Fase 3) — bean `MinioClient` a partir do prefixo
  `minio` do `application.yml`

O datasource, o JPA e o Flyway são configurados por propriedade em `application.yml`, sem classe
`@Configuration` — o autoconfigure do Spring Boot dá conta.
