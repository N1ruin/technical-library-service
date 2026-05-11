package by.niruin.library.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "transaction_outbox")
@EntityListeners(AuditingEntityListener.class)
public class TransactionOutboxRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 100, nullable = false)
    private EventType eventType;
    @Lob
    @Column(nullable = false)
    private String payload;
    @CreatedDate
    @Column(nullable = false)
    private Instant timestamp;

    public Long getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
