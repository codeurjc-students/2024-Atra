package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MuralService {

	@Autowired
	private MuralRepository repository;

	public Optional<Mural> findById(long id) {
		return repository.findById(id);
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public List<Mural> findAll() {
		return repository.findAll();
	}

	public void save(Mural user) {
		repository.save(user);
	}

	/**
	 * DeletionService.deleteMural(Long id) should be called instead.
	 * @param id
	 */
	void delete(long id) {
		repository.deleteById(id);
	}
}
