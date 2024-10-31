package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

	@Autowired
	private ActivityRepository repository;


	public Optional<Activity> findById(long id) {
		return repository.findById(id);
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public List<Activity> findAll() {
		return repository.findAll();
	}

	public void save(Activity activity) {
		repository.save(activity);
	}

	public void delete(long id) {
		repository.deleteById(id);
	}
}
