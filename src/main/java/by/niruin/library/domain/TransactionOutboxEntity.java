package by.niruin.library.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "transaction_outbox")
@EntityListeners(AuditingEntityListener.class)
public class TransactionOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(length = 1000)
    private OutboxType outboxType;
    @Lob
    private String payload;
    @CreatedDate
    private Instant timestamp;
}
