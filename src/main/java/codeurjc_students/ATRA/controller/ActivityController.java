package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.RouteService;
import codeurjc_students.ATRA.service.UserService;
import codeurjc_students.ATRA.service.DeletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

	@Autowired
	private ActivityService activityService;
    @Autowired
    private UserService userService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private DeletionService deletionService;

    public Activity getActivity(){
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivity(@PathVariable("id") Long id, Principal principal){
        //this can be extracted, returning User and throwing errors
        if (principal==null) return ResponseEntity.status(401).build();
        Optional<User> userOpt = userService.findByUserName(principal.getName());
        if (userOpt.isEmpty()) return  ResponseEntity.status(404).build(); //this should never happen. Maybe should be 500
        User user = userOpt.get();
        //this can be extracted, returning User and throwing errors

        if (id!=null) { //user is requesting a specific activity
            Optional<Activity> actOpt = activityService.findById(id);
            if (actOpt.isEmpty()) return ResponseEntity.notFound().build();

            //check that the user has permission to see this activity
            Activity activity = actOpt.get();

            if (!activity.getUser().equals(user) && !activity.getVisibility().isPublic()) return ResponseEntity.status(403).build();
            //fetch and return the activity

            return ResponseEntity.ok(new ActivityDTO(activity));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getActivities(Principal principal, @RequestParam(required=false) List<Long> ids){
        if (ids!=null) return getMultipleActivities(principal, ids);
        //this can be extracted, returning User and throwing errors
        if (principal==null) return ResponseEntity.status(401).build();
        Optional<User> userOpt = userService.findByUserName(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(404).build(); //this should never happen. Maybe should be 500
        User user = userOpt.get();
        return ResponseEntity.ok(ActivityDTO.toDto(user.getActivities()));
        //this can be extracted, returning User and throwing errors
    }

    private ResponseEntity<List<ActivityDTO>> getMultipleActivities(Principal principal, List<Long> ids) {
        //principal should be used to check permissions, but it's complex
        //Actually shouldn't even be using the principal.
        //should check that the principal is in the mural, and that the mural has access to the activity
        List<ActivityDTO> activityDTOS = new ArrayList<>();
        List<Activity> activities = activityService.findById(ids);
        activities.forEach(activity -> {
            activityDTOS.add(new ActivityDTO(activity));
        });
        return ResponseEntity.ok(activityDTOS);
    }

    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestParam("file") MultipartFile file, Principal principal){
        if (principal==null) {
            return  ResponseEntity.badRequest().build();
        }

        Activity activity = activityService.newActivity(file, principal.getName());
        return ResponseEntity.ok(activity);
    }

    public ResponseEntity<Activity> modifyActivity(){return null;}

    public ResponseEntity<Activity> deleteActivity(){
        return null;
    }


    @DeleteMapping("/{id}/route")
    public ResponseEntity<ActivityDTO> removeRoute(@PathVariable Long id) {
        Activity activity = activityService.findById(id).orElse(null);
        if (activity==null) return ResponseEntity.notFound().build();
        Route prevRoute = activity.getRoute();
        routeService.removeActivityFromRoute(activity,prevRoute);
        activity.setRoute(null);
        activityService.save(activity);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PostMapping("/{activityId}/route")
    public ResponseEntity<ActivityDTO> addRoute(@PathVariable Long activityId, @RequestBody Long routeId) {
        Activity activity = activityService.findById(activityId).orElse(null);
        Route route = routeService.findById(routeId).orElse(null);
        if (activity==null || route==null) return ResponseEntity.notFound().build();

        activity.setRoute(route);
        activityService.save(activity);
        route.addActivity(activity);
        routeService.save(route);
        //danger warning warn problema cuidado
        return ResponseEntity.ok(new ActivityDTO(activity)); //was new ActivityDTO(activity, new BasicNamedId(routeId, route.getName()))
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ActivityDTO> deleteActivity(@PathVariable Long id) {
        //gotta check permissions. If not allowed, should return 404 instead of 403, so as to not show ids in use
        Activity activity = activityService.findById(id).orElse(null);
        if (activity==null) return ResponseEntity.notFound().build();
        if (activity.getRoute()!=null) {
            Route route = activity.getRoute();
            route.removeActivity(activity);
            routeService.save(route);
        }

        deletionService.deleteActivity(id);

        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ActivityDTO> changeVisibility(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String visibilityString = body.get("visibility");
        try {
            VisibilityType visibilityType = VisibilityType.valueOf(visibilityString);
            String csvMuralIds = body.get("allowedMuralsList").substring(1,body.get("allowedMuralsList").length()-1);
            List<Long> allowedMuralsIds = csvMuralIds.isEmpty() ? null:Arrays.stream(csvMuralIds.split(",")).map(Long::parseLong).toList(); //use a fucking string and just parse it, I'm done with this crap
            boolean success = activityService.changeVisibility(id, visibilityType, allowedMuralsIds);
            if (!success) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(new ActivityDTO(activityService.findById(id).orElseThrow(()->new HttpException(404, "Could not find the activity with id " + id + " so the change visibility operation has been canceled"))));
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, visibilityString + " is not a valid Visibility Type");
        }
    }

}

