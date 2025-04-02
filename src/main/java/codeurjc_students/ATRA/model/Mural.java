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

	public void removeActivity(Activity activity) {
		this.activities.remove(activity);
	}

	public void removeRoute(Route route) {
		routes.remove(route);
	}

	public void removeOwner(User user)  {
		removeMember(user);
		owner = members.get(0);
	}

	public void removeMember(User user)  {
		if (members.size()==1) throw new RuntimeException("removeMember called with only one member remaining. delete should be called instead.");
		members.remove(user);
	}
}