package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.dto.DtoService;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.RouteService;
import codeurjc_students.ATRA.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
    private DtoService dtoService;

    public Activity getActivity(){
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivity(@PathVariable("id") Long id, Principal principal){
        //this can be extracted, returning User and throwing errors
        if (principal==null) return ResponseEntity.status(403).build();
        Optional<User> userOpt = userService.findByUserName(principal.getName());
        if (userOpt.isEmpty()) return  ResponseEntity.status(403).build(); //this should never happen. Maybe should be 500
        User user = userOpt.get();
        //this can be extracted, returning User and throwing errors

        if (id!=null) { //user is requesting a specific activity
            Optional<Activity> actOpt = activityService.findById(id);
            if (actOpt.isEmpty()) return ResponseEntity.notFound().build();

            //check that the user has permission to access this activity
            if (!actOpt.get().getUser().equals(user)) return ResponseEntity.status(403).build();
            //fetch and return the activity

            return ResponseEntity.ok(dtoService.toDTO(actOpt.get()));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getActivities(Principal principal){
        //this can be extracted, returning User and throwing errors
        if (principal==null) return ResponseEntity.status(403).build();
        Optional<User> userOpt = userService.findByUserName(principal.getName());
        if (userOpt.isEmpty()) return  ResponseEntity.status(403).build(); //this should never happen. Maybe should be 500
        User user = userOpt.get();
        return ResponseEntity.ok(dtoService.toDtoActivity(user.getActivities()));
        //this can be extracted, returning User and throwing errors
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
        return ResponseEntity.ok(new ActivityDTO(activity, new BasicNamedId(routeId, route.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteActivity(@PathVariable Long id) {
        //gotta check permissions. If not allowed, should return 404 instead of 403, so as to not show ids in use
        Activity activity = activityService.findById(id).orElse(null);
        if (activity==null) return ResponseEntity.notFound().build();
        if (activity.getRoute()!=null) {
            Route route = activity.getRoute();
            route.removeActivity(activity);
            routeService.save(route);
        }

        activityService.delete(id);

        return ResponseEntity.ok().build();
    }


}

