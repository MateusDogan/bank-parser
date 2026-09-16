package com.bankparser.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Ativa o preenchimento automatico de {@code createdAt}/{@code updatedAt}. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
