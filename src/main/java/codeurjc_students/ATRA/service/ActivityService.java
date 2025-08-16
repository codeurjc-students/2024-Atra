package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.WayPoint;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class ActivityService implements ChangeVisibilityInterface{

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private UserService userService;

	public Optional<Activity> findById(long id) {
		return activityRepository.findById(id);
	}

	public boolean exists(long id) {
		return activityRepository.existsById(id);
	}

	public List<Activity> findAll() {
		return activityRepository.findAll();
	}

	public void save(Activity activity) {
		activityRepository.save(activity);
	}

	/**
	 * DeletionService.deleteActivity(Long id) should be called instead.
	 * @param id the id of the activity to delete
	 */
	void delete(long id) {
		activityRepository.deleteById(id);
	}

	public Activity newActivity(MultipartFile file, String username){
		final GPX gpx;
        try {
			gpx = GPX.Reader.DEFAULT.read(file.getInputStream());
        } catch (IOException e) {throw new RuntimeException(e);}

		return newActivity(gpx, username, true);
    }

	@Transactional
	public Activity newActivity(Path path, String username, boolean saveToUser){
		GPX gpx;
        try {
			gpx = GPX.read(path);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
		return newActivity(gpx, username, saveToUser);
    }

	private Activity newActivity(GPX gpx, String username, boolean saveToUser){
		Track track = gpx.getTracks().get(0);
		List<WayPoint> pts = track.getSegments().get(0).getPoints();


		Activity activity = new Activity();
		//set user
		User user = userService.findByUserName(username).orElseThrow(()->new HttpException(404, "User not found"));
		activity.setUser(user);


		//process the metadata
		gpx.getMetadata().ifPresent(metadata -> activity.setStartTime(metadata.getTime().get()));
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
		if (activity.getStartTime()==null) activity.setStartTime(activity.getDataPoints().get(0).get_time());
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

	public List<Activity> get(List<Long> ids) {
		List<Activity> result = new ArrayList<>();
		for (var id : ids) {
			Optional<Activity> opt = this.findById(id);
            opt.ifPresent(result::add);
		}
		return result;
	}

	public Activity get(Long id) {
		Optional<Activity> opt = this.findById(id);
        return opt.orElse(null);
    }

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
		return totalDistance(dp1.get_lat(), dp1.get_long(), dP2.get_lat(), dP2.get_long());
	}

	public static Double totalDistance(Double lat1, Double lon1, DataPoint dP) {
		return totalDistance(lat1, lon1, dP.get_lat(), dP.get_long());
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


	public List<Activity> findById(List<Long> activityIds) {
		List<Activity> result = new ArrayList<>();
		for (var id : activityIds) {
			Optional<Activity> actOpt = findById(id);
            actOpt.ifPresent(result::add);
		}
		return result;
	}

	public void routeDeleted(Long deletedRouteId) {
		activityRepository.findAll().forEach(activity -> {
			if (activity.getRoute() != null && activity.getRoute().getId().equals(deletedRouteId)) {
				activity.setRoute(null);
			}
		});
	}

	/**
	 *
	 * @param activityId
	 * @param newVisibility
	 * @return false if activityId doesn't match any existing activities, true otherwise
	 */
	public boolean changeVisibility(Long activityId, VisibilityType newVisibility){ //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		return changeVisibility(activityId,newVisibility,null);
	}
	public boolean changeVisibility(Long activityId, VisibilityType newVisibility, Collection<Long> allowedMuralsCol) { //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>
		Activity activity = activityRepository.findById(activityId).orElseThrow(()->new HttpException(404));
		activity.changeVisibilityTo(newVisibility, allowedMuralsCol);
		activityRepository.save(activity);
		return true;
	}

	public List<Activity> getActivitiesFromUser(VisibilityType visibility, User user) {
		return getActivitiesFromUser(visibility, user, null);
	}
	public List<Activity> getActivitiesFromUser(VisibilityType visibility, User user, Long muralId) {
		List<Activity> activities = activityRepository.findByUser(user);
        return switch (visibility) {
            case PUBLIC -> activities.stream().filter(a -> a.getVisibility().isPublic()).toList();
            case MURAL_PUBLIC -> activities.stream().filter(a -> a.getVisibility().isMuralPublic()).toList();
            case MURAL_SPECIFIC -> {
                if (muralId == null)
                    throw new IllegalArgumentException("getActivities(MURAL_SPECIFIC, ...) called with a null muralId");
                yield activities.stream().filter(a -> a.getVisibility().isMuralPublic() || a.getVisibility().isVisibleByMural(muralId)).toList();
            }
            case PRIVATE -> activities;
        };
    }

	public Double elevationGain(Activity activity) {
		//can be done in a single line with filter and reduce like in activity summary, but this only iterates once
		double runningTotal = 0.0;
		DataPoint prevDp = null;
		for (var dp : activity.getDataPoints()) {
		    if (prevDp==null) prevDp=dp;
			double diff = dp.get_ele()-prevDp.get_ele();
			if (diff>0) {
				runningTotal += diff;
			}
			prevDp = dp;
		}
		return runningTotal;
	}

	public boolean isVisibleToMural(Activity activity, Mural mural) {
		return activity.getVisibility().isVisibleByMural(mural.getId());
	}

	public boolean isVisibleToUser(Activity activity, User user) {
		return user.equals(activity.getUser()) || activity.getVisibility().isPublic() || (user.hasRole("ADMIN") && !activity.getVisibility().isPrivate());
	}

    public Collection<Activity> findByVisibilityType(VisibilityType visibilityType) {
		return activityRepository.findByVisibilityType(visibilityType);
    }

	public Collection<Activity> findVisibleTo(Mural mural) {
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList());
	}

	public Collection<Activity> findVisibleTo(Mural mural, boolean shouldRouteBeNull) {
		if (shouldRouteBeNull) return activityRepository.findVisibleToMuralAndRouteIsNull(mural.getId(), mural.getMembers().stream().map(User::getId).toList());
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList());
	}

	public Page<Activity> findVisibleTo(Mural mural, int startPage, int pageSize) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList(), pageRequest);
	}

	public Page<Activity> findVisibleTo(Mural mural, int startPage, int pageSize, boolean shouldRouteBeNull) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());
		if (shouldRouteBeNull) return activityRepository.findVisibleToMuralAndRouteIsNull(mural.getId(), mural.getMembers().stream().map(User::getId).toList(), pageRequest);
		return activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList(), pageRequest);
	}

	public Collection<Activity> findByRoute(Route route) {
		return activityRepository.findByRoute(route);
	}

	public List<Activity> findByUser(User user) {
		return activityRepository.findByUser(user);
	}

	public List<Activity> findByUser(User user, boolean shouldRouteBeNull) {
		if (shouldRouteBeNull) return activityRepository.findByUserAndRouteIsNull(user);
		return activityRepository.findByUser(user);
	}


	public Page<Activity> findByUser(User user, int startPage, int pageSize) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());
		return activityRepository.findByUser(user, pageRequest);
	}

	public Page<Activity> findByUser(User user, int startPage, int pageSize, boolean shouldFetchRoutes) {
		PageRequest pageRequest = PageRequest.of(startPage, pageSize, Sort.by("startTime").descending());

		if (shouldFetchRoutes) return activityRepository.findByUserAndRouteIsNull(user, pageRequest);
		return activityRepository.findByUser(user, pageRequest);
	}


	public Collection<Activity> findByUserAndVisibleToMural(User user, Mural mural) {
		return activityRepository.findByUserAndVisibleToMural(user, mural.getId());
	}
}
