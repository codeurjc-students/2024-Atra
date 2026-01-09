package codeurjc_students.atra.dto;

import codeurjc_students.atra.model.Coordinates;
import codeurjc_students.atra.model.Route;
import codeurjc_students.atra.model.auxiliary.Visibility;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Setter
@Getter
public class RouteWithoutActivityDTO implements RouteDtoInterface {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Double totalDistance;
	private Double elevationGain;
	private Visibility visibility;
	@ElementCollection
	@OrderColumn(name = "position")
	private List<Coordinates> coordinates;
	private String name;
	@Nullable
	private String description;

	private Long createdBy;

	public RouteWithoutActivityDTO(Route route) {
		this.id = route.getId();
		this.totalDistance = route.getTotalDistance();
		this.elevationGain = route.getElevationGain();
		this.coordinates = route.getCoordinates();
		this.name = route.getName();
		this.description = route.getDescription();
		this.visibility = route.getVisibility();
		this.createdBy = route.getCreatedBy().getId();

	}

    public static List<RouteWithoutActivityDTO> toDto(Collection<Route> routes) {
		return routes.stream().map(RouteWithoutActivityDTO::new).toList();
    }
}