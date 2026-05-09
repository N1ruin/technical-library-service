package by.niruin.library.repository;

import by.niruin.library.domain.SafetyInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyInstructionRepository extends JpaRepository<SafetyInstruction, Long> {
    boolean existsByNumber(String number);
}
