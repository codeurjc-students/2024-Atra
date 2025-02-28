package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@Getter
@Entity
@Table(name = "routes")
public class Route {


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

}