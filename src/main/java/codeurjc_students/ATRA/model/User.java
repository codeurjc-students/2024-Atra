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
	@Column(unique = true)
	private String username;
	@ToString.Exclude
	private String password;

	private String name;
	private String email;

	@ToString.Exclude
	@ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
	private List<Mural> memberMurals = new ArrayList<>();

	@OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
	private List<Route> createdRoutes = new ArrayList<>();

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

	public void removeMemberMural(Mural mural) {
		memberMurals.remove(mural);
		//maybe remove it from ownerMurals as well? This will need to be fleshed out when we get to murals
	}

	public boolean hasRole(String role) {
		return this.roles.contains(role);
	}


    public void addMemberMural(Mural mural) {
		memberMurals.add(mural);
    }

	public void addRoute(Route route) {
		createdRoutes.add(route);
	}

	public void removeRoute(Route r) {
		createdRoutes.remove(r);
	}
}