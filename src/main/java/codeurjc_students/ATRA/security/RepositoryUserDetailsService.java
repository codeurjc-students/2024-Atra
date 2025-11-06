package codeurjc_students.atra.security;

import codeurjc_students.atra.model.User;
import codeurjc_students.atra.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements UserDetailsService. This means it is used when authenticating a user to get the user's data.
 * When authenticating a user, Spring compares the user/password in the request to those in the database using an AuthenticationManager.
 * However, it does not have direct access to the database.
 * Instead, it uses a UserDetailsService to get the user's data from their username.
 * This is the implementation of UserDetailsService used for this project.
 */
@Service
@RequiredArgsConstructor
public class RepositoryUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		List<GrantedAuthority> roles = new ArrayList<>();
		for (String role : user.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		return new org.springframework.security.core.userdetails.User(user.getUsername(),
				user.getPassword(), roles);
	}
}
