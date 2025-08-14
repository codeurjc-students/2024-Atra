package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.*;
import codeurjc_students.ATRA.service.auxiliary.UtilsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;


@RestController
@RequestMapping("/api/routes")
public class RouteController {

	@Autowired
	private UserService userService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private MuralService muralService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private DeletionService deletionService;


    @GetMapping("/{id}")
    public ResponseEntity<Route> getRoute(Principal principal, @PathVariable Long id){
        Route route = routeService.findById(id).orElseThrow(() -> new HttpException(404, "No route with id " + id));
        User user = principal==null ? null:principalVerification(principal);
        if (!routeService.isVisibleBy(route, user)) {
            throw new HttpException(403, "Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info
        }
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityOfRouteDTO>> getActivitiesAssignedToRoute(Principal principal, @PathVariable Long id){
        Route route = routeService.findById(id).orElseThrow(() -> new HttpException(404, "No route with id " + id));
        User user = principal==null ? null:principalVerification(principal);
        if (!routeService.isVisibleBy(route, user)) {
            throw new HttpException(403, "Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info
        }
        return ResponseEntity.ok(ActivityOfRouteDTO.toDto(route.getActivities()));
    }
    @GetMapping
    public ResponseEntity<List<? extends RouteDtoInterface>> getAllRoutes(Principal principal, @RequestParam(name="type", required = false) String type, @RequestParam(name="mural", required = false) Long muralId){
        //probably could/should add some authentication, but for now this works
        User user = principalVerification(principal);

        List<Route> routes;
        if (muralId==null) routes = routeService.findVisibleTo(user);
        else {
            Mural mural = muralService.findById(muralId).orElseThrow(() -> new HttpException(404, "Requested mural doesn't exist"));
            routes = routeService.findVisibleTo(mural);
        }
        if ("noActivities".equals(type))  return ResponseEntity.ok(RouteWithoutActivityDTO.toDto(routes)); //ideally we'd just return Routes, but we kinda can't
        return ResponseEntity.ok(RouteWithActivityDTO.toDto(routes));
    }
    @PostMapping
    public ResponseEntity<RouteWithActivityDTO> createRoute(@RequestBody Route route){
        //Route should have in its id field the id of the route from which it is to be created
        Long activityId = route.getId();
        if (!activityService.exists(activityId)) return ResponseEntity.notFound().build();
        Activity activity = activityService.get(activityId);
        if (activity==null) return ResponseEntity.badRequest().build();
        Route resultRoute = routeService.newRoute(activity, activityService);
        return ResponseEntity.ok(new RouteWithActivityDTO(resultRoute));

    }

    @DeleteMapping("/{routeId}/activities/{activityId}")
    public ResponseEntity<RouteWithActivityDTO> removeActivityFromRoute(Principal principal, @PathVariable Long routeId, @PathVariable Long activityId) {
        Route route;
        Activity activity;
        User user = principalVerification(principal);
        try {
            route = routeService.findById(routeId).orElseThrow();
            activity = activityService.findById(activityId).orElseThrow();
            if (!route.getActivities().contains(activity)) return ResponseEntity.notFound().build();
            if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) throw new HttpException(403, "A user can only remove their own activities from a route");

            //Confirmed that route and activity exists, and that they're related
            route.removeActivity(activity);
            activity.setRoute(null);
            routeService.save(route);
            activityService.save(activity);
            return ResponseEntity.ok(new RouteWithActivityDTO(route));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{routeId}/activities/")
    public ResponseEntity<RouteWithActivityDTO> addActivitiesToRoute(Principal principal, @PathVariable Long routeId, @RequestBody List<Long> activityIds) {
        Route route;
        List<Activity> activities;
        User user = principalVerification(principal);
        try {
            route = routeService.findById(routeId).orElseThrow();
            if (!routeService.isVisibleBy(route,user)) throw new HttpException(403, "User has no visibility of this route"); //404 might be better, more secure, gives less info. (security)
            activities = activityService.findById(activityIds);
            if (activities.isEmpty()) return ResponseEntity.notFound().build();

            //Confirmed that route and activity exists, and that they're related
            for (var act : activities) {
                if (!user.equals(act.getUser())) continue;
                if (act.getRoute()!=null) { //delete the activity from its previous route
                    act.getRoute().removeActivity(act);
                }
                route.addActivity(act);
                act.setRoute(route);
                activityService.save(act);
            }
            routeService.save(route);
            return ResponseEntity.ok(new RouteWithActivityDTO(route));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends RouteDtoInterface>> deleteRoute(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);

        Route route = routeService.findById(id).orElseThrow(()->new HttpException(404));
        if (route.getOwner()!=user && !user.hasRole("ADMIN")) throw new HttpException(403, "You cannot delete a route you are not the owner of. Public routes can only be deleted by administrators");

        if (route.getVisibility().isPublic() && !user.hasRole("ADMIN")) throw new HttpException(403, "Public routes can only be deleted by administrators"); //This is indirectly checked above by route.getOwner()!=user, since owner will be null for public routes
        if (route.getVisibility().isMuralSpecific() &&
            route.getActivities().stream().anyMatch(a->a.getUser()!=user)) {
            throw new HttpException(422, "Cannot delete a route that other people are using");
        }
        for (var act : route.getActivities()) {
            act.setRoute(null);
            activityService.save(act);
        }
        this.activityService.routeDeleted(id);

        this.deletionService.deleteRoute(id);
        return getAllRoutes(principal, null, null);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<RouteWithActivityDTO> changeVisibility(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
            User user = principalVerification(principal);
        Route route = routeService.findById(id).orElseThrow(()->new HttpException(404));
        if (route.getVisibility().isPublic()) throw new HttpException(422, "The visibility of a public route cannot be changed");
        if (!user.getId().equals(route.getOwner().getId()) && !user.hasRole("ADMIN")) throw new HttpException(403);

        UtilsService.changeVisibilityHelper(id, body, routeService); //throws error on not found or invalid visibility
        return ResponseEntity.ok(new RouteWithActivityDTO(routeService.findById(id).orElseThrow(
                ()->new HttpException(404, "Could not find the route with id " + id + " so the change visibility operation has been canceled"))));
    }

    @PatchMapping("/visibility/mural")
    public ResponseEntity<String> makeRoutesNotVisibleToMural(Principal principal, @RequestParam("id") Long muralId, @RequestBody List<Long> selectedRoutesIds) {
        User user = principalVerification(principal);
        if (!muralService.exists(muralId)) throw new HttpException(404, "Mural not found");

        List<Route> routes = routeService.findById(selectedRoutesIds);
        routes.forEach(route -> {
            if (!route.getVisibility().isMuralSpecific()) return;
            if (!user.equals(route.getOwner()) && !user.hasRole("ADMIN")) return;
            route.getVisibility().removeMural(muralId);

            routeService.save(route);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/InMural")
    public ResponseEntity<Collection<RouteWithoutActivityDTO>> getRoutesInMural(Principal principal, @RequestParam("muralId") Long muralId) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(muralId).orElseThrow(() -> new HttpException(404, "Mural not found"));
        if (!mural.getMembers().contains(user) && !user.hasRole("ADMIN")) throw new HttpException(403, "Only mural members or an admin can access this data.");

        Collection<RouteWithoutActivityDTO> result = new ArrayList<>();
        user.getCreatedRoutes().forEach(route -> {
            if (routeService.isVisibleBy(route, mural)) result.add(new RouteWithoutActivityDTO(route));
        });

        return ResponseEntity.ok(result);
    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
        //404 should never happen. Maybe should be 500
    }


}

