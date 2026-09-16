package com.bankparser.controller;

import com.bankparser.dto.ClientRequest;
import com.bankparser.dto.ClientResponse;
import com.bankparser.entity.Client;
import com.bankparser.exception.DuplicateClientException;
import com.bankparser.repository.ClientRepository;
import com.bankparser.service.CurrentOrganizationProvider;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;
    private final CurrentOrganizationProvider organizationProvider;

    public ClientController(ClientRepository clientRepository,
                            CurrentOrganizationProvider organizationProvider) {
        this.clientRepository = clientRepository;
        this.organizationProvider = organizationProvider;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        UUID organizationId = organizationProvider.currentOrganizationId();
        String normalizedDocument = Client.normalizeDocument(request.document());

        if (clientRepository.existsByOrganizationIdAndDocument(organizationId, normalizedDocument)) {
            throw new DuplicateClientException(
                    "Ja existe um cliente cadastrado com o documento " + normalizedDocument);
        }

        try {
            // O check acima nao e atomico: entre ele e o insert, outra requisicao
            // pode cadastrar o mesmo documento. Quem decide de fato e o indice
            // unico parcial (uq_clients_org_document), e a violacao dele e a
            // mesma situacao de negocio — responder 409, nao 500.
            Client client = clientRepository.saveAndFlush(
                    new Client(organizationId, request.name(), request.document()));
            return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateClientException(
                    "Ja existe um cliente cadastrado com o documento " + normalizedDocument);
        }
    }

    // Sem paginacao no MVP: volume de clientes de um escritorio e pequeno.
    @GetMapping
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository
                .findByOrganizationIdOrderByNameAsc(organizationProvider.currentOrganizationId())
                .stream()
                .map(ClientResponse::from)
                .toList();
    }
}
