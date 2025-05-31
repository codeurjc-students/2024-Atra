package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.NamedId;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@Getter
@Entity
@Table(name = "users")
public class User implements NamedId {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String username;
	private String password;

	private String name;
	private String email;

	//private List<Route> routes;

	@ToString.Exclude
	@OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
	private List<Mural> ownedMurals = new ArrayList<>();;
	@ToString.Exclude
	@ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
	private List<Mural> memberMurals = new ArrayList<>();;

	@ToString.Exclude
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY) //delete Activity if its User is deleted, twice over.
	private List<Activity> activities = new ArrayList<>();

	//<editor-fold desc="private List<String> roles">
	@Column(name = "role")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	private List<String> roles = new ArrayList<>();;
	//</editor-fold>

	public User(){
		setDefaultRoles();
	}
	public User(String username, String password){
		this.username = username;
		this.password = password;
		setDefaultRoles();
	}

	private void setDefaultRoles(){
		this.roles = List.of("USER");
	}

	public void addActivity(Activity activity) {
		activities.add(activity);
	}

	//public void addActivity(Long id) {
	//	activities.add(id);
	//}

	public boolean hasActivity(Activity activity) {
		return activities.contains(activity);
	}

	public void removeActivity(Activity activity) {
		activities.remove(activity);
	}

	public void removeOwnedMural(Mural mural) {
		ownedMurals.remove(mural);
		memberMurals.remove(mural);
	}

	public void removeMemberMural(Mural mural) {
		memberMurals.remove(mural);
		//maybe remove it from ownerMurals as well? This will need to be fleshed out when we get to murals
	}
}