package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.NewUserDTO;
import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.DeletionService;
import codeurjc_students.ATRA.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;


@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserService userService;
    @Autowired
    private DeletionService deletionService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id){
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userService.toDTO(userOpt.get()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> patchUser(@PathVariable Long id, @RequestBody UserDTO receivedUser, Principal principal){
        User user = userService.findById(id).orElse(null);
        if (user==null) return ResponseEntity.notFound().build();
        if (!user.getUsername().equals(principal.getName())) return ResponseEntity.status(403).build();
        if (receivedUser.getUsername()!=null && !receivedUser.getUsername().isEmpty() && !userService.existsByUsername(receivedUser.getUsername())) {
            user.setUsername(receivedUser.getUsername());
        }
        if (receivedUser.getDisplayname()!=null && !receivedUser.getDisplayname().isEmpty()) {
            user.setDisplayname(receivedUser.getDisplayname());
        }
        if (receivedUser.getEmail()!=null && !receivedUser.getEmail().isEmpty()) {
            user.setEmail(receivedUser.getEmail());
        }
        this.userService.save(user);
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal){
        int isGood = principalVerification(principal);
        if (isGood!=200) return ResponseEntity.status(isGood).build();

        //we can be certain user will be instantiated thanks to principalVerification
        User user = this.userService.findByUserName(principal.getName()).get();
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @GetMapping("/IsUsernameTaken")
    public ResponseEntity<Boolean> isUsernameTaken(@RequestParam String username){
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @GetMapping("/IsLoggedIn")
    public ResponseEntity<Boolean> isLoggedIn(Principal principal){
        return ResponseEntity.ok(principal != null);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody String password, Principal principal){
        if (principal==null) return ResponseEntity.status(401).build();
        User user = userService.findByUserName(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(passwordEncoder.matches(password, user.getPassword()));
    }

    @PostMapping("/password")
    public ResponseEntity<Object> changePassword(@RequestBody String password, Principal principal){
        int isGood = principalVerification(principal);
        if (isGood!=200) return ResponseEntity.status(isGood).build();

        //we can be certain user will be instantiated thanks to principalVerification
        User user = this.userService.findByUserName(principal.getName()).get();
        user.setPassword(passwordEncoder.encode(password));
        userService.save(user);
        return ResponseEntity.ok().build();
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

    @DeleteMapping
    public ResponseEntity<User> deleteCurrentlyAuthenticatedUser(Principal principal){
        int i = this.principalVerification(principal);
        if (i!=200) return ResponseEntity.status(i).build();

        User user = userService.findByUserName(principal.getName()).get(); //safe because of principalVerification
        deletionService.deleteUser(user);

        return ResponseEntity.ok().build();
    }

    // <editor-fold desc="Auxiliary Methods">
    private boolean isValidUser(NewUserDTO user){
        if (user.getUsername()==null || user.getPassword()==null) return false;
        if (userService.existsByUsername(user.getUsername())) return false;
        //other conditions
        return true;
    }

    private int principalVerification(Principal principal) {
        //this could be implemented as returning the User if it exists or throwing an error
        //with the code in its body or similar if it doesn't
        if (principal==null) return 401;
        User user = userService.findByUserName(principal.getName()).orElse(null);
        if (user == null) return 404;
        else return 200;
    }
    // <editor-fold>

}

