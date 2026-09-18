package com.bankparser.entity;

import com.bankparser.parser.dto.TransactionType;
import com.bankparser.repository.StatementRepository;
import com.bankparser.repository.TransactionRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o contexto Spring inteiro contra um Postgres real (binario embarcado,
 * sem Docker).
 *
 * <p>O teste mais importante e o proprio boot: com
 * {@code spring.jpa.hibernate.ddl-auto=validate}, o contexto so sobe se cada
 * campo das entidades bater com a coluna criada pelas migrations do Flyway.
 * Qualquer divergencia entre o Java e o SQL quebra aqui, e nao em producao.
 */
@SpringBootTest
@Transactional
class SchemaIntegrationTest {

    // Inicializado no load da classe: @DynamicPropertySource roda antes de @BeforeAll.
    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://localhost:" + POSTGRES.getPort() + "/postgres");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired private StatementRepository statementRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsCreateEveryTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains("statements", "transactions");
    }

    @Test
    void persistsAndReadsBackTheFullStatementGraph() {
        Statement statement = new Statement("stone", "extrato-agosto.pdf");
        statement.setIssuedAt(LocalDate.of(2026, 8, 27));
        statement.setTransactionCount(1);
        statementRepository.save(statement);

        Transaction transaction = new Transaction(statement, 0);
        transaction.setTransactionDate(LocalDate.of(2026, 8, 26));
        transaction.setType(TransactionType.SAIDA);
        transaction.setAmount(new BigDecimal("-300.00"));
        transaction.setBalance(new BigDecimal("2321.30"));
        transaction.setDescription("CLIENTE TESTE LTDA");
        transaction.setDetail("Transferencia | Pix");
        transactionRepository.save(transaction);

        List<Transaction> found =
                transactionRepository.findByStatementIdOrderByLineNumber(statement.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAmount()).isEqualByComparingTo("-300.00");
        assertThat(found.get(0).getStatement().getOriginalFilename()).isEqualTo("extrato-agosto.pdf");
    }

    @Test
    void softDeleteHidesTheRowButKeepsIt() {
        Statement statement = statementRepository.save(new Statement("stone", "removido.pdf"));
        UUID id = statement.getId();

        statementRepository.delete(statement);
        statementRepository.flush();

        assertThat(statementRepository.findById(id)).isEmpty();

        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM statements WHERE id = ?", Boolean.class, id);
        assertThat(deleted).isTrue();
    }

    @Test
    void fillsAuditTimestamps() {
        Statement statement = statementRepository.saveAndFlush(new Statement("stone", "datado.pdf"));

        assertThat(statement.getCreatedAt()).isNotNull();
        assertThat(statement.getUpdatedAt()).isNotNull();
    }
}
