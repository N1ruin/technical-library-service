package by.niruin.library.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_outbox")
public class TransactionOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
