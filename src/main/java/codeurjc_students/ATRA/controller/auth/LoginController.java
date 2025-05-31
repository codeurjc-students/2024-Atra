package codeurjc_students.ATRA.controller.auth;


import codeurjc_students.ATRA.security.jwt.AuthResponse;
import codeurjc_students.ATRA.security.jwt.AuthResponse.Status;
import codeurjc_students.ATRA.security.jwt.LoginRequest;
import codeurjc_students.ATRA.security.jwt.UserLoginService;
import codeurjc_students.ATRA.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

	@Autowired
	private UserLoginService userLoginService;
	@Autowired
	private UserService userService;

	@PostMapping("/login")
	@Operation(summary = "User login", description = "Authenticates a user with username and password.")
	@ApiResponse(responseCode = "200", description = "Authentication successful",
			content = @Content(schema = @Schema(implementation = AuthResponse.class)))
	public ResponseEntity<AuthResponse> login(
			@CookieValue(name = "accessToken", required = false) String accessToken,
			@CookieValue(name = "refreshToken", required = false) String refreshToken,
			@RequestBody LoginRequest loginRequest) {
		return userLoginService.login(loginRequest, accessToken, refreshToken);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Generate new Access token", description = "Generate and return a new Access token if the provided Refresh token is valid")
	@ApiResponse(responseCode = "200", description = "Token refreshed successfully",
			content = @Content(schema = @Schema(implementation = AuthResponse.class)))
	public ResponseEntity<AuthResponse> refreshToken(
			@CookieValue(name = "refreshToken", required = false) String refreshToken) {
		return userLoginService.refresh(refreshToken);
	}

	@PostMapping("/logout")
	@Operation(summary = "User logout", description = "Logs out the user and clears the tokens.")
	@ApiResponse(responseCode = "200", description = "Logout successful",
			content = @Content(schema = @Schema(implementation = AuthResponse.class)))
	public ResponseEntity<AuthResponse> logOut(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, userLoginService.logout(request, response)));
	}
}
