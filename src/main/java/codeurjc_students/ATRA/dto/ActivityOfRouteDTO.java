package codeurjc_students.atra.dto;

import codeurjc_students.atra.model.Activity;
import codeurjc_students.atra.model.ActivitySummary;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class ActivityOfRouteDTO implements ActivityDtoInterface {
	private Long id;
	private String name;
	private Long user;
	private ActivitySummary summary;

	public ActivityOfRouteDTO(Activity activity) {
		id = activity.getId();
		name = activity.getName();
		user = activity.getOwner().getId();
		summary = activity.getSummary();

	}

	public static List<ActivityOfRouteDTO> toDto(Collection<Activity> activities) {
		return activities.stream().map(ActivityOfRouteDTO::new).toList();
	}
}