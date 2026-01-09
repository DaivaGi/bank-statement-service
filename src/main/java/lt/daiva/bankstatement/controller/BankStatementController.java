package lt.daiva.bankstatement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.dto.ExportResult;
import lt.daiva.bankstatement.dto.ImportResult;
import lt.daiva.bankstatement.exception.BankStatementException;
import lt.daiva.bankstatement.service.BankStatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/statements")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    public BankStatementController(BankStatementService bankStatementService) {
        this.bankStatementService = bankStatementService;
    }

    /**
     * Imports bank operations from a CSV file.
     * Expected CSV header:
     * accountNumber,operationDateTime,beneficiary,comment,amount,currency
     * Returns the number of imported records and skipped duplicates.
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(
            summary = "Import bank statement from CSV",
            description = """
                    Expected CSV format:

                    accountNumber,operationDateTime,beneficiary,comment,amount,currency

                    Example:
                    LT100001,2025-01-05T12:10:00,Upwork	Freelance payment,200.00,EUR
                    """
    )
    public ResponseEntity<ImportResult> importCsv(@RequestPart("file") MultipartFile file) {
        if (!isCsv(file)) {
            throw new BankStatementException("Only CSV files are supported");
        }

        ImportResult result = bankStatementService.importFromCsv(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public BalanceResponse getBalance(
            @PathVariable
            @Parameter(description = "Account number", example = "LT100001")
            String accountNumber,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date", example = "2025-01-01")
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date", example = "2025-01-10")
            LocalDate to) {

        return bankStatementService.calculateBalance(accountNumber, from, to);
    }

    @GetMapping(value = "/export")
    @Operation(
            summary = "Export bank statement to CSV",
            description = "Exports operations for one or several accounts. Optional date range filters."
    )
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam
            @Parameter(description = "One or more account numbers", example = "LT100001")
            List<String> accounts,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date", example = "2025-01-01")
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date", example = "2025-01-10")
            LocalDate to
    ) {
        ExportResult result = bankStatementService.exportToCsv(accounts, from, to);

        String filename = "bank-statement-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".csv";

        return ResponseEntity.ok()
                .header("X-Total-Records", String.valueOf(result.totalRecords()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(result.csv());
    }

    private static boolean isCsv(MultipartFile file) {
        String name = Objects.toString(file.getOriginalFilename(), "");
        String contentType = Objects.toString(file.getContentType(), "");

        boolean csvByName = name.toLowerCase().endsWith(".csv");
        boolean csvByType =
                contentType.equals("text/csv") ||
                        contentType.equals("application/csv") ||
                        contentType.equals("application/vnd.ms-excel") ||
                        contentType.startsWith("text/");

        return csvByName || csvByType;
    }
}
