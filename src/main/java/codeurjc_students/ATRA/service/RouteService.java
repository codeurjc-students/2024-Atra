package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Coordinates;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.repository.RouteRepository;
import jakarta.transaction.Transactional;
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

	public Route newRoute(Activity activity, ActivityService activityService) {
		return this.newRoute(new Route(), activity, activityService);
	}
	public Route newRoute(Route route, Activity activity, ActivityService activityService) {
		if (route == null) {
			route = new Route();
		}
		route.setCoordinates(Coordinates.fromActivity(activity));

		if (route.getName()==null || route.getName().isEmpty()){
			route.setName("Route from Activity " + activity.getId());
		}
		if (route.getDescription()==null || route.getDescription().isEmpty()){
			route.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus auctor ligula sit amet fermentum ornare. Integer mauris justo, fermentum et arcu ac, vulputate ultrices metus.");
		}
		if (route.getTotalDistance()==null || route.getTotalDistance()==0) {
			route.setTotalDistance(activityService.totalDistance(activity));
		}
		if (route.getElevationGain()==null || route.getElevationGain()==0) {
			route.setElevationGain(activityService.elevationGain(activity));
		}

		route.addActivity(activity);
		route.setId(null);
		this.save(route);

		activity.setRoute(route);
		activityService.save(activity);
		return route;
	}

	@Transactional
	void addRouteToActivity(String routeName, Activity activity, ActivityService activityService) {
		if (!repository.existsByName(routeName)) {
			Route route = new Route();
			route.setName(routeName);
			this.newRoute(route, activity, activityService);
		}
		else {
			List<Route> routes = repository.findByName(routeName);
			Route route = routes.get(0); //safe because we're in the else
			activity.setRoute(route);
			route.addActivity(activity);
			activityService.save(activity);
			this.save(route);
		}
	}
}
