package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuralRepository extends JpaRepository<User, Long> {
}
