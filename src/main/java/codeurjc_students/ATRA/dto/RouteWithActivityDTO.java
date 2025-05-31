package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Coordinates;
import codeurjc_students.ATRA.model.Route;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class RouteWithActivityDTO implements RouteDtoInterface {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Double totalDistance;
	private Double elevationGain;
	@ElementCollection
	@OrderColumn(name = "position")
	private List<Coordinates> coordinates = new ArrayList<>();
	private String name;
	@Nullable
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
	}
}