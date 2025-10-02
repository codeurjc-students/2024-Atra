package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.dto.NewUserDTO;
import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.exception.EntityNotFoundException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.exception.PermissionException;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RouteRepository routeRepository;
	@Autowired
	private MuralRepository muralRepository;
	@Autowired
	private ActivityRepository activityRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;


	public Optional<User> findByUserName(String name) {
		return userRepository.findByUsername(name);
	}
	public boolean existsByUsername(String username) {
		return userRepository.findByUsername(username).isPresent();
	}

	protected void save(User user) {
		userRepository.save(user);
	}

	public User patchUser(User authUser, Long userId, UserDTO receivedUser) {
		User user = userRepository.findById(userId).orElseThrow(()-> new EntityNotFoundException("User not found"));
		if (!authUser.equals(user)) throw new PermissionException("User lacks permission to modify specified user");

		if (receivedUser.getUsername()!=null && !receivedUser.getUsername().isEmpty() && !this.existsByUsername(receivedUser.getUsername())) {
			user.setUsername(receivedUser.getUsername());
		}
		if (receivedUser.getName()!=null) {
			if (!receivedUser.getName().isEmpty())
				user.setName(receivedUser.getName());
			else
				user.setName(user.getUsername());
		}
		if (receivedUser.getEmail()!=null) {
			user.setEmail(receivedUser.getEmail());
		}
		userRepository.save(user);
		return user;
	}

	public void changePassword(User user, String password) {
		user.setPassword(passwordEncoder.encode(password));
		userRepository.save(user);
	}

	public User createUser(NewUserDTO userDTO) {
		if (userDTO.getUsername()==null || userDTO.getPassword()==null) throw new IncorrectParametersException("Could not parse the user. Make sure it has the correct fields");
		if (userDTO.getUsername().isEmpty() || userDTO.getPassword().isEmpty()) throw new IncorrectParametersException("Could not parse the user. Make sure it has the correct fields");
		if (userRepository.existsByUsername(userDTO.getUsername())) throw new IncorrectParametersException("Username is taken");

		User user = new User(userDTO.getUsername(), passwordEncoder.encode(userDTO.getPassword()));

		if (userDTO.hasEmail()) user.setEmail(userDTO.getEmail());
		if (userDTO.hasDisplayName())
			user.setName(userDTO.getName());
		else
			user.setName(userDTO.getUsername());

		userRepository.save(user);
		return user;
	}

	public void deleteUser(User user) {
		if (user==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

		activityRepository.deleteAll(activityRepository.findByOwner(user));

		for (Route r : routeRepository.findAllByCreatedBy(user)) {
			if (r.getVisibility().isMuralSpecific() || r.getVisibility().isMuralPublic()) { //in theory, routes can't be mural public, but just in case
				r.setVisibility(new Visibility(VisibilityType.PUBLIC));
				routeRepository.save(r);
			}
		}
		muralRepository.findByOwner(user).forEach(m-> {
			m.removeOwner();
			muralRepository.save(m);
		});
		user.getMemberMurals().forEach(mural -> {
			mural.removeMember(user);
			muralRepository.save(mural);
		});
		userRepository.delete(user);
	}

	public User getUser(Long id) {
		return  userRepository.findById(id).orElseThrow(()->new EntityNotFoundException("User not found"));
	}

	public Boolean verifyPassword(String password, String password1) {
		return passwordEncoder.matches(password, password1);
	}
}
