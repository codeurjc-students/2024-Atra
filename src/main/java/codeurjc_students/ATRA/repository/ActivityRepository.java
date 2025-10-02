package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Collection<Activity> findByVisibilityType(VisibilityType visibilityType);
    Collection<Activity> findByVisibilityTypeInAndOwnerIn(Collection<VisibilityType> visibility_types, Collection<User> user);

    @Query("""
        SELECT a FROM Activity a
        LEFT JOIN a.visibility.allowedMurals m
        WHERE (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND m = :muralId)
          )
          AND (a.owner.id IN :memberIds)
    """)
    Collection<Activity> findVisibleToMural(@Param("muralId") Long muralId, List<Long> memberIds);

    @Query("""
        SELECT a FROM Activity a
        LEFT JOIN a.visibility.allowedMurals m
        WHERE (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND m = :muralId)
          )
          AND (a.owner.id IN :memberIds)
    """)
    Page<Activity> findVisibleToMural(@Param("muralId") Long muralId, List<Long> memberIds, Pageable pageable);

    @Query("""
        SELECT a FROM Activity a
        LEFT JOIN a.visibility.allowedMurals m
        WHERE (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND m = :muralId)
          )
          AND (a.owner.id IN :memberIds)
    """)
    Page<Activity> findVisibleToMuralAndRouteIsNull(@Param("muralId") Long muralId, List<Long> memberIds, Pageable pageable);

    Collection<Activity> findByRoute(Route route);

    List<Activity> findByOwner(User user);

    List<Activity> findByOwnerAndRouteIsNull(User user);

    Page<Activity> findByOwner(User user, Pageable pageable);

    Page<Activity> findByOwnerAndRouteIsNull(User user, Pageable pageable);

    @Query("""
        SELECT a FROM Activity a
        WHERE (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND :muralId IN elements(a.visibility.allowedMurals))
          )
          AND (a.owner.id IN :memberIds)
          AND (a.route IS NULL)
    """)
    Collection<Activity> findVisibleToMuralAndRouteIsNull(@Param("muralId") Long id, List<Long> memberIds);

    @Query("""
    SELECT a FROM Activity a
    WHERE a.owner = :user
    AND (
            a.visibility.type = 'PUBLIC'
            OR a.visibility.type = 'MURAL_PUBLIC'
            OR (a.visibility.type = 'MURAL_SPECIFIC' AND :muralId IN elements(a.visibility.allowedMurals))
          )
    """)
    List<Activity> findByUserAndVisibleToMural(User user, Long muralId);

    Collection<Activity> getByRoute(Route route);

    List<Activity> findByRouteAndOwner(Route route, User user);

    List<Activity> findByRouteAndOwnerAndVisibilityTypeIn(Route route, User user, List<VisibilityType> visibilityTypes);

    @Query("""
    SELECT a FROM Activity a
    WHERE a.route=:route AND
    (
        a.visibility.type = 'PUBLIC'
        OR a.visibility.type = 'MURAL_PUBLIC'
        OR (a.visibility.type = 'MURAL_SPECIFIC' AND :muralId IN elements(a.visibility.allowedMurals))
    )
    """)
    Collection<Activity> findByRouteAndMural(Route route, Long muralId);

    @Query("""
    SELECT a FROM Activity a
    WHERE a.route.id = :routeId
    """)
    List<Activity> findByRoute(@Param("routeId") Long routeId);
}
