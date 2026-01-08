package lt.daiva.bankstatement.dto;

import java.util.List;

public record BalanceResponse(String accountNumber, List<CurrencyBalance> balances) {
}
