package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.NewUserDTO;
import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;



@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id){
        return ResponseEntity.ok(new UserDTO(userService.getUser(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> patchUser(Principal principal, @PathVariable Long id, @RequestBody UserDTO receivedUser){
        User authUser = principalVerification(principal);
        User resultUser = userService.patchUser(authUser, id, receivedUser);
        return ResponseEntity.ok(new UserDTO(resultUser));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal){
        User user = principalVerification(principal);
        return ResponseEntity.ok(new UserDTO(user));
    }

    @GetMapping("/IsUsernameTaken")
    public ResponseEntity<Boolean> isUsernameTaken(@RequestParam String username){
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(Principal principal, @RequestBody String password){
        User user = principalVerification(principal);
        return ResponseEntity.ok(userService.verifyPassword(password, user.getPassword()));
    }

    @PostMapping("/password")
    public ResponseEntity<String> changePassword(@RequestBody String password, Principal principal){
        User user = principalVerification(principal);
        userService.changePassword(user, password);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody NewUserDTO userDTO){
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping
    public ResponseEntity<User> deleteCurrentlyAuthenticatedUser(Principal principal){
        User user = principalVerification(principal);
        userService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }

    // <editor-fold desc="Auxiliary Methods">

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
    }
    // <editor-fold>

}

