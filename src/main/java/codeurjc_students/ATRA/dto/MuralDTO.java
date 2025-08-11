package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
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
	private String code;
	private String visibility;
	private String thumbnailUrl;
	private String bannerUrl;

	//entities could be User if we modify the user to remove its references. Either set to null or use a DTO
	private NamedId owner;
	private List<NamedId> members = new ArrayList<>();
	private List<NamedId> activities = new ArrayList<>();
	private List<NamedId> routes = new ArrayList<>();

	public MuralDTO(Mural mural) {
		id = mural.getId();
		name = mural.getName();
		description = mural.getDescription();
		//code = mural.getCode(); //code should be somewhat confidential. Only visible to owner and maybe members
		visibility = mural.getVisibility().toString();
		thumbnailUrl = "/api/murals/"+id+"/thumbnail";
		bannerUrl = "/api/murals/"+id+"/banner";

		owner = new BasicNamedId(mural.getOwner().getId(), mural.getOwner().getName());
		mural.getMembers().forEach(user -> {
			this.members.add(new BasicNamedId(user.getId(), user.getName()));
		});
	}

    public static List<MuralDTO> toDto(Collection<Mural> murals) {
		return murals.stream().map(MuralDTO::new).toList();
    }
}