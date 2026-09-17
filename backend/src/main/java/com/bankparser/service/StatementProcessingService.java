package com.bankparser.service;

import com.bankparser.entity.Client;
import com.bankparser.entity.Statement;
import com.bankparser.entity.Transaction;
import com.bankparser.exception.DocumentMismatchException;
import com.bankparser.exception.ResourceNotFoundException;
import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import com.bankparser.repository.ClientRepository;
import com.bankparser.repository.StatementRepository;
import com.bankparser.repository.TransactionRepository;
import com.bankparser.storage.StorageService;
import com.bankparser.validation.BalanceValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatementProcessingService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final Map<String, BankStatementParser> parsersByBankKey;
    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final StorageService storageService;
    private final BalanceValidationService validationService;

    public StatementProcessingService(List<BankStatementParser> parsers,
                                      ClientRepository clientRepository,
                                      StatementRepository statementRepository,
                                      TransactionRepository transactionRepository,
                                      StorageService storageService,
                                      BalanceValidationService validationService) {
        this.parsersByBankKey = parsers.stream()
                .collect(Collectors.toMap(BankStatementParser::bankKey, Function.identity()));
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.storageService = storageService;
        this.validationService = validationService;
    }

    @Transactional
    public Statement process(UUID organizationId, UUID clientId, String bankKey,
                             String originalFilename, byte[] content) {
        Client client = clientRepository.findByOrganizationIdAndId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + clientId));

        BankStatementParser parser = parsersByBankKey.get(bankKey);
        if (parser == null) {
            throw new ResourceNotFoundException("Banco sem parser disponivel: " + bankKey);
        }

        ParsingResult result = parser.parse(new ByteArrayInputStream(content));
        assertDocumentMatchesClient(result, client);

        Statement statement = new Statement(organizationId, client, bankKey, originalFilename);
        statement.setIssuedAt(result.metadata().emitidoEm());
        statement.setDocument(result.metadata().documento());
        statement.setTransactionCount(result.transactions().size());
        statement.setParserVersion(parser.parserVersion());

        List<Transaction> transactions = toTransactions(statement, result.transactions());
        BalanceValidationService.ValidationReport report = validationService.checkBalanceContinuity(transactions);
        statement.setValidationFlags(report.toStorageSummary());

        String objectKey = buildObjectKey(organizationId, statement.getId(), originalFilename);
        storageService.upload(objectKey, new ByteArrayInputStream(content), content.length, PDF_CONTENT_TYPE);
        statement.setStorageKey(objectKey);

        statementRepository.save(statement);
        transactionRepository.saveAll(transactions);
        return statement;
    }

    @Transactional(readOnly = true)
    public Statement findById(UUID organizationId, UUID statementId) {
        return statementRepository.findByOrganizationIdAndId(organizationId, statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Extrato nao encontrado: " + statementId));
    }

    @Transactional(readOnly = true)
    public List<Statement> findAll(UUID organizationId) {
        return statementRepository.findByOrganizationIdOrderByUploadedAtDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<ParsedTransaction> transactionsForExport(UUID organizationId, UUID statementId) {
        return transactionRepository
                .findByOrganizationIdAndStatementIdOrderByLineNumber(organizationId, statementId)
                .stream()
                .map(tx -> new ParsedTransaction(
                        tx.getTransactionDate(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getBalance(),
                        tx.getDescription(),
                        tx.getDetail()))
                .toList();
    }

    private void assertDocumentMatchesClient(ParsingResult result, Client client) {
        String fromPdf = Client.normalizeDocument(result.metadata().documento());
        // Documento ausente nao invalida o extrato: o cabecalho varia de layout
        // e nao da para conferir o que o parser nao achou.
        if (fromPdf == null || fromPdf.isBlank()) {
            return;
        }
        if (!fromPdf.equals(client.getDocument())) {
            throw new DocumentMismatchException(
                    "O documento do extrato (" + fromPdf + ") nao e o do cliente "
                            + client.getName() + " (" + client.getDocument() + ")");
        }
    }

    private List<Transaction> toTransactions(Statement statement, List<ParsedTransaction> parsed) {
        List<Transaction> transactions = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ParsedTransaction source = parsed.get(i);
            Transaction transaction = new Transaction(statement, i);
            transaction.setTransactionDate(source.data());
            transaction.setType(source.tipo());
            transaction.setAmount(source.valor());
            transaction.setBalance(source.saldo());
            transaction.setDescription(source.descricao());
            transaction.setDetail(source.detalhe());
            transactions.add(transaction);
        }
        return transactions;
    }

    private static String buildObjectKey(UUID organizationId, UUID statementId, String filename) {
        return "organizations/%s/statements/%s/%s".formatted(organizationId, statementId, filename);
    }
}
