package com.bankparser.service;

import com.bankparser.entity.Statement;
import com.bankparser.entity.Transaction;
import com.bankparser.exception.ResourceNotFoundException;
import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
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
    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final StorageService storageService;
    private final BalanceValidationService validationService;

    public StatementProcessingService(List<BankStatementParser> parsers,
                                      StatementRepository statementRepository,
                                      TransactionRepository transactionRepository,
                                      StorageService storageService,
                                      BalanceValidationService validationService) {
        this.parsersByBankKey = parsers.stream()
                .collect(Collectors.toMap(BankStatementParser::bankKey, Function.identity()));
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.storageService = storageService;
        this.validationService = validationService;
    }

    @Transactional
    public Statement process(String bankKey, String originalFilename, byte[] content) {
        BankStatementParser parser = parsersByBankKey.get(bankKey);
        if (parser == null) {
            throw new ResourceNotFoundException("Banco sem parser disponivel: " + bankKey);
        }

        ParsingResult result = parser.parse(new ByteArrayInputStream(content));

        Statement statement = new Statement(bankKey, originalFilename);
        statement.setIssuedAt(result.metadata().emitidoEm());
        statement.setDocument(result.metadata().documento());
        statement.setTransactionCount(result.transactions().size());
        statement.setParserVersion(parser.parserVersion());

        List<Transaction> transactions = toTransactions(statement, result.transactions());
        BalanceValidationService.ValidationReport report = validationService.checkBalanceContinuity(transactions);
        statement.setValidationFlags(report.toStorageSummary());

        String objectKey = buildObjectKey(statement.getId(), originalFilename);
        storageService.upload(objectKey, new ByteArrayInputStream(content), content.length, PDF_CONTENT_TYPE);
        statement.setStorageKey(objectKey);

        statementRepository.save(statement);
        transactionRepository.saveAll(transactions);
        return statement;
    }

    @Transactional(readOnly = true)
    public Statement findById(UUID statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Extrato nao encontrado: " + statementId));
    }

    @Transactional(readOnly = true)
    public List<Statement> findAll() {
        return statementRepository.findAllByOrderByUploadedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ParsedTransaction> transactionsForExport(UUID statementId) {
        return transactionRepository.findByStatementIdOrderByLineNumber(statementId)
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

    private static String buildObjectKey(UUID statementId, String filename) {
        return "statements/%s/%s".formatted(statementId, filename);
    }
}
