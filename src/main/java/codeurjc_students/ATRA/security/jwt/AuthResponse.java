package codeurjc_students.ATRA.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO holding String attributes for status, message and error. Used as the body of ResponseEntity during authentication.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

	private Status status;
	private String message;
	private String error;

	public enum Status {
		SUCCESS, FAILURE
	}

	public AuthResponse(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String toString() {
		return "LoginResponse [status=" + status + ", message=" + message + ", error=" + error + "]";
	}

}
