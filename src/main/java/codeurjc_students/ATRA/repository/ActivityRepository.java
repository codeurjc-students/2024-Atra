package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}
