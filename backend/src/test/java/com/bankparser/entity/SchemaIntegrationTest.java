package com.bankparser.entity;

import com.bankparser.parser.dto.TransactionType;
import com.bankparser.repository.ClientRepository;
import com.bankparser.repository.OrganizationRepository;
import com.bankparser.repository.StatementRepository;
import com.bankparser.repository.TransactionRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
 * sem Docker) para validar a Fase 2.
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

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private StatementRepository statementRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsCreateEveryTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains("organizations", "users", "clients", "statements", "transactions");
    }

    @Test
    void seedsTheDefaultOrganization() {
        assertThat(organizationRepository.findById(Organization.DEFAULT_ID))
                .isPresent()
                .get()
                .extracting(Organization::getName)
                .isEqualTo("Escritorio");
    }

    @Test
    void persistsAndReadsBackTheFullStatementGraph() {
        Client client = clientRepository.save(
                new Client(Organization.DEFAULT_ID, "Clinica Teste LTDA", "59.193.001/0001-42"));

        Statement statement = new Statement(
                Organization.DEFAULT_ID, client, "stone", "extrato-agosto.pdf");
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

        List<Transaction> found = transactionRepository
                .findByOrganizationIdAndStatementIdOrderByLineNumber(
                        Organization.DEFAULT_ID, statement.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAmount()).isEqualByComparingTo("-300.00");
        assertThat(found.get(0).getStatement().getClient().getName()).isEqualTo("Clinica Teste LTDA");
    }

    @Test
    void storesDocumentWithoutMask() {
        Client client = clientRepository.save(
                new Client(Organization.DEFAULT_ID, "Clinica Teste LTDA", "59.193.001/0001-42"));

        assertThat(client.getDocument()).isEqualTo("59193001000142");
        assertThat(clientRepository.findByOrganizationIdAndDocument(
                Organization.DEFAULT_ID, "59193001000142")).isPresent();
    }

    @Test
    void hidesDataFromOtherOrganizations() {
        Organization other = organizationRepository.save(new Organization("Outro Escritorio"));
        clientRepository.save(new Client(Organization.DEFAULT_ID, "Cliente A", "11111111111111"));
        clientRepository.save(new Client(other.getId(), "Cliente B", "22222222222222"));

        assertThat(clientRepository.findByOrganizationId(other.getId(), PageRequest.of(0, 10)))
                .extracting(Client::getName)
                .containsExactly("Cliente B");
    }

    @Test
    void softDeleteHidesTheRowButKeepsIt() {
        Client client = clientRepository.save(
                new Client(Organization.DEFAULT_ID, "Cliente Removido", "33333333333333"));
        UUID id = client.getId();

        clientRepository.delete(client);
        clientRepository.flush();

        assertThat(clientRepository.findByOrganizationIdAndId(Organization.DEFAULT_ID, id)).isEmpty();

        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM clients WHERE id = ?", Boolean.class, id);
        assertThat(deleted).isTrue();
    }

    @Test
    void allowsReusingTheDocumentOfADeletedClient() {
        Client first = clientRepository.save(
                new Client(Organization.DEFAULT_ID, "Cliente Antigo", "44444444444444"));
        clientRepository.delete(first);
        clientRepository.flush();

        Client second = clientRepository.saveAndFlush(
                new Client(Organization.DEFAULT_ID, "Cliente Novo", "44444444444444"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    @Test
    void fillsAuditTimestamps() {
        Client client = clientRepository.saveAndFlush(
                new Client(Organization.DEFAULT_ID, "Cliente Datado", "55555555555555"));

        assertThat(client.getCreatedAt()).isNotNull();
        assertThat(client.getUpdatedAt()).isNotNull();
    }
}
