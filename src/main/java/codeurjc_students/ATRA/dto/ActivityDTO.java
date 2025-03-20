package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
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
	private Long user;
	private NamedId route;
	private Double totalDistance;
	private Long totalTime; //seconds
	private Double elevationGain;
	private List<DataPoint> dataPoints;
	private Map<String, List<String>> streams;

	public ActivityDTO(Activity activity){
		id = activity.getId();
		name = activity.getName();
		type = activity.getType();
		startTime = activity.getStartTime();
		user = activity.getUser().getId();
		dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
		elevationGain = streams.get("elevation_gain").stream().map(Double::valueOf).filter(v -> v>=0).reduce(0.0, Double::sum);
		totalTime = calcTotalTime(activity);
		if (activity.getRoute() == null) {
			route = null;
		} else {
			throw new RuntimeException("ActivityDTO(Activity a) called with activity.getRoute()!=null");
		}


	}

	public ActivityDTO(Activity activity, NamedId routeIdAndName) {
		id = activity.getId();
		name = activity.getName();
		type = activity.getType();
		startTime = activity.getStartTime();
		user = activity.getUser().getId();
		route = routeIdAndName;
		dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
		elevationGain = streams.get("elevation_gain").stream().map(Double::valueOf).filter(v -> v>=0).reduce(0.0, Double::sum);
		totalTime = calcTotalTime(activity);
	}

	public ActivityDTO(Activity activity, Route route) {
		this.id = activity.getId();
		this.name = activity.getName();
		this.type = activity.getType();
		this.startTime = activity.getStartTime();
		this.user = activity.getUser().getId();
		this.route = new BasicNamedId(route);
		this.dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		this.totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
		this.elevationGain = streams.get("elevation_gain").stream().map(Double::valueOf).filter(v -> v>=0).reduce(0.0, Double::sum);
		this.totalTime = calcTotalTime(activity);
	}

	private long calcTotalTime(Activity activity) {
		Instant start = activity.getStartTime();
		Instant end = activity.getDataPoints().get(activity.getDataPoints().size()-1).get_time();
		Duration duration = Duration.between(start, end);
		return duration.toSeconds();
	}

	private void setUpStreams(List<DataPoint> dataPoints) {
		streams = new HashMap<>();
		Double distance = 0.0;
		Double prevLat = null;
		Double prevLon = null;
		Double prevEle = null;
		int c = 0;

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
			streams.get("pace").add(getPace(dP, i, dataPoints));
			distance += dist;
			prevLat = dP.get_lat();
			prevLon = dP.get_long();
			prevEle = dP.get_ele();
		}
	}

	private String getPace(DataPoint dP, int currentPos, List<DataPoint> dataPoints) {
		DataPoint prevDP =  currentPos-1>=0 ? dataPoints.get(currentPos-1):dP;
		DataPoint nextDP =  currentPos+1<=dataPoints.size()-1 ? dataPoints.get(currentPos+1):dP;

		Double prevDistance = ActivityService.totalDistance(prevDP, dP);
		Double nextDistance = ActivityService.totalDistance(nextDP, dP);
		double totalDistance = prevDistance+nextDistance;
		long totalTime = Duration.between(prevDP.get_time(), nextDP.get_time()).toSeconds();

		return String.valueOf(Math.round((double) totalTime / totalDistance)); //seconds / kilometer
	}
}