package lt.daiva.bankstatement.dto;

import java.math.BigDecimal;

public record BalanceResponse(String accountNumber,
                              BigDecimal balance,
                              String currency) {
}
