package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.dto.ExportResult;
import lt.daiva.bankstatement.dto.ImportResult;
import lt.daiva.bankstatement.exception.BankStatementException;
import lt.daiva.bankstatement.exception.InvalidCsvRecordException;
import lt.daiva.bankstatement.model.BankOperation;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class BankStatementService {

    private final BankOperationRepository bankOperationRepository;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "accountNumber", "operationDateTime", "beneficiary", "comment", "amount", "currency"
    );
    private static final CSVFormat IMPORT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();
    private static final LocalTime START_OF_DAY = LocalTime.MIN;
    private static final LocalTime END_OF_DAY = LocalTime.MAX;

    public BankStatementService(BankOperationRepository bankOperationRepository) {
        this.bankOperationRepository = bankOperationRepository;
    }

    /**
     * Parses CSV file, validates required headers and values,
     * skips duplicate operations and persists valid records.
     *
     * @param file CSV file with bank operations
     * @return result containing number of imported and skipped records
     */
    public ImportResult importFromCsv(MultipartFile file) {
        int imported = 0;
        int skipped = 0;

        try (var reader = new InputStreamReader(file.getInputStream())) {

            try (CSVParser parser = IMPORT_FORMAT.parse(reader)) {
                validateHeaders(parser);

                for (CSVRecord record : parser) {
                    try {
                        BankOperation operation = toOperation(record);
                        bankOperationRepository.save(operation);
                        imported++;

                    } catch (DataIntegrityViolationException e) {
                        if (isDuplicateKeyViolation(e)) {
                            skipped++;
                        } else {
                            throw e;
                        }
                    }
                }

                return new ImportResult(imported, skipped);
            }
        } catch (IllegalArgumentException e) {
            throw new BankStatementException("Invalid CSV format: missing required header or invalid file content");
        } catch (IOException e) {
            throw new BankStatementException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    /**
     * Calculates account balance for a given date range.
     *
     * @param accountNumber account identifier
     * @param from          optional start date (inclusive)
     * @param to            optional end date (inclusive)
     * @return balance grouped by currency
     */
    public BalanceResponse calculateBalance(String accountNumber,
                                            LocalDate from,
                                            LocalDate to) {
        validateDateRange(from, to);

        LocalDateTime fromDatetime = (from == null) ? null : from.atTime(START_OF_DAY);
        LocalDateTime toDatetime = (to == null) ? null : to.atTime(END_OF_DAY);

        var balances = bankOperationRepository.calculateBalancesByCurrency(accountNumber, fromDatetime, toDatetime);

        return new BalanceResponse(accountNumber, balances);
    }

    /**
     * Exports bank operations for one or several accounts.
     * Date filters are provided as LocalDate and converted internally
     * to day boundaries (start/end of day).
     *
     * @param accounts list of account numbers to export
     * @param from     optional start date (inclusive)
     * @param to       optional end date (inclusive)
     * @return CSV file content and metadata
     */
    public ExportResult exportToCsv(List<String> accounts, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        LocalDateTime fromDatetime = (from == null) ? null : from.atTime(START_OF_DAY);
        LocalDateTime toDatetime = (to == null) ? null : to.atTime(END_OF_DAY);

        var operations = bankOperationRepository.findForExport(accounts, fromDatetime, toDatetime);
        var csv = generateCsv(operations);

        return new ExportResult(csv, operations.size());
    }

    private BankOperation toOperation(CSVRecord record) {
        try {
            var operationComment = record.get("comment");
            if (operationComment == null) {
                operationComment = "";
            }
            return new BankOperation(
                    record.get("accountNumber"),
                    LocalDateTime.parse(record.get("operationDateTime")),
                    record.get("beneficiary"),
                    operationComment,
                    new BigDecimal(record.get("amount")),
                    record.get("currency").trim().toUpperCase()
            );

        } catch (DateTimeParseException e) {
            throw new InvalidCsvRecordException(
                    "Invalid date format for operationDateTime: " + record.get("operationDateTime")
            );
        } catch (NumberFormatException e) {
            throw new InvalidCsvRecordException(
                    "Invalid amount: " + record.get("amount")
            );
        }
    }

    private byte[] generateCsv(List<BankOperation> operations) {
        try (var out = new ByteArrayOutputStream();
             var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("accountNumber", "operationDateTime", "beneficiary", "comment", "amount", "currency")
                     .build())) {

            for (var operation : operations) {
                printer.printRecord(
                        operation.getAccountNumber(),
                        operation.getOperationTime(),
                        operation.getBeneficiary(),
                        operation.getOperationComment(),
                        operation.getAmount(),
                        operation.getCurrency()
                );
            }

            printer.flush();
            return out.toByteArray();

        } catch (IOException e) {
            throw new BankStatementException("Failed to export CSV: " + e.getMessage());
        }
    }

    /**
     * Returns operations for export filtered by accounts and date range.
     * Used by CSV export and balance calculation.
     */
    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw BankStatementException.invalidDateRange();
        }
    }

    private void validateHeaders(CSVParser parser) {
        var headerMap = parser.getHeaderMap();
        for (String header : REQUIRED_HEADERS) {
            if (!headerMap.containsKey(header)) {
                throw BankStatementException.missingRequiredColumn(header);
            }
        }
    }

    private Boolean isDuplicateKeyViolation(DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
        return msg.contains("uq_bank_operation_unique");
    }
}
