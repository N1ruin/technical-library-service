package by.niruin.library.repository;

import by.niruin.library.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByNameAndStandard(String name, String standard);
}
