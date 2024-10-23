package codeurjc_students.ATRA.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@Entity
@Table(name = "murals")
public class Mural {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
}