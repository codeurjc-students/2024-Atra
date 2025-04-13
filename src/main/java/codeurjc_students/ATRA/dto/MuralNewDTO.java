package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
public class MuralNewDTO {

	private String name;
	private String description;

	private byte[] thumbnail;
	private byte[] banner;

	public MuralNewDTO(Mural mural) {
		name = mural.getName();
		description = mural.getDescription();
		banner = mural.getBanner();
		thumbnail = mural.getThumbnail();
	}
}