package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.exception.BankStatementException;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class BankStatementServiceTest {

    @Mock
    private BankOperationRepository repository;

    @InjectMocks
    private BankStatementService service;

    @Test
    void shouldThrowExceptionWhenFromIsAfterTo() {
        LocalDateTime from = LocalDateTime.parse("2025-01-10T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2025-01-01T00:00:00");

        BankStatementException ex = assertThrows(
                BankStatementException.class,
                () -> service.calculateBalance("LT1", from, to)
        );

        assertTrue(ex.getMessage().contains("Invalid date range"));
    }
}