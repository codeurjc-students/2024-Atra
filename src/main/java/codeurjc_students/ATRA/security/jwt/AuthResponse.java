package codeurjc_students.ATRA.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
