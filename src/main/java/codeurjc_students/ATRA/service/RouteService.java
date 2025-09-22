package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.exception.*;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import codeurjc_students.ATRA.service.auxiliary.AtraUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteService implements ChangeVisibilityInterface{

	@Autowired
	private RouteRepository routeRepository;
	@Autowired
	private ActivityRepository activityRepository;
	@Autowired
	private MuralRepository muralRepository;
	@Autowired
	private UserRepository userRepository;

	public Optional<Route> findById(long id) {
		return routeRepository.findById(id);
	}

	public boolean exists(long id) {
		return routeRepository.existsById(id);
	}

	public void save(Route user) {
		routeRepository.save(user);
	}

	public Route newRoute(Activity activity) {
		return this.newRoute(new Route(), activity);
	}
	public Route newRoute(Route route, Activity activity) {
		ActivitySummary summary = activity.getSummary();
		if (route == null) {
			route = new Route();
		}
		route.setCoordinates(Coordinates.fromActivity(activity));

		if (route.getName()==null || route.getName().isEmpty()){
			route.setName("Route from Activity " + activity.getId());
		}
		if (route.getCreatedBy()==null){
			route.setCreatedBy(activity.getUser());
		}
		if (route.getDescription()==null || route.getDescription().isEmpty()){
			route.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus auctor ligula sit amet fermentum ornare. Integer mauris justo, fermentum et arcu ac, vulputate ultrices metus.");
		}
		if (route.getTotalDistance()==null || route.getTotalDistance()==0) {
			route.setTotalDistance(summary.getTotalDistance());
		}
		if (route.getElevationGain()==null || route.getElevationGain()==0) {
			route.setElevationGain(summary.getElevationGain());
		}

		route.setId(null);
		this.save(route);

		activity.setRoute(route);
		activityRepository.save(activity);
		return route;
	}

	@Transactional
	void addRouteToActivity(Route route, Activity activity, ActivityService activityService) {
		if (route==null) throw new RuntimeException("Can't add nonexistent route to activity");
		if (route.getCoordinates()==null || route.getCoordinates().isEmpty()) {
			route.setCoordinates(Coordinates.fromActivity(activity));
			this.save(route);
		}
		activity.setRoute(route);
		activityService.save(activity);
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
	public boolean changeVisibility(Long routeId, Visibility newVisibility){ //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		return changeVisibility(routeId,newVisibility.getType(),newVisibility.getAllowedMurals());
	}
	public boolean changeVisibility(Long routeId, VisibilityType newVisibility, Collection<Long> allowedMuralsCol) { //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		Route route = routeRepository.findById(routeId).orElseThrow(()->new EntityNotFoundException("Could not find the route with id " + routeId + " so the change visibility operation has been canceled"));
		Visibility currentVis = route.getVisibility();
		if (currentVis.isPublic()) throw new IncorrectParametersException("Cannot change visibility of a public route");

		HashSet<Long> allowedMurals = allowedMuralsCol == null ? new HashSet<>() : new HashSet<>(allowedMuralsCol);

		if (newVisibility==VisibilityType.PRIVATE) {
			User owner = route.getCreatedBy();
			if (activityRepository.findByRoute(route).stream().anyMatch(activity -> !owner.getId().equals(activity.getUser().getId()))) {
				throw new IncorrectParametersException("Cannot change visibility of a route that other users are using.");
			}
		}
		route.changeVisibilityTo(newVisibility, allowedMurals);
		routeRepository.save(route);
		return true;
	}

	public List<Route> findVisibleTo(Mural mural) {
        return routeRepository.findVisibleToMural(mural.getId());
	}

	public List<Route> findByCreatedByAndVisibilityTypeIn(User user, List<VisibilityType> visibilityTypes) {
		return routeRepository.findByCreatedByAndVisibilityTypeIn(user, visibilityTypes);
	}
	public List<Route> findByOwnerAndVisibilityType(User user, String visibility) {
		VisibilityType visibilityType;
		try {visibilityType = VisibilityType.valueOf(visibility.toUpperCase(Locale.ROOT));}
		catch (IllegalArgumentException e) {throw new HttpException(400, "Visibility specified is not a valid VisibilityType. Valid values are PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC. \"");		}
		return routeRepository.findByCreatedByAndVisibilityTypeIn(user, List.of(visibilityType));
	}

	public List<Route> findUsedOrCreatedBy(User user) {
		return routeRepository.findUsedOrCreatedBy(user);
	}

	public Collection<Activity> getActivitiesAssignedToRoute(Long routeId, User user, Long muralId) {
		Route route = this.findById(routeId).orElseThrow(() -> new HttpException(404, "No route with id " + routeId));
		Optional<Mural> muralOpt = muralRepository.findById(muralId);
		if (!AtraUtils.isRouteVisibleByUserOrOwnedMurals(route, user)) throw new HttpException(403, "Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info
		if (muralOpt.isPresent() && !muralOpt.get().getMembers().contains(user)) throw new HttpException(403, "User is not a member of specified mural. You need to be a member of a mural in order to view data bound to it.");

		if (muralOpt.isEmpty()) {
			return activityRepository.findByRouteAndUser(route, user);
		}
		return activityRepository.findByRouteAndMural(route, muralOpt.get().getId());
	}

    public Route getRoute(User user, Long id) {
		Route route = this.findById(id).orElseThrow(() -> new EntityNotFoundException("No route with id " + id));
		if (!AtraUtils.isRouteVisibleByUserOrOwnedMurals(route, user)) throw new VisibilityException("Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info
		return route;
	}

	public List<Route> getAllRoutes(User user, String type, String from, Long id, String visibility) {
		//For mural return all mural_specific and mural_public it sees
		//for public ones, return only if at least one user has an activity in it

		//for user return all created by them, and all public such that he has an activity there
		List<Route> routes;
		if (from==null || "authUser".equals(from)) routes = this.findUsedOrCreatedBy(user);
		else if ("user".equals(from)){
			User targetUser = userRepository.findById(id).orElseThrow(()-> new HttpException(404, "Target user not found"));
			if (!user.equals(targetUser) && !user.isAdmin()) {//return public activities
				routes = this.findByCreatedByAndVisibilityTypeIn(user, List.of(VisibilityType.PUBLIC));
			} else if (user.equals(targetUser)) {
				if (visibility==null) routes = this.findUsedOrCreatedBy(user);
				else routes = this.findByOwnerAndVisibilityType(user, visibility);
			} else if (user.isAdmin()) {
				if (visibility==null) routes = this.findByCreatedByAndVisibilityTypeIn(targetUser, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC));
				else if ("PRIVATE".equals(visibility)) throw new HttpException(400, "No one can see private routes except for the user who created them.");
				else routes = this.findByOwnerAndVisibilityType(user, visibility);
			}
			else throw new HttpException(500, "How'd you even get here? I think you only go here when targetUser is null, but that can only happen if id is null, which has been checked."); //I think this only happens with targetUser==null
		}
		else if ("mural".equals(from)){
			Mural mural = muralRepository.findById(id).orElseThrow(() -> new HttpException(404, "Requested mural doesn't exist"));
			routes = this.findVisibleTo(mural);
		} else throw new HttpException(400, "Specified value for 'from' ("+from+") is invalid. Valid values are: 'authUser', 'user', 'mural'");
		return routes;
	}

	public Route createRoute(User user, Long activityId) {
		Activity activity = activityRepository.findById(activityId).orElseThrow(()->new EntityNotFoundException("Could not find the Activity specified."));
		return this.newRoute(activity);

	}

	public void addActivitiesToRoute(User user, Long routeId, List<Long> activityIds) {

		Route route = routeRepository.findById(routeId).orElseThrow(()->new EntityNotFoundException("Route not found"));
		if (!AtraUtils.isRouteVisibleBy(route,user)) throw new VisibilityException("User has no visibility of this route"); //404 might be better, more secure, gives less info. (security)
		List<Activity> activities = activityRepository.findAllById(activityIds);
		if (activities.isEmpty()) throw new EntityNotFoundException("No activities found with specified ids.");

		//Confirmed that route and activity exists, and that they're related
		for (var act : activities) {
			if (!user.equals(act.getUser())) continue;
			act.setRoute(route);
			activityRepository.save(act);
		}
		routeRepository.save(route);
	}

	public void deleteRoute(User user, Long id) {
		Route route = routeRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Route not found"));
		if (route.getCreatedBy()!=user && !user.isAdmin()) throw new PermissionException("You cannot delete a route you are not the owner of. Public routes can only be deleted by administrators");

		if (route.getVisibility().isPublic() && !user.isAdmin()) throw new PermissionException("Public routes can only be deleted by administrators"); //This is indirectly checked above by route.getOwner()!=user, since owner will be null for public routes
		if (route.getVisibility().isMuralSpecific() &&
				activityRepository.findByRoute(route).stream().anyMatch(a->a.getUser()!=user)) {
			throw new IncorrectParametersException("Cannot delete a route that other people are using");
		}
		for (var act : activityRepository.findByRoute(route)) {
			act.setRoute(null);
			activityRepository.save(act);
		}

		this.deleteRouteConsequences(id);
	}

	public void changeVisibility(User user, Long id, Visibility newVisibility) {
		Route route = routeRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Route not found"));
		if (route.getVisibility().isPublic()) throw new IncorrectParametersException("The visibility of a public route cannot be changed");
		if (!user.getId().equals(route.getCreatedBy().getId())) throw new PermissionException("You don't have the necessary permissions to change the visibility of this route.");

		this.changeVisibility(id, newVisibility); //throws error on not found or invalid visibility
	}

	public void makeRoutesNotVisibleToMural(User user, Long muralId, List<Long> selectedRoutesIds) {
		if (!muralRepository.existsById(muralId)) throw new EntityNotFoundException("Mural not found");

		List<Route> routes = routeRepository.findAllById(selectedRoutesIds);
		routes.forEach(route -> {
			if (!route.getVisibility().isMuralSpecific()) return;
			if (!user.equals(route.getCreatedBy()) && !user.isAdmin()) return;
			route.getVisibility().removeMural(muralId);

			routeRepository.save(route);
		});
	}

	public Collection<Route> getOwnedRoutesInMural(User user, Long muralId) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(() -> new EntityNotFoundException("Mural not found"));
		if (!mural.getMembers().contains(user)) throw new PermissionException("Only mural members can access this data.");

		Collection<Route> result = new ArrayList<>();
		routeRepository.findAllByCreatedBy(user).forEach(route -> {
			if (AtraUtils.isRouteVisibleBy(route, mural)) result.add(route);
		});
		return result;
	}

	private void deleteRouteConsequences(long id) {
		Route route = routeRepository.findById(id).orElse(null);
		if (route==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

		activityRepository.findByRoute(route).forEach(activity -> {
			activity.removeRoute();
			activityRepository.save(activity);
		});

		routeRepository.deleteById(id);
	}
}
