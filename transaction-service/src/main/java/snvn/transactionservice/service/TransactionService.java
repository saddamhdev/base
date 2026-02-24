package snvn.transactionservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import snvn.transactionservice.model.Transaction;
import snvn.transactionservice.producer.TransactionEventProducer;
import snvn.transactionservice.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling transaction business logic
 */
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionEventProducer transactionEventProducer;

    /**
     * Create a new transaction
     */
    public Transaction createTransaction(Transaction transaction) {
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("PENDING");

        Transaction saved = transactionRepository.save(transaction);

        // Publish transaction event to Kafka
        transactionEventProducer.sendTransactionEvent(saved);

        return saved;
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Get transaction by ID
     */
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * Get transactions by account number
     */
    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get transactions by type
     */
    public List<Transaction> getTransactionsByType(String type) {
        return transactionRepository.findByTransactionType(type);
    }

    /**
     * Get transactions by status
     */
    public List<Transaction> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * Update transaction status
     */
    public Transaction updateTransactionStatus(Long id, String status) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            transaction.setStatus(status);
            return transactionRepository.save(transaction);
        }
        return null;
    }
}

