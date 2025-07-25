package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.WayPoint;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

		return newActivity(gpx, username);
    }

	@Transactional
	public Activity newActivity(Path path, String username){
		GPX gpx;
        try {
			gpx = GPX.read(path);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
		return newActivity(gpx, username);
    }

	private Activity newActivity(GPX gpx, String username){
		Track track = gpx.getTracks().get(0);
		List<WayPoint> pts = track.getSegments().get(0).getPoints();


		Activity activity = new Activity();
		//set user
		Optional<User> userOpt = userService.findByUserName(username);
		if (userOpt.isEmpty()) return null; //or throw exception caught above
		activity.setUser(userOpt.get());


		//process the metadata
		gpx.getMetadata().ifPresent(metadata -> activity.setStartTime(metadata.getTime().get()));
		activity.setName(track.getName().isPresent() ? track.getName().get():"No Name");
		activity.setType(track.getType().isPresent() ? track.getType().get():"No Type");
		//process the waypoints
		for (WayPoint pt: pts) {
			//add each waypoint to activity
			addWayPoint(activity, pt);
		}
		if (activity.getStartTime()==null) activity.setStartTime(activity.getDataPoints().get(0).get_time());
		activityRepository.save(activity);
		userOpt.get().addActivity(activity);
		userService.save(userOpt.get());
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

		HashSet<Long> allowedMurals = allowedMuralsCol == null ? new HashSet<>() : new HashSet<>(allowedMuralsCol);

		VisibilityType prevType = activity.getVisibility().getType();
		activity.changeVisibilityTo(newVisibility, allowedMuralsCol);
		//now we have to check ALL places where that activity could have been/could be now, to update them
		if (newVisibility.isLessPublicThan(prevType)) { //visibility decreased
			//delete all not in allowedMurals
			activity.getUser().getMemberMurals().forEach(mural -> {
				if (!allowedMurals.contains(mural.getId())) {
					mural.removeActivity(activity);
				}
			});
		} else if (newVisibility.isMorePublicThan(prevType)) { //visibility increased
			//add to all murals in allowedMurals ?? userMurals
			if (allowedMurals.isEmpty()) { //if allowedMurals is empty, add all murals
				activity.getUser().getMemberMurals().forEach(mural -> {
					if (!mural.getActivities().contains(activity))
						mural.addActivity(activity);
				});
			} else { //if allowedMurals has murals, only add to those
				activity.getUser().getMemberMurals().forEach(mural -> {
					if (allowedMurals.contains(mural.getId()))
						mural.addActivity(activity);
				});
			}
		} else { //same visibility, changes to allowed_murals
			if (newVisibility==VisibilityType.MURAL_SPECIFIC) {
				Collection<Long> allowedMuralsActivity = activity.getVisibility().getAllowedMurals();
				activity.getUser().getMemberMurals().forEach(mural -> {
					if (!allowedMurals.contains(mural.getId()) && allowedMuralsActivity.contains(mural.getId()))
						mural.removeActivity(activity);
					else if (allowedMurals.contains(mural.getId()) && !allowedMuralsActivity.contains(mural.getId())) {
						mural.addActivity(activity);
					}
				});
			}
		}
		activityRepository.save(activity);
		return true;
	}

	public List<Activity> getActivitiesFromUser(VisibilityType visibility, User user) {
		return getActivitiesFromUser(visibility, user, null);
	}
	public List<Activity> getActivitiesFromUser(VisibilityType visibility, User user, Long muralId) {
		List<Activity> activities = user.getActivities();
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
}
