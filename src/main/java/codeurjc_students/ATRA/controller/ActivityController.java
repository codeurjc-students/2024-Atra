package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.*;
import codeurjc_students.ATRA.service.auxiliary.UtilsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

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
    private MuralService muralService;
    @Autowired
    private DeletionService deletionService;

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivity(Principal principal, @PathVariable("id") Long id, @RequestParam(value="mural", required=false) Long muralId){
        if (id==null) return ResponseEntity.badRequest().build(); // this is useless unless the method is used somewhere. Spring won't allow it to be null

        User user = principalVerification(principal);
        Activity activity = activityService.findById(id).orElseThrow(()->new HttpException(404));

        //first check visibility
        if (muralId!=null) {
            Mural mural = muralService.findById(muralId).orElseThrow(()->new HttpException(404, "Requesting mural not found"));
            if (!activityService.isVisibleToMural(activity, mural)) throw new HttpException(403, "Activity is not visible to specified mural");
        } else {
            if (!activityService.isVisibleToUser(activity, user)) throw new HttpException(403, "Activity is not visible to user");
        }

        //then fetch and return the activity
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getActivities(Principal principal, @RequestParam(required=false) String from, @RequestParam(required=false) Long id){
        User user = principalVerification(principal);

        if (from==null || "authUser".equals(from))
            return ResponseEntity.ok(ActivityDTO.toDto(activityService.findByUser(user)));
        if (id==null) throw new HttpException(400, "Requested activities from a specific user/mural without providing their id");
        if ("mural".equals(from)) {
            Mural mural = muralService.findById(id).orElseThrow(() -> new HttpException(404, "Mural not found"));
            if (!mural.getMembers().contains(user) && !user.hasRole("ADMIN")) throw new HttpException(403, "Authenticated user is not a member of requested mural");
            return ResponseEntity.ok(ActivityDTO.toDto(activityService.findVisibleTo(mural)));
        }
        if ("user".equals(from)) //this is currently not in use.
            return ResponseEntity.ok(
                    ActivityDTO.toDto(
                            activityService.findByUser(
                                userService.findById(id).orElseThrow(()->new HttpException(404, "User not found"))
                            ).stream().filter(activity -> activity.getVisibility().isPublic()).toList())
                    );
        throw new HttpException(400, "Activities requested from an unknown/unhandled entity: " + from);
    }

    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestParam("file") MultipartFile file, Principal principal){
        if (principal==null) {
            return  ResponseEntity.badRequest().build();
        }

        Activity activity = activityService.newActivity(file, principal.getName());
        return ResponseEntity.ok(activity);
    }


    @DeleteMapping("/{id}/route")
    public ResponseEntity<ActivityDTO> removeRoute(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        Activity activity = activityService.findById(id).orElse(null);
        if (activity==null) return ResponseEntity.notFound().build();
        if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) throw new HttpException(403, "You can only remove the route of activities you own");
        Route prevRoute = activity.getRoute();
        routeService.removeActivityFromRoute(activity,prevRoute);
        activity.setRoute(null);
        activityService.save(activity);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PostMapping("/{activityId}/route")
    public ResponseEntity<ActivityDTO> addRoute(Principal principal, @PathVariable Long activityId, @RequestBody Long routeId) {
        User user = principalVerification(principal);
        Activity activity = activityService.findById(activityId).orElse(null);
        Route route = routeService.findById(routeId).orElse(null);
        if (activity==null || route==null) return ResponseEntity.notFound().build();
        if (!user.equals(activity.getUser())) throw new HttpException(403, "You can only change the route of activities you own");
        if (!routeService.isVisibleBy(route, user)) throw new HttpException(404, "Target route not found"); //in others used 403. (security)

        activity.setRoute(route);
        activityService.save(activity);
        route.addActivity(activity);
        routeService.save(route);
        //danger warning warn problema cuidado
        return ResponseEntity.ok(new ActivityDTO(activity)); //was new ActivityDTO(activity, new BasicNamedId(routeId, route.getName()))
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ActivityDTO> deleteActivity(Principal principal, @PathVariable Long id) {
        //gotta check permissions. If not allowed, should return 404 instead of 403, so as to not show ids in use
        User user = principalVerification(principal);
        Activity activity = activityService.findById(id).orElse(null);
        if (activity==null) return ResponseEntity.notFound().build();
        if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) throw new HttpException(403, "You can only delete activities you onw");
        if (activity.getRoute()!=null) {
            Route route = activity.getRoute();
            route.removeActivity(activity);
            routeService.save(route);
        }

        deletionService.deleteActivity(id);

        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ActivityDTO> changeVisibility(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Activity activity = activityService.findById(id).orElseThrow(()->new HttpException(404, "Could not find the activity with id " + id + " so the change visibility operation has been canceled"));
        if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) throw new HttpException(403);
        UtilsService.changeVisibilityHelper(id, body, activityService); //throws error on not found or invalid visibility
        return ResponseEntity.ok(new ActivityDTO(activityService.findById(id).orElseThrow(
                ()->new HttpException(404, "Could not find the activity with id " + id + " so the change visibility operation has been canceled"))));
    }

    @PatchMapping("/visibility/mural")
    public ResponseEntity<String> makeActivitiesNotVisibleToMural(Principal principal, @RequestParam("id") Long muralId, @RequestBody List<Long> selectedActivitiesIds) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(muralId).orElseThrow(()-> new HttpException(404, "Mural not found"));
        List<Activity> activities = activityService.findById(selectedActivitiesIds);
        activities.forEach(activity -> {
            if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) return;
            if (activity.getVisibility().isPrivate() || activity.getVisibility().isPublic()) return;
            if (activity.getVisibility().isMuralSpecific()) {
                activity.getVisibility().removeMural(muralId);
            } else if (activity.getVisibility().isMuralPublic()) {
                List<Long> memberMuralIds = new ArrayList<>(user.getMemberMurals().stream().map(Mural::getId).toList());
                memberMuralIds.remove(muralId);
                activity.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, memberMuralIds);
            }
            activityService.save(activity);
            muralService.save(mural);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/InMural")
    public ResponseEntity<Collection<ActivityDTO>> getActivitiesInMural(Principal principal, @RequestParam("muralId") Long muralId) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(muralId).orElseThrow(() -> new HttpException(404, "Mural not found"));
        Collection<ActivityDTO> result = new ArrayList<>();
        activityService.findByUser(user).forEach(activity -> {
            if (activityService.isVisibleToMural(activity, mural)) result.add(new ActivityDTO(activity));
        });

        return ResponseEntity.ok(result);
    }
    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
        //404 should never happen. Maybe should be 500
    }

}

