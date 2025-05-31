package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByName(String name);

    List<Route> findByName(String s);
}
