package by.niruin.library.repository;

import by.niruin.library.domain.TransactionOutboxRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionOutboxRepository extends JpaRepository<TransactionOutboxRecord, Long> {
    List<TransactionOutboxRecord> findTopByOrderById(PageRequest pageRequest);
}
