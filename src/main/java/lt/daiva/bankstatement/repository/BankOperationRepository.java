package lt.daiva.bankstatement.repository;

import lt.daiva.bankstatement.model.BankOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface BankOperationRepository extends JpaRepository<BankOperation, Long> {

    @Query("""
                select coalesce(sum(b.amount), 0)
                from BankOperation b
                where b.accountNumber = :accountNumber
                  and (:from is null or b.operationTime >= :from)
                  and (:to   is null or b.operationTime <= :to)
            """)
    BigDecimal calculateBalance(
            @Param("accountNumber") String accountNumber,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
