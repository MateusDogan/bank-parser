# Config

- `JpaAuditingConfig.java` (Fase 2) — ativa o preenchimento de `createdAt`/`updatedAt` em `BaseEntity`

O datasource, o JPA e o Flyway são configurados por propriedade em `application.yml`, sem classe
`@Configuration` — o autoconfigure do Spring Boot dá conta. `MinIOConfig` entra na Fase 3.
