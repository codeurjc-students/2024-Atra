package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.NamedId;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "murals")
public class Mural implements NamedId {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String description;

	@ToString.Exclude
	@ManyToOne
	private User owner;
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_mural", joinColumns = @JoinColumn(name="mural_id"), inverseJoinColumns = @JoinColumn(name="user_id"))
	private List<User> members = new ArrayList<>();

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "activity_mural", joinColumns =  @JoinColumn(name="mural_id"), inverseJoinColumns =  @JoinColumn(name="activity_id"))
	private List<Activity> activities = new ArrayList<>();;

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "route_mural", joinColumns =  @JoinColumn(name="mural_id"), inverseJoinColumns =  @JoinColumn(name="route_id"))
	private List<Route> routes = new ArrayList<>();

	@Lob
	private byte[] thumbnail;
	@Lob
	private byte[] banner;

	public Mural(User owner) {
		this.owner = owner;
		this.members.add(owner);
		this.name = owner.getName() + "'s Mural";
		this.description = "A mural created by " + owner.getName();
	}

	public Mural(User owner, Collection<User> members) {
		this.owner = owner;
		members.forEach(user -> {
			if (!user.equals(owner)) this.members.add(user);
		});
		this.members.add(owner);
		this.name = owner.getName() + "'s Mural";
		this.description = "A mural created by " + owner.getName();
	}

	public Mural(String name, User owner, Collection<User> members) {
		this.name = name;
		this.owner = owner;
		members.forEach(user -> {
			if (!user.equals(owner)) this.members.add(user);
		});
		this.members.add(owner);
		this.description = "A mural created by " + owner.getName();
	}

	public Mural(String name, String description, User owner, byte[] thumbnail, byte[] banner) {
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.members.add(owner);
		this.thumbnail = thumbnail;
		this.banner = banner;
	}
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