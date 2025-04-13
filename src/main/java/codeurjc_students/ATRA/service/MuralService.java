package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MuralService {

	@Autowired
	private MuralRepository repository;

	@Autowired
	private UserService userService; //a bit sketchy, I fear circular dependencies

	public Optional<Mural> findById(long id) {
		return repository.findById(id);
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public List<Mural> findAll() {
		return repository.findAll();
	}

	public void save(Mural mural) {
		repository.save(mural);
	}

	/**
	 * DeletionService.deleteMural(Long id) should be called instead.
	 * @param id
	 */
	void delete(long id) {
		repository.deleteById(id);
	}

	public void newMural(Mural mural) {
		repository.save(mural);

		User owner = mural.getOwner();
		owner.getOwnedMurals().add(mural);
		userService.save(owner);
		mural.getMembers().forEach(user -> {
			user.getMemberMurals().add(mural);
			userService.save(user);
		});
	}

	public Collection<Mural> findOther(List<Mural> memberMurals) {
		Set<Mural> hashSet = new HashSet<>(findAll());
		memberMurals.forEach(hashSet::remove);
		return hashSet;
	}
}
