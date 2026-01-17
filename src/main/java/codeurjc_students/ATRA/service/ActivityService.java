package codeurjc_students.atra.service;

import codeurjc_students.atra.dto.ActivityEditDTO;
import codeurjc_students.atra.exception.*;
import codeurjc_students.atra.model.*;
import codeurjc_students.atra.model.auxiliary.*;
import codeurjc_students.atra.repository.*;
import codeurjc_students.atra.service.auxiliary.AtraUtils;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.WayPoint;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

import static codeurjc_students.atra.service.Constants.*;

@Service
@RequiredArgsConstructor
public class ActivityService implements ChangeVisibilityInterface{

	private final ActivityRepository activityRepository;
	private final RouteRepository routeRepository;
	private final ActivitySummaryRepository summaryRepository;
	private final UserRepository userRepository;
	private final MuralRepository muralRepository;

	public void save(Activity activity) {
		activityRepository.save(activity);
	}

	public Activity newActivity(MultipartFile file, User user){
		final GPX gpx;
        try {
			gpx = GPX.Reader.DEFAULT.read(file.getInputStream());
        } catch (IOException e) {throw new RuntimeException(e);}

		return newActivity(gpx, user);
    }

	@Transactional
	public Activity newActivity(InputStream path, User user){
		GPX gpx;
        try {
			gpx = GPX.Reader.DEFAULT.read(path);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
		return newActivity(gpx, user);
    }

	private Activity newActivity(GPX gpx, User user){
		Track track = gpx.getTracks().get(0);
		List<WayPoint> pts = track.getSegments().get(0).getPoints();

		Activity activity = new Activity();
		activity.setOwner(user);

		//process the metadata
		gpx.getMetadata().ifPresent(metadata -> {
			Optional<Instant> optTime = metadata.getTime();
			if (optTime.isPresent()){
				activity.setStartTime(optTime.get());
			} else {
				activity.setStartTime(pts.get(0).getTime().orElseThrow(()-> new IncorrectParametersException("Uploaded activity is invalid")));
			}
		});
		activity.setName(track.getName().isPresent() ? track.getName().get():"No Name");
		activity.setType(track.getType().isPresent() ? track.getType().get():"No Type");
		//process the waypoints
		for (WayPoint pt: pts) {
			//add each waypoint to activity
			addWayPoint(activity, pt);
		}
		if (activity.getDataPoints().size()==0) {
			System.out.println("Activity has no datapoints, it will not be saved");
			return null;
		}
		if (activity.getStartTime()==null) activity.setStartTime(activity.getDataPoints().get(0).getTime());
		activityRepository.saveAndFlush(activity);

		//create summary
		ActivitySummary activitySummary = new ActivitySummary(activity);
		activity.setSummary(activitySummary);
		summaryRepository.save(activitySummary); //superfluous in theory, saving the activity should cascade to this. Need to test it before I'm comfortable removing this call
		activityRepository.save(activity);

		return activity;
	}

	private void addWayPoint(Activity activity, WayPoint pt) {
		//processes the WayPoint and adds it to activity in ATRA format
		DataPoint dataPoint = new DataPoint();
		//handle lat, long, ele
		double latitude = pt.getLatitude().doubleValue();
		double longitude = pt.getLongitude().doubleValue();
		double elevation = (pt.getElevation().isPresent() ? pt.getElevation().get().doubleValue() : 0.0);

		dataPoint.put("lat", Double.toString(latitude));
		dataPoint.put("long", Double.toString(longitude));
		dataPoint.put("ele", Double.toString(elevation));

		//handle time
		Optional<Instant> timeOpt = pt.getTime();
        timeOpt.ifPresent(instant -> dataPoint.put("time", instant.toString()));

		//handle extensions
		Optional<Document> extensions = pt.getExtensions();
		if (extensions.isEmpty()) {
			activity.addDataPoint(dataPoint);
			return;
		}
		Element element = extensions.get().getDocumentElement();

		Node currentMetric = element.getFirstChild().getChildNodes().item(0);
		while (currentMetric!=null) {
			//extract the value
			String metric = currentMetric.getNodeName();
			String metricValue = currentMetric.getFirstChild().getNodeValue();

			if (metric.startsWith("gpxtpx:")) metric = metric.substring(7);
			//else System.out.println("Found a metric that does not start with 'gpxtcx:'"); //ideally throw an exception or sth but for now this works

			dataPoint.put(metric, metricValue);
			currentMetric = currentMetric.getNextSibling();
		}

		activity.addDataPoint(dataPoint);
	}


	//<editor-fold desc="calculations">
	public Double totalDistance(Activity activity) {
		List<DataPoint> dataPoints = activity.getDataPoints();
		Double total = 0.0;
		DataPoint prevDp = null;
		for (var dp : dataPoints) {
			total += totalDistance(prevDp==null ? dp:prevDp, dp);
			prevDp = dp;
		}
		return total;
	}

	public static Double totalDistance(DataPoint dp1, DataPoint dP2){
		return totalDistance(dp1.getLat(), dp1.getLon(), dP2.getLat(), dP2.getLon());
	}

	public static Double totalDistance(Double lat1, Double lon1, DataPoint dP) {
		return totalDistance(lat1, lon1, dP.getLat(), dP.getLon());
	}

	public static Double totalDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
		//Haversine formula, courtesy of https://stackoverflow.com/a/27943, https://stackoverflow.com/a/11172685, https://www.movable-type.co.uk/scripts/latlong.html
		var R = 6371; // Radius of the earth in km
		double dLat = deg2rad(lat2-lat1);  // deg2rad below
		double dLon = deg2rad(lon2-lon1);
		var a =
				Math.sin(dLat/2) * Math.sin(dLat/2) +
						Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
								Math.sin(dLon/2) * Math.sin(dLon/2);
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return R * c; //distance in km
	}

