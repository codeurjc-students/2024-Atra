package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Coordinates;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
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

	public RouteWithActivityDTO(Route route) {
		this.id = route.getId();
		this.totalDistance = route.getTotalDistance();
		this.elevationGain = route.getElevationGain();
		this.coordinates = route.getCoordinates();
		this.name = route.getName();
		this.description = route.getDescription();
		this.activities = route.getActivities().stream().map(ActivityOfRouteDTO::new).toList();
		this.visibility = route.getVisibility();

	}

	public static List<RouteWithActivityDTO> toDto(Collection<Route> routes) {
		return routes.stream().map(RouteWithActivityDTO::new).toList();
	}
}