package lt.daiva.bankstatement.repository;

import lt.daiva.bankstatement.model.BankOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankOperationRepository extends JpaRepository<BankOperation, Long> {
}
