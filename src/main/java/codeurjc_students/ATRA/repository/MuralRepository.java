package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuralRepository extends JpaRepository<Mural, Long> {
}
