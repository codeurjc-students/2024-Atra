package codeurjc_students.ATRA.security;

import codeurjc_students.ATRA.security.jwt.JwtRequestFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.SecureRandom;

/**
 * Configures security elements. Mainly:
 * 	1. Configures and exposes an AuthenticationManager. It will use RepositoryUserDetailsService to access user data, and a generic PasswordEncoder
 * 	2. Configures access permissions as well as security settings inside securityFilterChain, such as disabling CSRF and session management.
 */
@Configuration
public class RestSecurityConfig {

	private final String ADMIN_ROLE = "ADMIN";
	private final String USER_ROLE = "USER";

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService)
				.passwordEncoder(passwordEncoder); //NoOpPasswordEncoder is for testing purposes
	}

	//Expose AuthenticationManager as a Bean to be used in other services
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
			.securityMatcher("/api/**")
			.authorizeHttpRequests(authorize -> authorize

				// URLs that need authentication to access to it
					.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/auth/IsLoggedIn").permitAll()
					.requestMatchers(HttpMethod.POST, "/api/auth/refresh").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/auth/logout").hasAnyRole(USER_ROLE, ADMIN_ROLE)

					.requestMatchers(HttpMethod.DELETE, "/api/users").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/users/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/users/verify-password").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/users/password").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.PATCH, "/api/users/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/users/IsUsernameTaken").permitAll()
					.requestMatchers(HttpMethod.POST, "/api/users").anonymous() //can only create a user if not logged in

					.requestMatchers(HttpMethod.DELETE, "/api/activities/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/activities").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/activities/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.DELETE, "/api/activities/{id:[0-9]+}/route").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/activities").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/activities/{id:[0-9]+}/route").hasAnyRole(USER_ROLE, ADMIN_ROLE)

					.requestMatchers(HttpMethod.GET, "/api/murals/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/murals/{id:[0-9]+}/banner").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/murals/{id:[0-9]+}/thumbnail").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/murals").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/murals").hasAnyRole(USER_ROLE, ADMIN_ROLE)

					.requestMatchers(HttpMethod.DELETE, "/api/routes/{rid:[0-9]+}/activities/{aid:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/routes").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.DELETE, "/api/routes/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/routes/{id:[0-9]+}/activities/").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.POST, "/api/routes").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/routes/{id:[0-9]+}").hasAnyRole(USER_ROLE, ADMIN_ROLE)
					.requestMatchers(HttpMethod.GET, "/api/routes/{id:[0-9]+}/activities").hasAnyRole(USER_ROLE, ADMIN_ROLE)



				// Other URLs can be accessed without authentication
				.anyRequest().permitAll())

			// Disable CSRF protection (it is difficult to implement in REST APIs)
			.csrf(AbstractHttpConfigurer::disable) //csrf -> csrf.disable()

			// Disable Http Basic Authentication
			.httpBasic(AbstractHttpConfigurer::disable) //httpBasic -> httpBasic.disable()

			// Disable Form login Authentication
			.formLogin(AbstractHttpConfigurer::disable) //formLogin -> formLogin.disable()

			// Avoid creating session, since we are using JWT, which is stateless
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// Add JWT Token filter
			.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)

			// Return 401 when trying to access a protected endpoint without a session
			.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))); //it appears that when tokens time out the restSecurityConfig treats it as you not having appropriate permissions, which is technically true, but it returns 403 when it should be 401. This will, whenever the token is invalid/null, return a 401 in theory
		return http.build();
	}
}
