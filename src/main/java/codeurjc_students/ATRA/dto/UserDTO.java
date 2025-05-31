package codeurjc_students.ATRA.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;

    private String name;
    private String email;
    //private List<Long> routes;
    //private List<Long> murals;

    private List<String> roles;
}
