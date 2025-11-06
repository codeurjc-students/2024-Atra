package codeurjc_students.atra.repository;

import codeurjc_students.atra.model.Mural;
import codeurjc_students.atra.model.User;
import codeurjc_students.atra.model.auxiliary.VisibilityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface MuralRepository extends JpaRepository<Mural, Long> {
    Optional<Mural> findByCode(String muralCode);

    Collection<Mural> findByOwner(User user);

    Collection<Mural> findByVisibility(VisibilityType visibilityType);
}
