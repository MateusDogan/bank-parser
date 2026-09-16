package com.bankparser.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitarios de parsing reutilizaveis por diferentes implementacoes de
 * {@link BankStatementParser}: valores monetarios brasileiros e datas.
 */
public final class TransactionExtractor {

    private static final Pattern MONEY_PATTERN = Pattern.compile("([\\d.]+,\\d{2})");

    private static final Map<String, Integer> MESES_PT = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("fevereiro", 2), Map.entry("março", 3),
            Map.entry("abril", 4), Map.entry("maio", 5), Map.entry("junho", 6),
            Map.entry("julho", 7), Map.entry("agosto", 8), Map.entry("setembro", 9),
            Map.entry("outubro", 10), Map.entry("novembro", 11), Map.entry("dezembro", 12)
    );

    private TransactionExtractor() {
    }

    /**
     * Converte texto de valor monetario brasileiro em BigDecimal com sinal.
     * Aceita formatos como {@code "R$ 1.234,56"} ou {@code "- R$ 1.234,56"}.
     *
     * @return o valor, ou {@code null} se nenhum numero reconhecivel for encontrado
     */
    public static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = MONEY_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        boolean negative = text.contains("-");
        String normalized = matcher.group(1).replace(".", "").replace(",", ".");
        BigDecimal value = new BigDecimal(normalized);
        return negative ? value.negate() : value;
    }

    /** Converte data no formato dd/MM/yy (ano assumido no seculo 2000+) para LocalDate. */
    public static LocalDate parseDateDDMMYY(String day, String month, String yearTwoDigits) {
        int year = 2000 + Integer.parseInt(yearTwoDigits);
        return LocalDate.of(year, Integer.parseInt(month), Integer.parseInt(day));
    }

    /** Numero do mes (1-12) a partir do nome em portugues, ou {@code null} se nao reconhecido. */
    public static Integer monthFromPortugueseName(String monthName) {
        if (monthName == null) {
            return null;
        }
        return MESES_PT.get(monthName.toLowerCase(new Locale("pt", "BR")));
    }
}
