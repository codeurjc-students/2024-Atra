package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}
