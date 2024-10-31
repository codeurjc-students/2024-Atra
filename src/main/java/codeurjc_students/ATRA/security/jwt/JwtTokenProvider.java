package codeurjc_students.ATRA.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * POJO that provides functionality regarding JWTs.
 * Allows creation and validation of tokens, as well as methods to extract a token from a HttpServletRequest, and to extract user data from a token (username/Authentication)
 */
@Component
public class JwtTokenProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);
	
	@Value("${jwt.secret}")
	private String jwtSecret;

	private SecretKey secretKey;
	
	private static long JWT_EXPIRATION_IN_MS = 5400000;
	private static Long REFRESH_TOKEN_EXPIRATION_MSEC = 10800000l;
	
	@Autowired
	private UserDetailsService userDetailsService;

	/**
	 * Generate a HMAC key and store it in an attribute. This is necessary for Jwts to work properly.
	 */
	private void setSigningKey() {
		byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
		secretKey = Keys.hmacShaKeyFor(keyBytes);  // Generate HMAC key
	}

	public Authentication getAuthentication(String token) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(getUsername(token));
		return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
	}

	public String getUsername(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
	}

	/**
	 * Extracts a token from a request.
	 * @param req the request from which to extract a token
	 * @return a String holding the token or null if there was none
	 */
	public String resolveToken(HttpServletRequest req) {
		String bearerToken = req.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7, bearerToken.length());
		}
		return null;
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(secretKey) 		//define what key to decrypt with
					.build()					//create the parser (it was a builder)
					.parseSignedClaims(token); 	//verify it is valid (and do some other stuff we don't care about)
			return true;
		} catch (SignatureException ex) {
			LOG.debug("Invalid JWT Signature");
		} catch (MalformedJwtException ex) {
			LOG.debug("Invalid JWT token");
		} catch (ExpiredJwtException ex) {
			LOG.debug("Expired JWT token");
		} catch (UnsupportedJwtException ex) {
			LOG.debug("Unsupported JWT exception");
		} catch (IllegalArgumentException ex) {
			LOG.debug("JWT claims string is empty");
		}
		return false;
	}

	public Token generateToken(UserDetails user) {
		return buildToken(user, true);
	}

	public Token generateRefreshToken(UserDetails user) {
		return buildToken(user, false);
	}

	private Token buildToken(UserDetails user, boolean isAccess){
		ClaimsBuilder claims = Jwts.claims().subject(user.getUsername());

		claims.add("auth", user.getAuthorities().stream().map(s -> new SimpleGrantedAuthority("ROLE_"+s))
				.filter(Objects::nonNull).collect(Collectors.toList()));

		Date now = new Date();
		Long duration = now.getTime() + (isAccess ? JWT_EXPIRATION_IN_MS : REFRESH_TOKEN_EXPIRATION_MSEC);
		Date expiryDate = new Date(now.getTime() + (isAccess ? JWT_EXPIRATION_IN_MS : REFRESH_TOKEN_EXPIRATION_MSEC));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		calendar.add(Calendar.HOUR_OF_DAY, 8);

		String token = Jwts.builder().claims(claims.build()).subject((user.getUsername())).issuedAt(new Date())
				.expiration(expiryDate).signWith(secretKey).compact();

		return new Token((isAccess ? Token.TokenType.ACCESS : Token.TokenType.REFRESH), token, duration,
				LocalDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault()));

	}
}
