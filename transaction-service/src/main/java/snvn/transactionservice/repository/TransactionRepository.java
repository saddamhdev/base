package snvn.transactionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snvn.transactionservice.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByTransactionType(String transactionType);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByFromAccountNumber(String fromAccountNumber);

    List<Transaction> findByToAccountNumber(String toAccountNumber);
}

