package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByName(String name);

    List<Route> findByName(String s);

    List<Route> findByVisibilityType(VisibilityType visibilityType);

    @Query("""
        SELECT r FROM Route r
        LEFT JOIN r.visibility.allowedMurals m
        WHERE (r.visibility.type = 'MURAL_SPECIFIC' AND m = :muralId)
          OR r.visibility.type = 'PUBLIC'
    """)
    List<Route> findVisibleToMural(@Param("muralId") Long muralId);

}
