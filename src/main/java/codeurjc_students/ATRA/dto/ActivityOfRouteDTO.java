package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.service.ActivityService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class ActivityOfRouteDTO implements ActivityDtoInterface {
	private Long id;
	private String name;
	private Long user;
	private  ActivitySummary summary;

	public ActivityOfRouteDTO(Activity activity) {
		id = activity.getId();
		name = activity.getName();
		user = activity.getUser().getId();
		summary = new ActivitySummary(id, activity);

	}

	public static List<ActivityOfRouteDTO> toDto(Collection<Activity> activities) {
		return activities.stream().map(ActivityOfRouteDTO::new).toList();
	}
}