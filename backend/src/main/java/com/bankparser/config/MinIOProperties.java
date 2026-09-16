package com.bankparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinIOProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
}
