package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
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
public class ActivityDTO {
	private Long id;
	private String name;
	private String type;
	private Instant startTime;
	private Long user;
	private Long route;
	private Double totalDistance;
	private Long totalTime; //seconds
	private List<DataPoint> dataPoints;
	private Map<String, List<String>> streams;

	public ActivityDTO(Activity activity) {
		id = activity.getId();
		name = activity.getName();
		type = activity.getType();
		startTime = activity.getStartTime();
		user = activity.getUser();
		route = activity.getRoute();
		dataPoints = activity.getDataPoints();
		setUpStreams(activity.getDataPoints());
		totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
		totalTime = calcTotalTime(activity);
	}

	private long calcTotalTime(Activity activity) {
		Instant start = activity.getStartTime();
		Instant end = activity.getDataPoints().get(activity.getDataPoints().size()-1).get_time();
		Duration duration = Duration.between(start, end);
		return duration.toSeconds();
	}

	public static List<ActivityDTO> toDTO(List<Activity> activities) {
		List<ActivityDTO> dtoList = new ArrayList<>();
		for (var act: activities) {
			dtoList.add(toDTO(act));
		}
		return dtoList;
	}

	private void setUpStreams(List<DataPoint> dataPoints) {
		streams = new HashMap<>();
		Double distance = 0.0;
		Double prevLat = null;
		Double prevLon = null;
		int c = 0;

		streams.put("time", new ArrayList<>());
		streams.put("distance", new ArrayList<>());
		streams.put("position", new ArrayList<>());
		streams.put("altitude", new ArrayList<>());
		streams.put("heartrate", new ArrayList<>());
		streams.put("cadence", new ArrayList<>());
		streams.put("pace", new ArrayList<>());

		for (int i=0; i<dataPoints.size();i++) {
			DataPoint dP = dataPoints.get(i);

			if (prevLat == null) prevLat = dP.get_lat();
			if (prevLon == null) prevLon = dP.get_long();
			Double dist = totalDistance(prevLat, prevLon, dP);

			streams.get("time").add(dP.get_time().toString());
			streams.get("distance").add(Double.toString(dist+distance));
			streams.get("position").add(dP.get_lat().toString() + ";" + dP.get_long().toString());
			streams.get("altitude").add(dP.get_ele().toString());
			streams.get("heartrate").add(Integer.toString(dP.getHr()));
			streams.get("cadence").add(Integer.toString(dP.getCad()));
			streams.get("pace").add(getPace(dP, i, dataPoints));
			distance += dist;
			prevLat = dP.get_lat();
			prevLon = dP.get_long();
		}
	}

	private String getPace(DataPoint dP, int currentPos, List<DataPoint> dataPoints) {
		DataPoint prevDP =  currentPos-1>=0 ? dataPoints.get(currentPos-1):dP;
		DataPoint nextDP =  currentPos+1<=dataPoints.size()-1 ? dataPoints.get(currentPos+1):dP;

		Double prevDistance = totalDistance(prevDP, dP);
		Double nextDistance = totalDistance(nextDP, dP);
		double totalDistance = prevDistance+nextDistance;
		long totalTime = Duration.between(prevDP.get_time(), nextDP.get_time()).toSeconds();

		return String.valueOf(Math.round((double) totalTime / totalDistance)); //seconds / kilometer
	}

	private Double totalDistance(DataPoint dp1, DataPoint dP2){
		return totalDistance(dp1.get_lat(), dp1.get_long(), dP2.get_lat(), dP2.get_long());
	}

	private Double totalDistance(Double lat1, Double lon1, DataPoint dP) {
		return totalDistance(lat1, lon1, dP.get_lat(), dP.get_long());
	}

	private Double totalDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
		//Haversine formula, courtesy of https://stackoverflow.com/a/27943, https://stackoverflow.com/a/11172685, https://www.movable-type.co.uk/scripts/latlong.html
		var R = 6371; // Radius of the earth in km
		double dLat = deg2rad(lat2-lat1);  // deg2rad below
		double dLon = deg2rad(lon2-lon1);
		var a =
				Math.sin(dLat/2) * Math.sin(dLat/2) +
						Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
								Math.sin(dLon/2) * Math.sin(dLon/2);
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return R * c; //distance in km
	}

	private Double deg2rad(Double deg) {
		return deg * (Math.PI/180);
	}

	static ActivityDTO toDTO(Activity activity) {
		return new ActivityDTO(activity);
	}
}