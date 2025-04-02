package codeurjc_students.ATRA.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Data
@Setter
@Getter
@Entity
@Table(name = "murals")
public class Mural {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Exclude
	@ManyToOne
	private User owner;
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_mural", joinColumns = @JoinColumn(name="mural_id"), inverseJoinColumns = @JoinColumn(name="user_id"))
	private List<User> members;

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "activity_mural", joinColumns =  @JoinColumn(name="mural_id"), inverseJoinColumns =  @JoinColumn(name="activity_id"))
	private List<Activity> activities;

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "route_mural", joinColumns =  @JoinColumn(name="mural_id"), inverseJoinColumns =  @JoinColumn(name="route_id"))
	private List<Route> routes;
}