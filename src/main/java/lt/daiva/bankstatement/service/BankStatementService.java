package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.dto.CurrencyBalance;
import lt.daiva.bankstatement.dto.ExportResult;
import lt.daiva.bankstatement.exception.BankStatementException;
import lt.daiva.bankstatement.model.BankOperation;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankStatementService {

    private final BankOperationRepository bankOperationRepository;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "accountNumber", "operationDateTime", "beneficiary", "comment", "amount", "currency"
    );

    public BankStatementService(BankOperationRepository bankOperationRepository) {
        this.bankOperationRepository = bankOperationRepository;
    }

    /**
     * Imports bank operations from CSV file.
     * Expected header:
     * accountNumber,operationDateTime,beneficiary,comment,amount,currency
     */
    public int importFromCsv(MultipartFile file) {
        try (var reader = new InputStreamReader(file.getInputStream())) {

            var format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = format.parse(reader)) {
                validateHeaders(parser);

                List<BankOperation> operations = new ArrayList<>();

                for (CSVRecord record : parser) {
                    BankOperation operation = new BankOperation(
                            record.get("accountNumber"),
                            LocalDateTime.parse(record.get("operationDateTime")),
                            record.get("beneficiary"),
                            record.get("comment"),
                            new BigDecimal(record.get("amount")),
                            record.get("currency").toUpperCase()
                    );
                    operations.add(operation);
                }
                bankOperationRepository.saveAll(operations);
                return operations.size();
            }

        } catch (IllegalArgumentException e) {
            throw new BankStatementException("Invalid CSV format: missing required header or invalid file content");
        } catch (IOException e) {
            throw new BankStatementException("Failed to read uploaded file");
        }
    }

    public BalanceResponse calculateBalance(String accountNumber,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        validateDateRange(from, to);
        var balances = bankOperationRepository.calculateBalancesByCurrency(accountNumber, from, to);

        return new BalanceResponse(accountNumber, balances);
    }

    public ExportResult exportToCsv(List<String> accounts, LocalDateTime from, LocalDateTime to) {
        validateDateRange(from, to);

        var operations = bankOperationRepository.findForExport(accounts, from, to);
        var csv = generateCsv(operations);

        return new ExportResult(csv, operations.size());
    }

    private byte[] generateCsv(List<BankOperation> operations) {
        try (var out = new ByteArrayOutputStream();
             var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("accountNumber", "operationDateTime", "beneficiary", "comment", "amount", "currency")
                     .build()
             )) {

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

        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }

    }

    private static void validateDateRange(LocalDateTime from, LocalDateTime to) {
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
}
