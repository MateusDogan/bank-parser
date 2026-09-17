package com.bankparser.controller;

import com.bankparser.entity.Client;
import com.bankparser.entity.Organization;
import com.bankparser.repository.ClientRepository;
import com.bankparser.repository.OrganizationRepository;
import com.bankparser.repository.StatementRepository;
import com.bankparser.repository.TransactionRepository;
import com.bankparser.storage.StorageService;
import com.bankparser.testsupport.InMemoryStorageService;
import com.bankparser.testsupport.SyntheticStatementPdf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: upload, persistence e export against real embedded Postgres.
 * {@link StorageService} uses in-memory impl (MinIO requires Docker).
 */
@Import(StatementControllerIntegrationTest.StubStorage.class)
class StatementControllerIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class StubStorage {
        @Bean
        @Primary
        StorageService inMemoryStorageService() {
            return new InMemoryStorageService();
        }
    }

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private StatementRepository statementRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private StorageService storageService;

    private Client client;

    @BeforeEach
    void resetData() {
        // Sem @Transactional no teste: as asserções precisam enxergar o que a
        // requisicao de fato commitou (e o que ela NAO commitou, no caso de erro).
        jdbcTemplate.execute("TRUNCATE transactions, statements, clients CASCADE");
        ((InMemoryStorageService) storageService).clear();

        client = clientRepository.save(new Client(
                Organization.DEFAULT_ID, "Cliente Teste", SyntheticStatementPdf.DOCUMENT_DIGITS));
    }

    @Test
    void uploadPersistsStatementTransactionsAndFile() throws Exception {
        mockMvc.perform(uploadOf(SyntheticStatementPdf.withTwoTransactions(), client.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.bankKey").value("stone"))
                .andExpect(jsonPath("$.clientName").value("Cliente Teste"))
                .andExpect(jsonPath("$.issuedAt").value("2026-08-27"));

        assertThat(statementRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(((InMemoryStorageService) storageService).keys()).hasSize(1);
    }

    @Test
    void uploadRecordsParserVersionAndBalanceCheck() throws Exception {
        // Os valores do PDF sintetico foram escolhidos para exercitar a extracao,
        // nao para fechar contabilmente: 32321.29 + (-300.00) nao da 2321.30, e a
        // checagem de saldo tem que acusar exatamente a linha 0 por isso.
        mockMvc.perform(uploadOf(SyntheticStatementPdf.withTwoTransactions(), client.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parserVersion").value("1.0"))
                .andExpect(jsonPath("$.validationFlags")
                        .value("1 divergencia(s) de saldo nas linhas: 0"));
    }

    @Test
    void exportReturnsCsvInPdfOrder() throws Exception {
        UUID statementId = uploadAndGetId();

        MvcResult result = mockMvc.perform(get("/api/statements/{id}/export", statementId)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv.lines()).containsExactly(
                "data;tipo;valor;saldo;descricao;detalhe",
                "26/08/2026;Saída;-300.00;2321.30;CLIENTE TESTE LTDA;Transferência | Pix",
                "17/08/2026;Entrada;485.70;32321.29;;Visa | Crédito");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("extrato.csv");
    }

    @Test
    void findByIdReturnsMetadata() throws Exception {
        UUID statementId = uploadAndGetId();

        mockMvc.perform(get("/api/statements/{id}", statementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(statementId.toString()))
                .andExpect(jsonPath("$.document").value(SyntheticStatementPdf.DOCUMENT));
    }

    @Test
    void findAllListsStatementsFromCurrentOrganization() throws Exception {
        uploadAndGetId();

        mockMvc.perform(get("/api/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientName").value("Cliente Teste"));
    }

    @Test
    void findAllReturnsMostRecentUploadFirst() throws Exception {
        byte[] pdf = SyntheticStatementPdf.withTwoTransactions();
        mockMvc.perform(uploadOf(pdf, client.getId(), "primeiro.pdf")).andExpect(status().isCreated());
        mockMvc.perform(uploadOf(pdf, client.getId(), "segundo.pdf")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFilename").value("segundo.pdf"))
                .andExpect(jsonPath("$[1].originalFilename").value("primeiro.pdf"));
    }

    @Test
    void rejectsInvalidPdfsWithoutPersistingAnything() throws Exception {
        // Illegible amount in valid PDF structure
        mockMvc.perform(uploadOf(SyntheticStatementPdf.withIllegibleAmount(), client.getId()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(statementRepository.count()).isZero();

        // Completely unreadable PDF
        mockMvc.perform(uploadOf("not a pdf".getBytes(StandardCharsets.UTF_8), client.getId()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(statementRepository.count()).isZero();
    }

    @Test
    void rejectsStatementBelongingToAnotherDocument() throws Exception {
        Client outro = clientRepository.save(
                new Client(Organization.DEFAULT_ID, "Outro Cliente", "99999999999999"));

        mockMvc.perform(uploadOf(SyntheticStatementPdf.withTwoTransactions(), outro.getId()))
                .andExpect(status().isConflict());

        assertThat(statementRepository.count()).isZero();
    }

    @Test
    void doesNotAcceptClientFromAnotherOrganization() throws Exception {
        Organization other = organizationRepository.save(new Organization("Outro Escritorio"));
        Client foreign = clientRepository.save(
                new Client(other.getId(), "Cliente de Fora", SyntheticStatementPdf.DOCUMENT_DIGITS));

        mockMvc.perform(uploadOf(SyntheticStatementPdf.withTwoTransactions(), foreign.getId()))
                .andExpect(status().isNotFound());
    }

    private UUID uploadAndGetId() throws Exception {
        MvcResult result = mockMvc
                .perform(uploadOf(SyntheticStatementPdf.withTwoTransactions(), client.getId()))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.id"));
    }

    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            uploadOf(byte[] pdf, UUID clientId) {
        return uploadOf(pdf, clientId, "extrato.pdf");
    }

    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            uploadOf(byte[] pdf, UUID clientId, String filename) {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, MediaType.APPLICATION_PDF_VALUE, pdf);
        var builder = multipart("/api/statements/upload");
        builder.file(file);
        builder.param("clientId", clientId.toString());
        return builder;
    }
}
