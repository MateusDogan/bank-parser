package com.bankparser.controller;

import com.bankparser.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: create and list clients against real embedded Postgres.
 */
class ClientControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ClientRepository clientRepository;

    @BeforeEach
    void resetData() {
        jdbcTemplate.execute("TRUNCATE transactions, statements, clients CASCADE");
    }

    @Test
    void createsClientWithNormalizedDocument() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Teste", "document": "12.345.678/0001-99"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cliente Teste"))
                .andExpect(jsonPath("$.document").value("12345678000199"));

        assertThat(clientRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateDocumentInSameOrganization() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Um", "document": "12345678000199"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Dois", "document": "12345678000199"}
                                """))
                .andExpect(status().isConflict());

        assertThat(clientRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "document": "12345678000199"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(clientRepository.count()).isZero();
    }

    @Test
    void listsAllClientsInOrganization() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente A", "document": "11111111000191"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente B", "document": "22222222000192"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
