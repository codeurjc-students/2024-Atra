package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Data
@Setter
@Getter
@Entity
@Table(name = "routes")
public class Route implements NamedId {


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

	private Visibility visibility = new Visibility(VisibilityType.PRIVATE);


	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private User createdBy;

	public void changeVisibilityTo(VisibilityType visibilityType) {
		visibility.changeTo(visibilityType);
	}
	public void changeVisibilityTo(VisibilityType visibilityType, Collection<Long> allowedMurals) {
		visibility.changeTo(visibilityType, allowedMurals);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Route route)) return false;
        return Objects.equals(id, route.id) && Objects.equals(totalDistance, route.totalDistance) && Objects.equals(elevationGain, route.elevationGain) && Objects.equals(name, route.name) && Objects.equals(description, route.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, totalDistance, elevationGain, name, description);
	}
}