package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface MuralRepository extends JpaRepository<Mural, Long> {
    Optional<Mural> findByCode(String muralCode);

    Collection<Mural> findByOwner(User user);
}
