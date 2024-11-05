package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
public class Activity {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String type;
	private Instant startTime;

	private Long user;

	@ElementCollection
	private List<DataPoint> dataPoints = new ArrayList<>();

	public void addDataPoint(DataPoint dataPoint) {
		dataPoints.add(dataPoint);
	}
}