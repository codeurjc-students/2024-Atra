package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Coordinates;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.service.ActivityService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
public class ActivityOfRouteDTO implements ActivityDtoInterface {
	private Long id;
	private String name;
	private Long user;
	private Double totalDistance;
	private Long totalTime; //seconds
	private Double elevationGain;

	public ActivityOfRouteDTO(Activity activity) {
		id = activity.getId();
		name = activity.getName();
		user = activity.getUser().getId();
		calcDistanceAndElevation(activity);
		totalTime = calcTotalTime(activity);
	}

	private void calcDistanceAndElevation(Activity activity) {
		DataPoint prevDp = null;
		Double prevEle = null;
		Double distanceTotal = 0.0;
		Double eleTotal = 0.0;
		for (var dp : activity.getDataPoints()) {
			if (prevDp==null) prevDp=dp;
			if (prevEle==null) prevEle=dp.get_ele();

		    distanceTotal += ActivityService.totalDistance(prevDp, dp);
			double eleDelta = dp.get_ele() - prevEle;
			eleTotal +=  eleDelta>=0 ? eleDelta:0;

			prevDp = dp;
			prevEle = dp.get_ele();
		}

		totalDistance = distanceTotal;
		elevationGain = eleTotal;
	}

	private long calcTotalTime(Activity activity) {
		Instant start = activity.getStartTime();
		Instant end = activity.getDataPoints().get(activity.getDataPoints().size()-1).get_time();
		Duration duration = Duration.between(start, end);
		return duration.toSeconds();
	}
}