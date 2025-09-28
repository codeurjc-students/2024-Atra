package codeurjc_students.ATRA.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MuralEditDTO implements ActivityDtoInterface {
	private String name;
	private String description;
	private Long newOwner;

}