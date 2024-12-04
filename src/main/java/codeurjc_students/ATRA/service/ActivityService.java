package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.repository.ActivityRepository;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

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

	public void delete(long id) {
		activityRepository.deleteById(id);
	}



	public Activity newActivity(MultipartFile file, String username){
		final GPX gpx;
        try {
			gpx = GPX.Reader.DEFAULT.read(file.getInputStream());
        } catch (IOException e) {throw new RuntimeException(e);}

		return newActivity(gpx, username);
    }

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
		List<WayPoint> pts = gpx.getTracks().get(0).getSegments().get(0).getPoints();


		Activity activity = new Activity();
		//set user
		Optional<User> userOpt = userService.findByUserName(username);
		if (userOpt.isEmpty()) return null; //or throw exception caught above
		activity.setUser(userOpt.get().getId());

		//process the metadata
		gpx.getMetadata().ifPresent(metadata -> activity.setStartTime(metadata.getTime().get()));
		activity.setName(gpx.getTracks().get(0).getName().get());
		activity.setType(gpx.getTracks().get(0).getType().get());
		//process the waypoints
		for (WayPoint pt: pts) {
			//add each waypoint to activity
			addWayPoint(activity, pt);
		}
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

		//hanlde time
		Optional<Instant> timeOpt = pt.getTime();
        timeOpt.ifPresent(instant -> dataPoint.put("time", instant.toString()));

		//handle extensions
		Optional<Document> extensions = pt.getExtensions();
		if (extensions.isEmpty()) return;
		Element element = extensions.get().getDocumentElement();

		Node currentMetric = element.getFirstChild().getChildNodes().item(0);
		while (currentMetric!=null) {
			//extract the value
			String metric = currentMetric.getNodeName();
			String metricValue = currentMetric.getFirstChild().getNodeValue();

			if (metric.startsWith("gpxtpx:")) metric = metric.substring(7);
			else System.out.println("Found a metric that does not start with 'gpxtcx:'"); //ideally throw an exception or sth but for now this works

			dataPoint.put(metric, metricValue);
			currentMetric = currentMetric.getNextSibling();
		}

		activity.addDataPoint(dataPoint);
	}

}
