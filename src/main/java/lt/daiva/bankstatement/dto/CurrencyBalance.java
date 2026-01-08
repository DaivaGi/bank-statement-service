package lt.daiva.bankstatement.dto;

import java.math.BigDecimal;

public record CurrencyBalance(String currency, BigDecimal amount) {}
