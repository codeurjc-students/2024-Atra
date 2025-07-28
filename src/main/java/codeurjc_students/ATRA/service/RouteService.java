package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.RouteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteService implements ChangeVisibilityInterface{

	@Autowired
	private RouteRepository repository;

	public Optional<Route> findById(long id) {
		return repository.findById(id);
	}

	public List<Route> findById(List<Long> ids) {
		List<Route> result = new ArrayList<>();
		for (var id : ids) {
			Optional<Route> actOpt = findById(id);
			actOpt.ifPresent(result::add);
		}
		return result;
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
		if (route.getOwner()==null){
			route.setOwner(activity.getUser());
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
	void addRouteToActivity(Route route, Activity activity, ActivityService activityService) {
		if (route==null) {
			throw new RuntimeException("Can't add nonexistent route to activity");
		}
		if (route.getCoordinates()==null || route.getCoordinates().isEmpty()) {
			route.setCoordinates(Coordinates.fromActivity(activity));
		}
		activity.setRoute(route);
		route.addActivity(activity);
		activityService.save(activity);
		this.save(route);
	}

	/**
	 *
	 * @param routeId
	 * @param newVisibility
	 * @return false if routeId doesn't match any existing activities, true otherwise
	 */
	public boolean changeVisibility(Long routeId, VisibilityType newVisibility){ //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		return changeVisibility(routeId,newVisibility,null);
	}
	public boolean changeVisibility(Long routeId, VisibilityType newVisibility, Collection<Long> allowedMuralsCol) { //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		Route route = repository.findById(routeId).orElseThrow(()->new HttpException(404));
		Visibility currentVis = route.getVisibility();
		if (currentVis.isPublic()) throw new HttpException(422, "Cannot change visibility of a public route");

		HashSet<Long> allowedMurals = allowedMuralsCol == null ? new HashSet<>() : new HashSet<>(allowedMuralsCol);

		if (newVisibility==VisibilityType.PRIVATE) {
			User owner = route.getOwner();
			if (route.getActivities().stream().anyMatch(activity -> !owner.getId().equals(activity.getUser().getId()))) {
				throw new HttpException(422, "Cannot change visibility of a route that other users are using.");
			}
		} else if (newVisibility==VisibilityType.MURAL_SPECIFIC) { //visibility increased
			//add to all murals in allowedMurals ?? userMurals
			route.getOwner().getMemberMurals().forEach(mural -> {
				if (allowedMurals.contains(mural.getId()) && !route.getVisibility().getAllowedMurals().contains(mural.getId()))
					mural.addRoute(route);
				if (!allowedMurals.contains(mural.getId()) && route.getVisibility().getAllowedMurals().contains(mural.getId()))
					mural.removeRoute(route);
				//haría falta guardarlos con muralService.save(mural), no?
				//potencialmente se podría usar
				//@PersistenceContext
				//  private EntityManager entityManager;
				// entityManager.flush()
			});

		} else if (newVisibility == VisibilityType.PUBLIC) {
			route.setOwner(null);
		}
		route.changeVisibilityTo(newVisibility, allowedMurals);
		repository.save(route);
		return true;
	}

	public List<Route> findVisibleTo(User user) {
		Set<Route> routes = new HashSet<>(user.getCreatedRoutes()); //your own
		routes.addAll(repository.findByVisibilityType(VisibilityType.PUBLIC)); //public ones
		if (user.hasRole("ADMIN")) routes.addAll(repository.findByVisibilityType(VisibilityType.MURAL_SPECIFIC)); //and semi-public ones if admin
		return new ArrayList<>(routes);
	}

	public List<Route> findVisibleTo(Mural mural) {
		List<Route> visibleToMural = repository.findVisibleToMural(mural.getId());
		Set<Route> routes = new HashSet<>(mural.getRoutes());
		routes.addAll(repository.findByVisibilityType(VisibilityType.PUBLIC));
		List<Route> returnValue = new ArrayList<>(routes);
		if (!returnValue.equals(visibleToMural)) throw new RuntimeException("Custom query and manual fetch disagree");
		//findVisibleToMural is a custom query. It should do the same thing as the rest of the code does
		// if its result disagrees with returnValue, it should be checked.
		// If it consistently returns the correct data, then it should be used instead of calculating it manually. And potentially consider doing the same thing with findVisibleTo(User)
		return returnValue;
	}

	public boolean isVisibleBy(Route route, User user) {
        return (route.getOwner() == null || route.getVisibility().isPublic())
				|| route.getOwner().equals(user)
				|| (user.hasRole("ADMIN") && !route.getVisibility().isPrivate());
    }

	public boolean isVisibleBy(Route route, Mural mural) {
		return (route.getVisibility().isPublic())
				|| mural.getRoutes().contains(route);
	}
}
