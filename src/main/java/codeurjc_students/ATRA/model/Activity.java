package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	@ElementCollection
	@OrderColumn(name = "position")
	private List<DataPoint> dataPoints = new ArrayList<>();

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	@ToString.Exclude
	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	private Route route;

	@ToString.Exclude
	@ManyToMany(mappedBy = "activities", fetch = FetchType.LAZY)
	private List<Mural> murals;

	public void addDataPoint(DataPoint dataPoint) {
		dataPoints.add(dataPoint);
	}
}