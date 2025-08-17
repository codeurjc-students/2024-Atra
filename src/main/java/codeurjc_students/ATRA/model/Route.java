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
	@ToString.Exclude
	@OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
	private List<Activity> activities = new ArrayList<>();


	public void addActivity(Activity activityId) {
		activities.add(activityId);
	}

	public void removeActivity(Activity activity) {
		activities.remove(activity);
	}

	public void changeVisibilityTo(VisibilityType visibilityType) {
		visibility.changeTo(visibilityType);
	}
	public void changeVisibilityTo(VisibilityType visibilityType, Collection<Long> allowedMurals) {
		visibility.changeTo(visibilityType, allowedMurals);
	}

	public User getCreatedBy(){return createdBy;}
}