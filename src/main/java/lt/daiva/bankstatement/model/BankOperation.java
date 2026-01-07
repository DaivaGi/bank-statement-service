package lt.daiva.bankstatement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_operation")
public class BankOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    @Column(nullable = false, length = 255)
    private String beneficiary;

    @Column(name = "operation_comment", length = 1024)
    private String operationComment;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected BankOperation() {
    }

    public BankOperation(String accountNumber, LocalDateTime operationTime, String beneficiary, String operationComment, BigDecimal amount, String currency) {
        this.accountNumber = accountNumber;
        this.operationTime = operationTime;
        this.beneficiary = beneficiary;
        this.operationComment = operationComment;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public String getBeneficiary() {
        return beneficiary;
    }

    public String getOperationComment() {
        return operationComment;
    }

    public BigDecimal getAmount() {
        return amount;
    }


    public String getCurrency() {
        return currency;
    }
}