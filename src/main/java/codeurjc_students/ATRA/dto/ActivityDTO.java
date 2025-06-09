package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.ActivityService; //consider substituting this for a DistanceUtils or similar utility class, consisting of static methods, and independent of spring
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
public class ActivityDTO implements ActivityDtoInterface {
	private Long id;
	private String name;
	private String type;
	private Instant startTime;
	private NamedId user; //should check this doesn't break anything
	private NamedId route;
	private List<DataPoint> dataPoints; // I think this can be deleted
	private Map<String, List<String>> streams;
	private ActivitySummary summary;
	private VisibilityType visibility;

	public ActivityDTO(Activity activity){
		id = activity.getId();
		name = activity.getName();
		type = activity.getType();
		startTime = activity.getStartTime();
		user = new BasicNamedId(activity.getUser());
		dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		summary = new ActivitySummary(this);
		route = activity.getRoute() != null ? new BasicNamedId(activity.getRoute().getId(), activity.getRoute().getName()) : null;
		visibility = activity.getVisibility().getType();
	}

	//these may not be needed (since activity holds its route now)
	public ActivityDTO(Activity activity, NamedId routeIdAndName) {
		id = activity.getId();
		name = activity.getName();
		type = activity.getType();
		startTime = activity.getStartTime();
		user = new BasicNamedId(activity.getUser());
		route = routeIdAndName;
		dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		summary = new ActivitySummary(this);
		visibility = activity.getVisibility().getType();
	}

	//these may not be needed.
	public ActivityDTO(Activity activity, Route route) {
		this.id = activity.getId();
		this.name = activity.getName();
		this.type = activity.getType();
		this.startTime = activity.getStartTime();
		this.user = new BasicNamedId(activity.getUser());
		this.route = new BasicNamedId(route);
		this.dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		summary = new ActivitySummary(this);
		visibility = activity.getVisibility().getType();
	}



	private void setUpStreams(List<DataPoint> dataPoints) {
		streams = new HashMap<>();
		Double distance = 0.0;
		Double prevLat = null;
		Double prevLon = null;
		Double prevEle = null;

		streams.put("time", new ArrayList<>());
		streams.put("distance", new ArrayList<>());
		streams.put("position", new ArrayList<>());
		streams.put("altitude", new ArrayList<>());
		streams.put("elevation_gain", new ArrayList<>());
		streams.put("heartrate", new ArrayList<>());
		streams.put("cadence", new ArrayList<>());
		streams.put("pace", new ArrayList<>());

		for (int i=0; i<dataPoints.size();i++) {
			DataPoint dP = dataPoints.get(i);

			if (prevLat == null) prevLat = dP.get_lat();
			if (prevLon == null) prevLon = dP.get_long();
			if (prevEle == null) prevEle = dP.get_ele();

			Double dist = ActivityService.totalDistance(prevLat, prevLon, dP);
			Double eleGain = dP.get_ele()-prevEle;

			streams.get("time").add(dP.get_time().toString());
			streams.get("distance").add(Double.toString(dist+distance));
			streams.get("position").add(dP.get_lat().toString() + ";" + dP.get_long().toString());
			streams.get("altitude").add(dP.get_ele().toString());
			streams.get("elevation_gain").add(eleGain.toString());
			streams.get("heartrate").add(Integer.toString(dP.getHr()));
			streams.get("cadence").add(Integer.toString(dP.getCad()));
			streams.get("pace").add(getPace(i, dataPoints));
			distance += dist;
			prevLat = dP.get_lat();
			prevLon = dP.get_long();
			prevEle = dP.get_ele();
		}
	}

	private String getPace(int currentPos, List<DataPoint> dataPoints) {
		DataPoint currentDP = dataPoints.get(currentPos);
		DataPoint prevDP =  currentPos-1>=0 ? dataPoints.get(currentPos-1):currentDP;
		DataPoint nextDP =  currentPos+1<=dataPoints.size()-1 ? dataPoints.get(currentPos+1):currentDP;

		Double prevDistance = ActivityService.totalDistance(prevDP, currentDP);
		Double nextDistance = ActivityService.totalDistance(nextDP, currentDP);
		double totalDistance = prevDistance+nextDistance;
		long totalTime = Duration.between(prevDP.get_time(), nextDP.get_time()).toSeconds();
		long paceSecs = Math.round((double) totalTime / totalDistance);
		if (paceSecs==Long.MAX_VALUE) {
			return "0";
		}
		return String.valueOf(Math.round((double) totalTime / totalDistance)); //seconds / kilometer
	}
}