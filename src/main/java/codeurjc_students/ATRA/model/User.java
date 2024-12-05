package codeurjc_students.ATRA.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@Getter
@Entity
@Table(name = "users")
public class User {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String username;
	private String password;

	private String displayname;
	private String email;

	//private List<Route> routes;
	//private List<Mural> murals;
	@ElementCollection
	private List<Long> activities = new ArrayList<>();

	//<editor-fold desc="private List<String> roles">
	@Column(name = "role")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	private List<String> roles;
	//</editor-fold>

	public User(){
		setDefaultRoles();
	}
	public User(String username, String password){
		this.username = username;
		this.password = password;
		setDefaultRoles();
	}
	public String getEncodedPassword() {
		return password;
	}

	private void setDefaultRoles(){
		this.roles = List.of("USER");
	}

	public void addActivity(Activity activity) {
		activities.add(activity.getId());
	}

	public void addActivity(Long id) {
		activities.add(id);
	}

	public boolean hasActivity(Long id) {
		return activities.contains(id);
	}
}