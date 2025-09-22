package codeurjc_students.ATRA.repository;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.ActivitySummary;
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

public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Long> {
}
