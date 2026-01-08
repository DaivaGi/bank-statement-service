package lt.daiva.bankstatement.repository;

import lt.daiva.bankstatement.model.BankOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class BankOperationRepositoryTest {

    @Autowired
    private BankOperationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldCalculateBalanceWithinDateRange() {
        // given
        entityManager.persist(new BankOperation("LT1",
                LocalDateTime.parse("2025-01-01T10:00:00"),
                "A", null, new BigDecimal("100"), "EUR"));

        entityManager.persist(new BankOperation("LT1",
                LocalDateTime.parse("2025-01-05T10:00:00"),
                "B", null, new BigDecimal("50"), "EUR"));

        entityManager.persist(new BankOperation("LT1",
                LocalDateTime.parse("2025-01-10T10:00:00"),
                "C", null, new BigDecimal("25"), "EUR"));

        entityManager.flush();

        // when
        BigDecimal result = repository.calculateBalance(
                "LT1",
                LocalDateTime.parse("2025-01-02T00:00:00"),
                LocalDateTime.parse("2025-01-09T23:59:59")
        );

        // then
        assertEquals(0, result.compareTo(new BigDecimal("50")));
    }

    @Test
    void shouldIncludeOperationsOnBoundaryDates() {
        // given
        entityManager.persist(new BankOperation("LT2",
                LocalDateTime.parse("2025-01-02T00:00:00"),
                "A", null, new BigDecimal("10"), "EUR"));

        entityManager.flush();

        // when
        BigDecimal result = repository.calculateBalance(
                "LT2",
                LocalDateTime.parse("2025-01-02T00:00:00"),
                LocalDateTime.parse("2025-01-02T00:00:00")
        );

        // then
        assertEquals(0, result.compareTo(new BigDecimal("10")));
    }
}