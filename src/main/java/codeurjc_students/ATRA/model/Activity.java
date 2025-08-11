package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Setter
@Getter
@Entity
@Table(name = "activities")
public class Activity implements NamedId {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String type;
	private Instant startTime;
	private Visibility visibility = new Visibility();

	@ElementCollection // maybe should be (fetch = FetchType.EAGER) to avoid issues with lazy loading
	@OrderColumn(name = "position")
	private List<DataPoint> dataPoints = new ArrayList<>();

	@ToString.Exclude
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private User user;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	private Route route;

	public void addDataPoint(DataPoint dataPoint) {
		dataPoints.add(dataPoint);
	}

	public boolean hasRoute(){ return route!=null;}

	public void removeRoute() {
		route = null;
	}

	public void changeVisibilityTo(VisibilityType visibilityType) {
		visibility.changeTo(visibilityType);
	}
	public void changeVisibilityTo(VisibilityType visibilityType, Collection<Long> allowedMurals) {
		visibility.changeTo(visibilityType, allowedMurals);
	}
}