package lt.daiva.bankstatement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lt.daiva.bankstatement.dto.BalanceResponse;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statements")
public class BankStatementController {

    private final BankStatementService service;

    public BankStatementController(BankStatementService service) {
        this.service = service;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(
            summary = "Import bank statement from CSV",
            description = """
                    Expected CSV format:

                    accountNumber,operationDateTime,beneficiary,comment,amount,currency

                    Example:
                    LT100001,2025-01-01T09:15:00,Employer,January salary,1500.00,EUR
                    """
    )
    public ResponseEntity<Map<String, Integer>> importCsv(@RequestPart("file") MultipartFile file) {
        int imported = service.importFromCsv(file);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public BalanceResponse getBalance(
            @PathVariable String accountNumber,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(
                    description = "Start date-time in ISO format",
                    example = "2025-01-02T00:00:00")
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(
                    description = "End date-time in ISO format",
                    example = "2025-01-05T23:59:59")
            LocalDateTime to) {
        return service.calculateBalance(accountNumber, from, to);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(
            summary = "Export bank statement to CSV",
            description = "Exports operations for one or several accounts. Optional date range filters are ISO date-time."
    )
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam
            @Parameter(description = "One or more account numbers", example = "LT100001")
            List<String> accounts,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date-time (ISO)", example = "2025-01-02T00:00:00")
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date-time (ISO)", example = "2025-01-05T23:59:59")
            LocalDateTime to
    ) {
        byte[] csv = service.exportToCsv(accounts, from, to);

        String filename = "bank-statement-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
