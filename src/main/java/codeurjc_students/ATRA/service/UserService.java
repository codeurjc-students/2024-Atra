package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.dto.UserDTO;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

	@Autowired
	private UserRepository repository;


	public Optional<User> findById(long id) {
		return repository.findById(id);
	}

	public Optional<User> findByUserName(String name) {
		return repository.findByUsername(name);
	}
	public boolean exists(long id) {
		return repository.existsById(id);
	}
	public boolean existsByUsername(String username) {
		return repository.findByUsername(username).isPresent();
	}

	public List<User> findAll() {
		return repository.findAll();
	}

	public void save(User user) {
		repository.save(user);
	}

	public void delete(long id) {
		repository.deleteById(id);
	}

    public UserDTO toDTO(User user) {
		UserDTO dto = new UserDTO();
		dto.setId(user.getId());
		dto.setDisplayname(user.getDisplayname());
		dto.setUsername(user.getUsername());
		dto.setRoles(user.getRoles());

		dto.setEmail(user.getEmail());
		return dto;
    }
}
