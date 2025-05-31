package codeurjc_students.ATRA.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@NoArgsConstructor
public class NewUserDTO {

	private String username;
	private String password;

	private String name; //optional
	private String email; //optional

	public boolean hasDisplayName(){ return name != null;}
	public boolean hasEmail(){ return email != null;}



}