package codeurjc_students.atra.controller;

import codeurjc_students.atra.dto.NewUserDTO;
import codeurjc_students.atra.dto.UserDTO;
import codeurjc_students.atra.exception.HttpException;
import codeurjc_students.atra.model.User;
import codeurjc_students.atra.service.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

	private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a user's information by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> getUser(
        @Parameter(description = "User ID") @PathVariable Long id){
        return ResponseEntity.ok(new UserDTO(userService.getUser(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user", description = "Partially update a user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> patchUser(
        Principal principal, 
        @Parameter(description = "User ID") @PathVariable Long id, 
        @RequestBody UserDTO receivedUser){
        User authUser = principalVerification(principal);
        User resultUser = userService.patchUser(authUser, id, receivedUser);
        return ResponseEntity.ok(new UserDTO(resultUser));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieve the authenticated user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Current user information retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal){
        User user = principalVerification(principal);
        return ResponseEntity.ok(new UserDTO(user));
    }

    @GetMapping("/IsUsernameTaken")
    @Operation(summary = "Check username availability", description = "Check if a username is already taken")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Username availability checked")
    })
    public ResponseEntity<Boolean> isUsernameTaken(
        @Parameter(description = "Username to check") @RequestParam String username){
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @PostMapping("/verify-password")
    @Operation(summary = "Verify password", description = "Verify if the provided password matches the authenticated user's password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password verification result returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<Boolean> verifyPassword(
        Principal principal, 
        @RequestBody String password){
        User user = principalVerification(principal);
        return ResponseEntity.ok(userService.verifyPassword(password, user.getPassword()));
    }

    @PostMapping("/password")
    @Operation(summary = "Change password", description = "Change the authenticated user's password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Password changed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<String> changePassword(
        @RequestBody String password, 
        Principal principal){
        User user = principalVerification(principal);
        userService.changePassword(user, password);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user data")
    })
    public ResponseEntity<User> createUser(@RequestBody NewUserDTO userDTO){
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping
    @Operation(summary = "Delete current user", description = "Delete the authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
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

