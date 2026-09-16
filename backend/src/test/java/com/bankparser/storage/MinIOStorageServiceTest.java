package com.bankparser.storage;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.bankparser.config.MinIOProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercita o {@link MinIOStorageService} contra um endpoint S3 embarcado.
 *
 * <p>O MinIO fala o protocolo S3, entao isso cobre o que um mock do
 * {@code MinioClient} nao cobriria: bucket criado sob demanda, chave gravada e
 * bytes recuperados iguais aos enviados. Sem Docker — quando o
 * {@code docker-compose} subir, vale repetir contra o MinIO real.
 */
class MinIOStorageServiceTest {

    // O S3Mock sobe uma aplicacao Spring Boot propria no mesmo classpath, entao
    // os starters de JPA/Flyway se autoconfiguram nela e tentam abrir conexao
    // com o Postgres. Excluir essas autoconfiguracoes basta — nao da para
    // trocar o spring.config.name, porque o S3Mock depende do proprio
    // application.properties para as configuracoes dele.
    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder()
            .withSecureConnection(false)
            .withProperty("spring.autoconfigure.exclude", String.join(",",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                    "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"))
            .build();

    private static final String BUCKET = "bank-statements-test";

    private MinIOStorageService storage;

    @BeforeEach
    void setUp() {
        MinIOProperties properties = new MinIOProperties(
                "http://localhost:" + S3_MOCK.getHttpPort(), "minioadmin", "minioadmin123", BUCKET);
        MinioClient client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        storage = new MinIOStorageService(client, properties);
    }

    @Test
    void uploadsAndDownloadsTheSameBytes() throws Exception {
        byte[] content = "conteudo do extrato".getBytes(StandardCharsets.UTF_8);
        String key = "organizations/org-1/statements/st-1/extrato.pdf";

        storage.upload(key, new ByteArrayInputStream(content), content.length, "application/pdf");

        try (InputStream downloaded = storage.download(key)) {
            assertThat(downloaded.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void createsTheBucketOnFirstUpload() {
        byte[] content = "primeiro".getBytes(StandardCharsets.UTF_8);

        // O bucket nao existe antes desta chamada; se nao fosse criado sob
        // demanda, o upload falharia.
        storage.upload("chave-inicial", new ByteArrayInputStream(content), content.length, "text/plain");

        assertThat(storage.download("chave-inicial")).isNotNull();
    }

    @Test
    void deleteRemovesTheObject() {
        byte[] content = "temporario".getBytes(StandardCharsets.UTF_8);
        storage.upload("descartavel", new ByteArrayInputStream(content), content.length, "text/plain");

        storage.delete("descartavel");

        assertThatThrownBy(() -> storage.download("descartavel"))
                .isInstanceOf(StorageException.class);
    }
}
