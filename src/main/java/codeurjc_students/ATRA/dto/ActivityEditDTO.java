package codeurjc_students.atra.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEditDTO implements ActivityDtoInterface {
	private String name;
	private String type;

}