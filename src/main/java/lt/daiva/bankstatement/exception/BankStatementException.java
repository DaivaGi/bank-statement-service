package lt.daiva.bankstatement.exception;

public class BankStatementException extends RuntimeException {
    public BankStatementException(String message) {
        super(message);
    }

    public static BankStatementException invalidDateRange() {
        return new BankStatementException("Invalid date range: 'from' is after 'to'");
    }

    public static BankStatementException missingRequiredColumn(String column) {
        return new BankStatementException("Missing required column: " + column);
    }
}
