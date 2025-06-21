package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Mural;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MuralRepository extends JpaRepository<Mural, Long> {
    Optional<Mural> findByCode(String muralCode);
}
