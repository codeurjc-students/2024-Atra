package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.NewUserDTO;
import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id){
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userService.toDTO(userOpt.get()));
    }

    @GetMapping("/IsUsernameTaken")
    public ResponseEntity<Boolean> isUsernameTaken(@RequestParam String username){
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody NewUserDTO userDTO){
        if (!isValidUser(userDTO)) return ResponseEntity.badRequest().build();

        User user = new User(userDTO.getUsername(), passwordEncoder.encode(userDTO.getPassword()));

        if (userDTO.hasEmail()) user.setEmail(userDTO.getEmail());
        if (userDTO.hasDisplayName())
            user.setDisplayname(userDTO.getDisplayname());
        else
            user.setDisplayname(userDTO.getUsername());

        userService.save(user);
        return ResponseEntity.ok(user);
    }

    public ResponseEntity<User> modifyUser(){return null;}

    public ResponseEntity<User> deleteUser(){
        return null;
    }

    // <editor-fold desc="Auxiliary Methods">
    private boolean isValidUser(NewUserDTO user){
        if (user.getUsername()==null || user.getPassword()==null) return false;
        if (userService.existsByUsername(user.getUsername())) return false;
        //other conditions
        return true;
    }
    // <editor-fold>

}

