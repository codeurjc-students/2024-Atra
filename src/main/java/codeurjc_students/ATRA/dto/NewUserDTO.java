package codeurjc_students.ATRA.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


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