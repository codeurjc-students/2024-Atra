package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
public class MuralDTO {

	private Long id;
	private String name;
	private String description;

	//entities could be User if we modify the user to remove its references. Either set to null or use a DTO
	private NamedId owner;
	private List<NamedId> members = new ArrayList<>();
	private List<NamedId> activities = new ArrayList<>();
	private List<NamedId> routes = new ArrayList<>();

	private byte[] thumbnail;
	private byte[] banner;

	public MuralDTO(Mural mural) {
		id = mural.getId();
		name = mural.getName();
		description = mural.getDescription();
		owner = new BasicNamedId(mural.getOwner().getId(), mural.getOwner().getDisplayname());
		mural.getMembers().forEach(user -> {
			this.members.add(new BasicNamedId(user.getId(), user.getDisplayname()));
		});
		mural.getActivities().forEach(activity -> {
			this.members.add(new BasicNamedId(activity.getId(), activity.getName()));
		});
		mural.getRoutes().forEach(route -> {
			this.members.add(new BasicNamedId(route.getId(), route.getName()));
		});
		banner = mural.getBanner();
		thumbnail = mural.getThumbnail();
	}
}