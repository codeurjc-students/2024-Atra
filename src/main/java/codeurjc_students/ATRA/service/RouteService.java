package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

	@Autowired
	private RouteRepository repository;

	public Optional<Route> findById(long id) {
		return repository.findById(id);
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public List<Route> findAll() {
		return repository.findAll();
	}

	public void save(Route user) {
		repository.save(user);
	}

	/**
	 * DeletionService.deleteRoute(Long id) should be called instead.
	 * @param id
	 */
	void delete(long id) {
		repository.deleteById(id);
	}

    public void removeActivityFromRoute(Activity activity, Route route) {
		if (route==null) return;
		route.removeActivity(activity);
		save(route);
	}
}
