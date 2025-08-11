package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Collection<Activity> findByVisibilityType(VisibilityType visibilityType);
    Collection<Activity> findByVisibilityTypeAndUserIn(VisibilityType visibility_type, Collection<User> users);

    @Query("""
        SELECT a FROM Activity a
        LEFT JOIN a.visibility.allowedMurals m
        WHERE (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND m = :muralId)
          )
          AND (a.user.id IN :memberIds)
    """)
    Collection<Activity> findVisibleToMural(@Param("muralId") Long muralId, List<Long> memberIds);

    Collection<Activity> findByRoute(Route route);

    List<Activity> findByUser(User user);
}
