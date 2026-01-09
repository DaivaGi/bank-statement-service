package lt.daiva.bankstatement.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.text.MessageFormat;

@Hidden
@RestControllerAdvice(basePackages = "lt.daiva.bankstatement.controller")
public class GlobalExceptionHandler {
    @ExceptionHandler(BankStatementException.class)
    public ResponseEntity<ApiError> handleBankStatement(BankStatementException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String param = ex.getName();
        ex.getValue();
        String value = ex.getValue().toString();

        String message = MessageFormat.format(
                "Invalid value for ''{0}'': {1}. Expected format: {2}",
                param,
                value,
                "yyyy-MM-dd");

        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_PARAMETER", message));
    }

    @ExceptionHandler(InvalidCsvRecordException.class)
    public ResponseEntity<ApiError> handleInvalidCsvRecord(InvalidCsvRecordException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_CSV_RECORD", e.getMessage()));
    }
}
