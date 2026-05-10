package by.niruin.library.repository;

import by.niruin.library.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByIndex(String index);
}
