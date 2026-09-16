package com.bankparser.controller;

import com.bankparser.dto.ClientRequest;
import com.bankparser.dto.ClientResponse;
import com.bankparser.entity.Client;
import com.bankparser.exception.DuplicateClientException;
import com.bankparser.repository.ClientRepository;
import com.bankparser.service.CurrentOrganizationProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

        Client client = clientRepository.save(
                new Client(organizationId, request.name(), request.document()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    // Sem paginacao no MVP: volume de clientes de um escritorio e pequeno.
    @GetMapping
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository
                .findByOrganizationId(organizationProvider.currentOrganizationId(), Pageable.unpaged())
                .stream()
                .map(ClientResponse::from)
                .toList();
    }
}
