package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.dto.NewUserDTO;
import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.DeletionService;
import codeurjc_students.ATRA.service.MuralService;
import codeurjc_students.ATRA.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserService userService;
    @Autowired
	private ActivityService activityService;
    @Autowired
    private MuralService muralService;
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
        if (principal==null) return ResponseEntity.status(401).build();
        User user = userService.findById(id).orElse(null);
        if (user==null) return ResponseEntity.notFound().build();
        if (!user.getUsername().equals(principal.getName())) return ResponseEntity.status(403).build();
        if (receivedUser.getUsername()!=null && !receivedUser.getUsername().isEmpty() && !userService.existsByUsername(receivedUser.getUsername())) {
            user.setUsername(receivedUser.getUsername());
        }
        if (receivedUser.getName()!=null && !receivedUser.getName().isEmpty()) {
            user.setName(receivedUser.getName());
        }
        if (receivedUser.getEmail()!=null && !receivedUser.getEmail().isEmpty()) {
            user.setEmail(receivedUser.getEmail());
        }
        this.userService.save(user);
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal){
        try {
            User user = this.principalVerification(principal);
            return ResponseEntity.ok(userService.toDTO(user));
        } catch (HttpException e) {
            System.out.println("ERROR: current user is not authenticated or does not exist");
            return ResponseEntity.status(e.getStatus()).build();
        }
    }

    @GetMapping("/IsUsernameTaken")
    public ResponseEntity<Boolean> isUsernameTaken(@RequestParam String username){
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody String password, Principal principal){
        if (principal==null) return ResponseEntity.status(401).build();
        User user = userService.findByUserName(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(passwordEncoder.matches(password, user.getPassword()));
    }

    @PostMapping("/password")
    public ResponseEntity<String> changePassword(@RequestBody String password, Principal principal){
        try {
            User user = this.principalVerification(principal);
            user.setPassword(passwordEncoder.encode(password));
            userService.save(user);
            return ResponseEntity.noContent().build();
        } catch (HttpException e) {
            return ResponseEntity.status(e.getStatus()).build();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody NewUserDTO userDTO){
        if (!isValidUser(userDTO)) return ResponseEntity.badRequest().build();

        User user = new User(userDTO.getUsername(), passwordEncoder.encode(userDTO.getPassword()));

        if (userDTO.hasEmail()) user.setEmail(userDTO.getEmail());
        if (userDTO.hasDisplayName())
            user.setName(userDTO.getName());
        else
            user.setName(userDTO.getUsername());

        userService.save(user);
        return ResponseEntity.ok(user);
    }

    public ResponseEntity<User> modifyUser(){return null;}

    @DeleteMapping
    public ResponseEntity<User> deleteCurrentlyAuthenticatedUser(Principal principal){
        User user = this.principalVerification(principal);

        for (Route r : new ArrayList<>(user.getCreatedRoutes())) {
            if (r.getVisibility().isMuralSpecific() || r.getVisibility().isMuralPublic()) { //in theory, routes can't be mural public, but just in case
                r.setVisibility(new Visibility(VisibilityType.PUBLIC));
                user.removeRoute(r);
            }
        }
        muralService.findOwnedBy(user).forEach(Mural::removeOwner);
        deletionService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }

    // <editor-fold desc="Auxiliary Methods">
    private boolean isValidUser(NewUserDTO user){
        if (user.getUsername()==null || user.getPassword()==null) return false;
        if (userService.existsByUsername(user.getUsername())) return false;
        //other conditions
        return true;
    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
    }
    // <editor-fold>

}

