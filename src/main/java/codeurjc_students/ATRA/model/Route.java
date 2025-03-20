package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	@OneToMany(mappedBy = "route")
	private List<Activity> activities = new ArrayList<>();
	@ManyToMany(mappedBy = "routes")
	private List<Mural> murals;


	public void addActivity(Activity activityId) {
		activities.add(activityId);
	}

	public void removeActivity(Activity activity) {
		activities.remove(activity);
	}

}