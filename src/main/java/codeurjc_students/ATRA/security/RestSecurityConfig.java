package codeurjc_students.ATRA.security;

import codeurjc_students.ATRA.security.jwt.JwtRequestFilter;

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

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService)
				.passwordEncoder(passwordEncoder());
	}

	//Expose AuthenticationManager as a Bean to be used in other services
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	//@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10, new SecureRandom());
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
			.securityMatcher("/api/**")
			.authorizeHttpRequests(authorize -> authorize
		
				// URLs that need authentication to access to it
				.requestMatchers(HttpMethod.POST, "/api/example/1").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/example/2").hasRole("USER")
				.requestMatchers(HttpMethod.GET, "/api/example/3").hasAnyRole("ADMIN", "USER")
				.requestMatchers(HttpMethod.GET, "/api/example/4").permitAll()

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
			.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
