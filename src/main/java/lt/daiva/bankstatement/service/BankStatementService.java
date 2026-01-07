package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.model.BankOperation;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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

    private final BankOperationRepository repository;

    public BankStatementService(BankOperationRepository repository) {
        this.repository = repository;
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

            Iterable<CSVRecord> records = format.parse(reader);

            List<BankOperation> operations = new ArrayList<>();

            for (CSVRecord record : records) {
                BankOperation op = new BankOperation(
                        record.get("accountNumber"),
                        LocalDateTime.parse(record.get("operationDateTime")),
                        record.get("beneficiary"),
                        record.get("comment"),
                        new BigDecimal(record.get("amount")),
                        record.get("currency").toUpperCase()
                );
                operations.add(op);
            }

            repository.saveAll(operations);
            return operations.size();

        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV file", e);
        }
    }

    public BalanceResponse calculateBalance(String accountNumber,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        BigDecimal balance = Optional
                .ofNullable(repository.calculateBalance(accountNumber, from, to))
                .orElse(BigDecimal.ZERO);

        return new BalanceResponse(accountNumber, balance, "EUR");
    }

    public byte[] exportToCsv(List<String> accounts, LocalDateTime from, LocalDateTime to) {
        var ops = repository.findForExport(accounts, from, to);

        try (var out = new ByteArrayOutputStream();
             var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("accountNumber", "operationDateTime", "beneficiary", "comment", "amount", "currency")
                     .build()
             )) {

            for (var op : ops) {
                printer.printRecord(
                        op.getAccountNumber(),
                        op.getOperationTime(),
                        op.getBeneficiary(),
                        op.getOperationComment(),
                        op.getAmount(),
                        op.getCurrency()
                );
            }

            printer.flush();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }
}
