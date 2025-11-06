package codeurjc_students.atra.repository;

import codeurjc_students.atra.model.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Long> {
}
