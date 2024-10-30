package codeurjc_students.ATRA.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


/**
 * UserLoginService, as the name suggests, is the Service used during the login/logout process.
 * It handles login, logout, token refresh, and provides a method to access the currently authenticated username.
 * It makes use of a JwtTokenProvider to create and validate Access and Refresh tokens.
 */
@Service
@Lazy
public class UserLoginService {

	/**
	 * It is used to authenticate the user. It makes use of our UserDetailsService implementation to do so.
	 */
	@Autowired
	private AuthenticationManager authenticationManager;

	/**
	 * Used to fetch the user's data (as a UserDetails) from their username
	 */
	@Autowired
	private UserDetailsService userDetailsService;

	/**
	 * Used to validate and create Access and Refresh tokens.
	 * This is a custom class not native to Spring.
	 */
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	/**
	 * It is used to create cookies to hold the Access and Refresh tokens.
	 * This is a custom class not native to Spring.
	 */
	@Autowired
	private JwtCookieManager cookieUtil;

	/**
	 * Attempts to log the user in with their username and password. If authentication fails, an exception is thrown.
	 * If it goes through, tokens are refreshed, and a success response is sent.
	 * @param loginRequest custom class that holds the username and password to authenticate
	 * @param encryptedAccessToken holds the user's access token, if they have one
	 * @param encryptedRefreshToken holds the user's refresh token, if they have one
	 * @return a ResponseEntity. Its headers hold cookies with any new tokens. Its body is an AuthResponse indicating success.
	 */
	public ResponseEntity<AuthResponse> login(LoginRequest loginRequest, String encryptedAccessToken, String 
			encryptedRefreshToken) {
		
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		String accessToken = SecurityCipher.decrypt(encryptedAccessToken);
		String refreshToken = SecurityCipher.decrypt(encryptedRefreshToken);
		
		String username = loginRequest.getUsername();
		UserDetails user = userDetailsService.loadUserByUsername(username);

		boolean accessTokenValid = jwtTokenProvider.validateToken(accessToken);
		boolean refreshTokenValid = jwtTokenProvider.validateToken(refreshToken);

		HttpHeaders responseHeaders = new HttpHeaders();
		Token newAccessToken;
		Token newRefreshToken;
		if (accessTokenValid == refreshTokenValid) {
			newAccessToken = jwtTokenProvider.generateToken(user);
			newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
			addAccessTokenCookie(responseHeaders, newAccessToken);
			addRefreshTokenCookie(responseHeaders, newRefreshToken);
		}

		if (!accessTokenValid && refreshTokenValid) {
			newAccessToken = jwtTokenProvider.generateToken(user);
			addAccessTokenCookie(responseHeaders, newAccessToken);
		}

		AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.SUCCESS,
				"Auth successful. Tokens are created in cookie.");
		return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
	}

	/**
	 * If the refresh token received is valid, generate a new access token.
	 * @param encryptedRefreshToken
	 * @return a ResponseEntity. If successful, its headers will hold a cookie with the new access token, and the body a successful AuthResponse. If failed, empty headers, and a failure AuthResponse as body.
	 */
	public ResponseEntity<AuthResponse> refresh(String encryptedRefreshToken) {
		
		String refreshToken = SecurityCipher.decrypt(encryptedRefreshToken);
		
		boolean refreshTokenValid = jwtTokenProvider.validateToken(refreshToken);
		
		if (!refreshTokenValid) {
			AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.FAILURE,
					"Invalid refresh token !");
			return ResponseEntity.ok().body(loginResponse);
		}

		String username = jwtTokenProvider.getUsername(refreshToken);
		UserDetails user = userDetailsService.loadUserByUsername(username);
				
		Token newAccessToken = jwtTokenProvider.generateToken(user);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil
				.createAccessTokenCookie(newAccessToken.getTokenValue(), newAccessToken.getDuration()).toString());

		AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.SUCCESS,
				"Auth successful. Tokens are created in cookie.");
		return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
	}

	/**
	 * Exposes the username of the currently authenticated user.
	 * @return the username of the currently authenticated user.
	 */
	public String getUserName() {
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		return authentication.getName();
	}

	/**
	 * Logs the user out by:
	 * 	1. Clearing the security context
	 * 	2. Invalidating the user's session (HttpSession) if there is one
	 * 	3. Deleting the user's cookies
	 * @param request Holds the user's session and cookies
	 * @param response Receives the new cookies with ttl=0, effectively eliminating them
	 * @return a String indicating success
	 */
	public String logout(HttpServletRequest request, HttpServletResponse response) {

		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				cookie.setMaxAge(0);
				cookie.setValue("");
				cookie.setHttpOnly(true);
				cookie.setPath("/");
				response.addCookie(cookie);
			}
		}

		return "logout successfully";
	}

	/**
	 * Adds the token to the HttpHeaders as an Access Token Cookie
	 * @param httpHeaders
	 * @param token
	 */
	private void addAccessTokenCookie(HttpHeaders httpHeaders, Token token) {
		httpHeaders.add(HttpHeaders.SET_COOKIE,
				cookieUtil.createAccessTokenCookie(token.getTokenValue(), token.getDuration()).toString());
	}

	/**
	 * Adds the token to the HttpHeaders as a Refresh Token Cookie
	 * @param httpHeaders
	 * @param token
	 */
	private void addRefreshTokenCookie(HttpHeaders httpHeaders, Token token) {
		httpHeaders.add(HttpHeaders.SET_COOKIE,
				cookieUtil.createRefreshTokenCookie(token.getTokenValue(), token.getDuration()).toString());
	}
}
