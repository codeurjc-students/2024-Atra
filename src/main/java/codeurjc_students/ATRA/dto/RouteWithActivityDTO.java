package codeurjc_students.atra.dto;

import codeurjc_students.atra.model.Coordinates;
import codeurjc_students.atra.model.Route;
import codeurjc_students.atra.model.auxiliary.Visibility;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class RouteWithActivityDTO implements RouteDtoInterface {



	private Long id;
	private Double totalDistance;
	private Double elevationGain;
	private Visibility visibility;

	private List<Coordinates> coordinates = new ArrayList<>();
	private String name;
	private String description;

	private List<ActivityOfRouteDTO> activities = new ArrayList<>();

	public RouteWithActivityDTO(Route route, List<ActivityOfRouteDTO> activities) {
		this.id = route.getId();
		this.totalDistance = route.getTotalDistance();
		this.elevationGain = route.getElevationGain();
		this.coordinates = route.getCoordinates();
		this.name = route.getName();
		this.description = route.getDescription();
		this.activities = activities;
		this.visibility = route.getVisibility();
	}

	public static List<RouteWithActivityDTO> toDto(List<Route> routes, List<List<ActivityOfRouteDTO>> activityList) {
		List<RouteWithActivityDTO> result = new ArrayList<>();
		for (int i = 0; i < routes.size(); i++) {
			result.add(new RouteWithActivityDTO(routes.get(i),activityList.get(i)));

		}
		return result;
	}
}