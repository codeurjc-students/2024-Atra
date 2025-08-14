package codeurjc_students.ATRA.model;

import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import jakarta.persistence.*;
import lombok.*;

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

	@Column(unique = true)
	private String code;
	private VisibilityType visibility = VisibilityType.PUBLIC; //Only uses private and public

	@ToString.Exclude
	@ManyToOne
	private User owner;
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_mural", joinColumns = @JoinColumn(name="mural_id"), inverseJoinColumns = @JoinColumn(name="user_id"))
	private List<User> members = new ArrayList<>();
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "banned_user_mural", joinColumns = @JoinColumn(name="mural_id"), inverseJoinColumns = @JoinColumn(name="user_id"))
	private List<User> bannedUsers = new ArrayList<>();

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

	public Mural(String name, String description, User owner, VisibilityType visibility, byte[] thumbnail, byte[] banner) {
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.members.add(owner);
		this.visibility = visibility;
		this.thumbnail = thumbnail;
		this.banner = banner;
	}

	public void removeOwner() {
		removeMember(owner);
		owner = members.get(0);
	}
	public void removeOwner(User user, User inheritor)  {
		if (!owner.equals(user)) return;
		removeMember(user);
		if (inheritor==null) owner = members.get(0);
		else if (members.contains(inheritor)) owner = inheritor;
		else throw new IllegalArgumentException("Mural.removeOnwer() called with an inheritor who's not part of the mural.");
	}

	public void removeMember(User user)  {
		if (members.size()==1) throw new RuntimeException("removeMember called with only one member remaining. delete should be called instead.");
		members.remove(user);
	}

	public void addMember(User user) {
		members.add(user);
	}

	public void banUser(User user) {
		bannedUsers.add(user);
	}

	public void setVisibility(VisibilityType visibilityType) {
		if (visibilityType==VisibilityType.MURAL_PUBLIC || visibilityType==VisibilityType.MURAL_SPECIFIC) throw new IllegalArgumentException("VisibilityType for a mural must be PUBLIC or PRIVATE, not "+visibilityType);
		this.visibility = visibilityType;
	}
}