package com.bankparser.validation;

import com.bankparser.entity.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida se o saldo de cada transacao e consistente com a proxima (ou seja,
 * se saldo[i] == saldo[i+1] + valor[i]). Nao bloqueia o upload — alguns
 * extratos reais quebram este invariante por particularidades do layout
 * (tarifa mal atribuida, por exemplo) que nao sao motivo pra rejeitar.
 *
 * <p>Armazena uma lista de linhas com divergencia, que pode ser recuperada
 * para auditoria posterior.
 */
@Service
public class BalanceValidationService {

    public ValidationReport checkBalanceContinuity(List<Transaction> transactions) {
        List<Integer> divergences = new ArrayList<>();

        // Extratos vem do mais recente pro mais antigo. Conferir se cada saldo
        // "vem" do anterior via saldo[i] == saldo[i+1] + valor[i].
        for (int i = 0; i < transactions.size() - 1; i++) {
            Transaction current = transactions.get(i);
            Transaction next = transactions.get(i + 1);

            if (current.getBalance() == null || next.getBalance() == null || current.getAmount() == null) {
                continue; // Nao consegue validar se faltam dados
            }

            BigDecimal expectedBalance = next.getBalance().add(current.getAmount());
            if (current.getBalance().compareTo(expectedBalance) != 0) {
                divergences.add(current.getLineNumber());
            }
        }

        return new ValidationReport(divergences);
    }

    public record ValidationReport(List<Integer> divergentLines) {
        public String toStorageSummary() {
            if (divergentLines.isEmpty()) {
                return null;
            }
            return divergentLines.size() + " divergencia(s) de saldo nas linhas: "
                    + divergentLines.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
        }
    }
}
