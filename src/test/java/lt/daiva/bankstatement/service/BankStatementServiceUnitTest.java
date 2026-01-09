package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.dto.CurrencyBalance;
import lt.daiva.bankstatement.dto.ExportResult;
import lt.daiva.bankstatement.exception.BankStatementException;
import lt.daiva.bankstatement.model.BankOperation;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankStatementServiceUnitTest {

    @Mock
    private BankOperationRepository bankOperationRepository;

    @InjectMocks
    private BankStatementService bankStatementService;

    @Test
    void calculateBalance_shouldThrow_whenFromIsAfterTo() {
        LocalDate from = LocalDate.parse("2025-01-10");
        LocalDate to = LocalDate.parse("2025-01-01");

        BankStatementException ex = assertThrows(
                BankStatementException.class,
                () -> bankStatementService.calculateBalance("LT100001", from, to)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("invalid date range"));
        verifyNoInteractions(bankOperationRepository);
    }

    @Test
    void calculateBalance_shouldReturnBalancesByCurrency() {
        when(bankOperationRepository.calculateBalancesByCurrency(eq("LT100001"), any(), any()))
                .thenReturn(List.of(new CurrencyBalance("EUR", new BigDecimal("10.00"))));

        BalanceResponse response = bankStatementService.calculateBalance("LT100001", null, null);

        assertEquals("LT100001", response.accountNumber());

        assertEquals(1, response.balances().size());
        assertEquals("EUR", response.balances().get(0).currency());
        assertEquals(0, response.balances().get(0).amount().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void calculateBalance_shouldReturnEmptyList_whenNoOperationsFound() {
        when(bankOperationRepository.calculateBalancesByCurrency(eq("LT100001"), any(), any()))
                .thenReturn(List.of());

        BalanceResponse response = bankStatementService.calculateBalance("LT100001", null, null);

        assertEquals("LT100001", response.accountNumber());
        assertNotNull(response.balances());
        assertTrue(response.balances().isEmpty());
    }

    @Test
    void importFromCsv_shouldSkipDuplicatesAndReturnCounts() {
        String csv = """
                accountNumber,operationDateTime,beneficiary,comment,amount,currency
                LT100001,2025-01-01T09:15:00,Employer,January salary,1500.00,EUR
                LT100001,2025-01-03T18:40:00,Maxima,Groceries,85.32,EUR
                LT100001,2025-01-03T18:40:00,Maxima,Groceries,85.32,EUR
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.csv",
                "text/csv",
                csv.getBytes()
        );

        when(bankOperationRepository.save(any(BankOperation.class)))
                .thenReturn(null)
                .thenReturn(null)
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        var result = bankStatementService.importFromCsv(file);

        assertEquals(2, result.imported());
        assertEquals(1, result.skippedDuplicates());

        ArgumentCaptor<BankOperation> captor = ArgumentCaptor.forClass(BankOperation.class);
        verify(bankOperationRepository, times(3)).save(captor.capture());

        var ops = captor.getAllValues();
        assertEquals(3, ops.size());

        var op1 = ops.getFirst();
        assertEquals("LT100001", op1.getAccountNumber());
        assertEquals(LocalDateTime.parse("2025-01-01T09:15:00"), op1.getOperationTime());
        assertEquals("Employer", op1.getBeneficiary());
        assertEquals("January salary", op1.getOperationComment());
        assertEquals(0, op1.getAmount().compareTo(new BigDecimal("1500.00")));
        assertEquals("EUR", op1.getCurrency());
    }

    @Test
    void importFromCsv_shouldThrow_whenMissingRequiredHeader() {
        String csvMissingComment = """
                accountNumber,operationDateTime,beneficiary,amount,currency
                LT100001,2025-01-01T09:15:00,Employer,1500.00,EUR
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "missing-column.csv",
                "text/csv",
                csvMissingComment.getBytes()
        );

        BankStatementException ex = assertThrows(
                BankStatementException.class,
                () -> bankStatementService.importFromCsv(file)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("missing"));
        assertTrue(ex.getMessage().toLowerCase().contains("comment"));

        verifyNoInteractions(bankOperationRepository);
    }

    @Test
    void exportToCsv_shouldReturnCsvWithHeaderAndRows_andTotalCount() {
        List<BankOperation> ops = List.of(
                new BankOperation("LT100001", LocalDateTime.parse("2025-01-01T09:15:00"),
                        "Employer", "Salary", new BigDecimal("1500.00"), "EUR"),
                new BankOperation("LT100001", LocalDateTime.parse("2025-01-03T18:40:00"),
                        "Maxima", "Groceries", new BigDecimal("85.32"), "EUR")
        );

        when(bankOperationRepository.findForExport(anyList(), any(), any())).thenReturn(ops);

        ExportResult result = bankStatementService.exportToCsv(
                List.of("LT100001"),
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-01-31")
        );

        assertEquals(2, result.totalRecords());

        String csv = new String(result.csv(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("accountNumber,operationDateTime,beneficiary,comment,amount,currency"));
        assertTrue(csv.contains("LT100001"));
        assertTrue(csv.contains("Employer"));
        assertTrue(csv.contains("Maxima"));
    }
}