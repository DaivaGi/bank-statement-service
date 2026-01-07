package lt.daiva.bankstatement.service;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.model.BankOperation;
import lt.daiva.bankstatement.repository.BankOperationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        BigDecimal balance = repository.calculateBalance(accountNumber, from, to);

        return new BalanceResponse(accountNumber, balance, "EUR");
    }
}