	private static Double deg2rad(Double deg) {
		return deg * (Math.PI/180);
	}

	public Double elevationGain(Activity activity) {
		//can be done in a single line with filter and reduce like in activity summary, but this only iterates once
		double runningTotal = 0.0;
		DataPoint prevDp = null;
		for (var dp : activity.getDataPoints()) {
			if (prevDp==null) prevDp=dp;
			double diff = dp.getEle()-prevDp.getEle();
			if (diff>0) {
				runningTotal += diff;
			}
			prevDp = dp;
		}
		return runningTotal;
	}

	//</editor-fold>

	//<editor-fold desc="fetching methods">
	public Optional<Activity> findById(long id) {
		return activityRepository.findById(id);
	}

	public boolean exists(long id) {
		return activityRepository.existsById(id);
	}

	public List<Activity> get(List<Long> ids) {
		List<Activity> result = new ArrayList<>();
		for (var id : ids) {
			Optional<Activity> opt = this.findById(id);
			opt.ifPresent(result::add);
		}
		return result;
	}

	public Activity get(Long id) {
		return activityRepository.findById(id).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));
	}

	public Collection<Activity> findVisibleTo(Mural mural, boolean shouldRouteBeNull) {
		if (shouldRouteBeNull) return activityRepository.findVisibleToMuralAndRouteIsNull(mural.getId(), mural.getMembers().stream().map(User::getId).toList());
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList());
	}

	public Page<Activity> findVisibleTo(Mural mural, int startPage, int pageSize, boolean shouldRouteBeNull) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());
		if (shouldRouteBeNull) return activityRepository.findVisibleToMuralAndRouteIsNull(mural.getId(), mural.getMembers().stream().map(User::getId).toList(), pageRequest);
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList(), pageRequest);
	}

	public List<Activity> findByUser(User user) {
		return activityRepository.findByOwner(user);
	}

	public List<Activity> findByUser(User user, boolean shouldRouteBeNull) {
		if (shouldRouteBeNull) return activityRepository.findByOwnerAndRouteIsNull(user);
		return activityRepository.findByOwner(user);
	}

	public Page<Activity> findByUser(User user, int startPage, int pageSize, boolean shouldFetchRoutes) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());

		if (shouldFetchRoutes) return activityRepository.findByOwnerAndRouteIsNull(user, pageRequest);
		return activityRepository.findByOwner(user, pageRequest);
	}

	public Collection<Activity> findByUserAndVisibilityType(User user, String visibility) {
		VisibilityType visibilityType;
		try {visibilityType = VisibilityType.valueOf(visibility.toUpperCase(Locale.ROOT));}
		catch (IllegalArgumentException e) {throw new HttpException(400, "Visibility specified is not a valid VisibilityType. Valid values are PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC. \"");		}
		return findByUserAndVisibilityType(user, visibilityType);
	}

	public Collection<Activity> findByUserAndVisibilityType(User user, VisibilityType visibility) {
		return activityRepository.findByVisibilityTypeInAndOwnerIn(List.of(visibility), List.of(user));
	}

	public Collection<Activity> findByUserAndVisibilityNonPrivate(User targetUser) {
		return activityRepository.findByVisibilityTypeInAndOwnerIn(
				List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC),
				List.of(targetUser));
	}

	public List<Activity> findByRouteAndUser(Route route, User user) {
		return activityRepository.findByRouteAndOwner(route, user);
	}

	public List<Activity> findByRouteAndUserAndVisibilityTypeIn(Route route, User user, List<VisibilityType> visibilityTypes) {
		return activityRepository.findByRouteAndOwnerAndVisibilityTypeIn(route,user,visibilityTypes);
	}

	//</editor-fold>


	/**
	 *
	 * @param activityId
	 * @param newVisibility
	 * @return false if activityId doesn't match any existing activities, true otherwise
	 */
	public boolean changeVisibility(Long activityId, VisibilityType newVisibility){ //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		return changeVisibility(activityId,newVisibility,null);
	}
	public Activity changeVisibility(User user, Long id, Visibility newVisibility) {
		Activity activity = activityRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Could not find the activity with id " + id + " so the change visibility operation has been canceled"));
		if (!user.equals(activity.getOwner())) throw new PermissionException("Only the owner of an activity can change its visibility.");
		this.changeVisibility(id, newVisibility);
		return activity;
	}
	public boolean changeVisibility(Long id, Visibility newVisibility) {
		return changeVisibility(id, newVisibility.getType(), newVisibility.getAllowedMuralsNonNull());
	}
	public boolean changeVisibility(Long activityId, VisibilityType newVisibility, Collection<Long> allowedMuralsCol) { //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		Activity activity = activityRepository.findById(activityId).orElseThrow(()->new EntityNotFoundException("Could not find the activity with id " + activityId + " so the change visibility operation has been canceled"));
		activity.changeVisibilityTo(newVisibility, allowedMuralsCol);
		activityRepository.save(activity);
		return true;
	}

	private boolean isVisibleToMural(Activity activity, Mural mural) {
		return activity.getVisibility().isVisibleByMural(mural.getId());
	}

	private boolean isVisibleToUser(Activity activity, User user) {
		return user.equals(activity.getOwner()) || activity.getVisibility().isPublic() || (user.isAdmin() && !activity.getVisibility().isPrivate());
	}

	public List<List<Activity>> getActivitiesFromRoutes(List<Route> routes, User user, String from, Long targetId) {
		List<List<Activity>> activityList = new ArrayList<>();
		if (from==null || "authUser".equals(from)) routes.forEach(route -> activityList.add(this.findByRouteAndUser(route, user)));
		else if ("user".equals(from)){
			User targetUser = userRepository.findById(targetId).orElseThrow(()-> new HttpException(404, "Target user not found"));
			if (!user.equals(targetUser) && !user.isAdmin()) {//return public activities
				routes.forEach(route -> activityList.add(this.findByRouteAndUserAndVisibilityTypeIn(route, user, List.of(VisibilityType.PUBLIC))));
			} else if (user.equals(targetUser)) {
				routes.forEach(route -> activityList.add(this.findByRouteAndUser(route, user)));
			} else if (user.isAdmin()) {
				routes.forEach(route -> activityList.add(this.findByRouteAndUserAndVisibilityTypeIn(route, user, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC))));
			}
			else throw new HttpException(500, "How'd you even get here? I think you only go here when targetUser is null, but that can only happen if id is null, which has been checked."); //I think this only happens with targetUser==null
		}
		else {
			Mural mural = muralRepository.findById(targetId).orElseThrow(() -> new HttpException(404, "Requested mural doesn't exist"));
			routes.forEach(route -> activityList.add(activityRepository.findByRouteAndMural(route, mural.getId()).stream().toList()));
		}
		return activityList;
	}

	public Activity removeActivityFromRoute(User user, Long routeId, Long activityId) {
		Activity activity = activityRepository.findById(activityId).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));

		if (routeId!=-1){ //it's -1 when called from this.removeRoute
			Route route = routeRepository.findById(routeId).orElseThrow(()->new EntityNotFoundException(ROUTE_NOT_FOUND));
			if (!route.equals(activity.getRoute())) throw new IncorrectParametersException("The requested activity is not a member of the specified route.");
		}
		if (!user.equals(activity.getOwner()) && !user.isAdmin()) throw new PermissionException("You can only remove your own activities from a route");

		//Confirmed that route and activity exists, and that they're related
		activity.setRoute(null);
		activityRepository.save(activity);
		return activity;
	}

	public Activity getActivity(User user, Long id, Long muralId) {
		Activity activity = activityRepository.findById(id).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));

		if (muralId!=null) {
			Mural mural = muralRepository.findById(muralId).orElseThrow(()->new EntityNotFoundException("Requesting mural not found"));
			if (!mural.getMembers().contains(user)) throw new IncorrectParametersException("Cannot request activities through a mural you're not part of");
			if (!this.isVisibleToMural(activity, mural)) throw new VisibilityException("Activity is not visible to specified mural");
		} else {
			if (!this.isVisibleToUser(activity, user)) throw new VisibilityException("Activity is not visible to user");
		}
		return activity;
	}

	public List<Activity> getActivitiesByIds(User user, List<Long> ids, Long muralId) {
		Mural mural;
		if (muralId!=null) {
			mural = muralRepository.findById(muralId).orElseThrow(() -> new HttpException(404, "Specified mural not found"));
			if (!mural.getMembers().contains(user) && !user.isAdmin()) throw new HttpException(403, "User is not a member of specified mural");
		} else {
			mural = null;
		}
		List<Activity> activities = activityRepository.findAllById(ids);
		return activities.stream().filter(a->
				(user.isAdmin()&&!a.getVisibility().isPrivate()) ||
						user.equals(a.getOwner()) ||
						a.getVisibility().isPublic() ||
						mural!=null && a.getVisibility().isVisibleByMural(muralId)
			).toList();
	}

	public PagedActivities getActivitiesPaged(User user, GetActivitiesParams params) {
		String from = params.getFrom();
		Long id = params.getId();
		Integer startPage = params.getStartPage();
		Integer pagesToFetch = params.getPagesToFetch();
		Integer pageSize = params.getPageSize();
		String cond = params.getCond();

		if (pageSize==null) pageSize=Constants.PAGE_SIZE;

		boolean shouldFetchRoutes = "nullRoute".equals(cond);

		PagedActivities activities = new PagedActivities();
		Page<Activity> firstPage;
		int pagesSent;

		if (from == null || "authUser".equals(from)) {
			firstPage = this.findByUser(user, startPage, pageSize, shouldFetchRoutes);
			activities.addAll(firstPage.getContent());
			for (pagesSent = 1; pagesSent < pagesToFetch; pagesSent++) {
				Page<Activity> currentPage = this.findByUser(user, startPage + pagesSent, pageSize, shouldFetchRoutes);
				if (!currentPage.hasContent()) break;
				activities.addAll(currentPage.getContent());
			}
		}
		else if ("mural".equals(from)) {
			if (id==null) throw new IncorrectParametersException("Requested activities from a specific mural without providing their id");
			Mural mural = muralRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(MURAL_NOT_FOUND));
			if (!mural.getMembers().contains(user) && !user.isAdmin()) throw new VisibilityException("Authenticated user is not a member of requested mural");

			firstPage = this.findVisibleTo(mural, startPage, pageSize, shouldFetchRoutes);
			activities.addAll(firstPage.getContent());
			for (pagesSent=1; pagesSent < pagesToFetch; pagesSent++) {
				Page<Activity> currentPage = this.findVisibleTo(mural, startPage + pagesSent, pageSize, shouldFetchRoutes);
				if (!currentPage.hasContent()) break;
				activities.addAll(currentPage.getContent());
			}
		}
		else throw new IncorrectParametersException("Specified value for 'from' ("+from+") is invalid. Valid values are: 'authUser', 'mural'");

		activities.setTotalPages(firstPage.getTotalPages());
		activities.setTotalEntries(firstPage.getTotalElements());
		activities.setStartPage(startPage);
		activities.setPagesSent(pagesSent);
		activities.setEntriesSent(activities.size());
		activities.setPageSize(pageSize);
		return activities;
	}

	public Collection<Activity> getActivities(User user, GetActivitiesParams params) {
		String from = params.getFrom();
		Long id = params.getId();
		String cond = params.getCond();
		String visibility = params.getVisibility();
		Collection<Activity> activities;

		if (from==null || "authUser".equals(from))
			activities = this.findByUser(user, "nullRoute".equals(cond));
		else if (id==null) throw new IncorrectParametersException("Requested activities from a specific user/mural without providing their id");
		else if ("mural".equals(from)) {
			Mural mural = muralRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(MURAL_NOT_FOUND));
			if (!mural.getMembers().contains(user) && !user.isAdmin()) throw new VisibilityException("Authenticated user is not a member of requested mural");
			activities = this.findVisibleTo(mural, "nullRoute".equals(cond));
		}
		else if ("user".equals(from)) {
			User targetUser = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
			if (!user.equals(targetUser) && !user.isAdmin()) {//return public activities
				activities = this.findByUserAndVisibilityType(user, VisibilityType.PUBLIC);
			} else if (user.equals(targetUser)) {
				if (visibility==null) activities = this.findByUser(user, "nullRoute".equals(cond));
				else activities = this.findByUserAndVisibilityType(user, visibility);
			} else if (user.isAdmin()) {
				if (visibility==null) activities = this.findByUserAndVisibilityNonPrivate(targetUser);
				else if ("PRIVATE".equals(visibility)) throw new IncorrectParametersException("No one can see private activities except for the user who created them.");
				else activities = this.findByUserAndVisibilityType(user, visibility);
			}
			else throw new RuntimeException("How'd you even get here? I think you only go here when targetUser is null, but that can only happen if id is null, which has been checked."); //I think this only happens with targetUser==null
		}
		else throw new IncorrectParametersException("Activities requested from an unknown/unhandled entity type: " + from);

		return activities;
	}

	public Activity removeRoute(User user, Long id) {
		return removeActivityFromRoute(user, -1L, id);
	}

	public Activity addRoute(User user, Long activityId, Long routeId) {
		Activity activity = activityRepository.findById(activityId).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));
		Route route = routeRepository.findById(routeId).orElseThrow(()->new EntityNotFoundException(ROUTE_NOT_FOUND));
		if (!user.equals(activity.getOwner())) throw new PermissionException("You can only change the route of activities you own");
		if (!AtraUtils.isRouteVisibleBy(route, user)) throw new VisibilityException("User has no visibility of selected route. Can't use a non-visible route");

		activity.setRoute(route);
		activityRepository.save(activity);
		return activity;
	}

	public Activity deleteActivity(User user, Long id) {
		Activity activity = activityRepository.findById(id).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));
		if (!user.equals(activity.getOwner()) && !user.isAdmin()) throw new PermissionException("You can only delete an activity if you're its creator or you're an admin");
		if (!user.equals(activity.getOwner()) && user.isAdmin()  && activity.getVisibility().isPrivate()) throw new PermissionException("Only the creator can delete private activities.");
		activityRepository.deleteById(id);
		return activity;
	}

	public void makeActivitiesNotVisibleToMural(User user, Long muralId, List<Long> selectedActivitiesIds) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(()-> new EntityNotFoundException(MURAL_NOT_FOUND));
		List<Activity> activities = activityRepository.findAllById(selectedActivitiesIds);
		activities.forEach(activity -> {
			if (!user.equals(activity.getOwner()) && !user.isAdmin()) return;
			if (activity.getVisibility().isPrivate()) return;
			if (activity.getVisibility().isMuralSpecific()) {
				activity.getVisibility().removeMural(muralId);
			} else if (activity.getVisibility().isMuralPublic() || activity.getVisibility().isPublic()) {
				List<Long> memberMuralIds = new ArrayList<>(user.getMemberMurals().stream().map(Mural::getId).toList());
				memberMuralIds.remove(muralId);
				activity.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, memberMuralIds);
			}
			activityRepository.save(activity);
			muralRepository.save(mural);
		});
	}

	public List<Activity> getOwnedActivitiesInMural(User user, Long muralId) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(() -> new EntityNotFoundException(MURAL_NOT_FOUND));
		return activityRepository.findByUserAndVisibleToMural(user, mural.getId());
	}

	public Activity editActivity(User user, Long id, ActivityEditDTO activity) {
		Activity act = activityRepository.findById(id).orElseThrow(()->new EntityNotFoundException(ACT_NOT_FOUND));
		if (!user.equals(act.getOwner())) throw new PermissionException("User does not have access to specified activity");
		if (activity.getName()!=null && !activity.getName().isEmpty()) {
			act.setName(activity.getName());
		}
		if (activity.getType()!=null && !activity.getType().isEmpty()) {
			act.setType(activity.getType());
		}
		activityRepository.save(act);
		return act;
	}
}
