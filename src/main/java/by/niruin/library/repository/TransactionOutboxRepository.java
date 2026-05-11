package by.niruin.library.repository;

import by.niruin.library.domain.TransactionOutboxRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOutboxRepository extends JpaRepository<TransactionOutboxRecord, Long> {
}
