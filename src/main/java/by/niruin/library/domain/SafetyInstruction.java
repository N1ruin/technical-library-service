package by.niruin.library.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(schema = "library", name = "safety_instruction")
@EntityListeners(AuditingEntityListener.class)
public class SafetyInstruction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 10)
    private String number;
    @Column(length = 200)
    private String description;
    @Column(name = "created_date", nullable = false)
    @CreatedDate
    private LocalDateTime createdDate;
    @Column(name = "updated_date")
    @LastModifiedDate
    private LocalDateTime updatedDate;

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
}
