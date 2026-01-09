package lt.daiva.bankstatement.exception;

public class InvalidCsvRecordException extends RuntimeException {
    public InvalidCsvRecordException(String message) {
        super(message);
    }
}