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
    public ResponseEntity<RouteWithoutActivityDTO> getRoute(Principal principal, @PathVariable Long id){
        Route route = routeService.findById(id).orElseThrow(() -> new HttpException(404, "No route with id " + id));
        User user = principal==null ? null:principalVerification(principal);
        if (!routeService.isVisibleByDeep(route, user)) throw new HttpException(403, "Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info
        return ResponseEntity.ok(new RouteWithoutActivityDTO(route));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityOfRouteDTO>> getActivitiesAssignedToRoute(Principal principal, @PathVariable Long id, @RequestParam("mural") Long muralId){
        Route route = routeService.findById(id).orElseThrow(() -> new HttpException(404, "No route with id " + id));
        User user = principal==null ? null:principalVerification(principal);
        if (!routeService.isVisibleByDeep(route, user)) throw new HttpException(403, "Authenticated user has no visibility of specified route"); //technically 404 would be safer, gives less info

        if (muralId==-1) {
            return ResponseEntity.ok(ActivityOfRouteDTO.toDto(activityService.findByRouteAndUser(route, user)));
        }
        Mural mural = muralService.findById(muralId).orElseThrow(()->new HttpException(404, "Requested mural not found"));
        return ResponseEntity.ok(ActivityOfRouteDTO.toDto(activityService.findByRouteAndMural(route, mural)));
    }
    @GetMapping
    public ResponseEntity<List<? extends RouteDtoInterface>> getAllRoutes(
            Principal principal,
            @RequestParam(name="type", required = false) String type,
            @RequestParam(name="from", required = false) String from,
            @RequestParam(name="id", required = false) Long id,
            @RequestParam(name="visibility", required = false) String visibility
    ){
        //probably could/should add some authentication, but for now this works
        User user = principalVerification(principal);

        //For mural return all mural_specific and mural_public it sees
        //for public ones, return only if at least one user has an activity in it

        //for user return all created by them, and all public such that he has an activity there
        List<Route> routes;
        if (from==null || "authUser".equals(from)) routes = routeService.findUsedOrCreatedBy(user);
        else if ("user".equals(from)){
            User targetUser = userService.findById(id).orElseThrow(()-> new HttpException(404, "Target user not found"));
            if (!user.equals(targetUser) && !user.hasRole("ADMIN")) {//return public activities
                routes = routeService.findByCreatedByAndVisibilityTypeIn(user, List.of(VisibilityType.PUBLIC));
            } else if (user.equals(targetUser)) {
                if (visibility==null) routes = routeService.findUsedOrCreatedBy(user);
                else routes = routeService.findByOwnerAndVisibilityType(user, visibility);
            } else if (user.hasRole("ADMIN")) {
                if (visibility==null) routes = routeService.findByCreatedByAndVisibilityTypeIn(targetUser, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC));
                else if ("PRIVATE".equals(visibility)) throw new HttpException(400, "No one can see private routes except for the user who created them.");
                else routes = routeService.findByOwnerAndVisibilityType(user, visibility);
            }
            else throw new HttpException(500, "How'd you even get here? I think you only go here when targetUser is null, but that can only happen if id is null, which has been checked."); //I think this only happens with targetUser==null
        }
        else if ("mural".equals(from)){
            Mural mural = muralService.findById(id).orElseThrow(() -> new HttpException(404, "Requested mural doesn't exist"));
            routes = routeService.findVisibleTo(mural);
        } else throw new HttpException(400, "Specified value for 'from' ("+from+") is invalid. Valid values are: 'authUser', 'user', 'mural'");
        if ("noActivities".equals(type))  return ResponseEntity.ok(RouteWithoutActivityDTO.toDto(routes)); //ideally we'd just return Routes, but we kinda can't

        //fetch and return activities
        List<List<ActivityOfRouteDTO>> activityList = new ArrayList<>();
        if (from==null || "authUser".equals(from)) routes.forEach(route -> activityList.add(ActivityOfRouteDTO.toDto(activityService.findByRouteAndUser(route, user))));
        else if ("user".equals(from)){
            User targetUser = userService.findById(id).orElseThrow(()-> new HttpException(404, "Target user not found"));
            if (!user.equals(targetUser) && !user.hasRole("ADMIN")) {//return public activities
                routes.forEach(route -> activityList.add(ActivityOfRouteDTO.toDto(activityService.findByRouteAndUserAndVisibilityTypeIn(route, user, List.of(VisibilityType.PUBLIC)))));
            } else if (user.equals(targetUser)) {
                routes.forEach(route -> activityList.add(ActivityOfRouteDTO.toDto(activityService.findByRouteAndUser(route, user))));
            } else if (user.hasRole("ADMIN")) {
                routes.forEach(route -> activityList.add(ActivityOfRouteDTO.toDto(activityService.findByRouteAndUserAndVisibilityTypeIn(route, user, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC)))));
            }
            else throw new HttpException(500, "How'd you even get here? I think you only go here when targetUser is null, but that can only happen if id is null, which has been checked."); //I think this only happens with targetUser==null
        }
        else {
            Mural mural = muralService.findById(id).orElseThrow(() -> new HttpException(404, "Requested mural doesn't exist"));
            routes.forEach(route -> activityList.add(ActivityOfRouteDTO.toDto(activityService.findByRouteAndMural(route, mural))));
        }

        return ResponseEntity.ok(RouteWithActivityDTO.toDto(routes, activityList));
    }
    @PostMapping
    public ResponseEntity<RouteWithActivityDTO> createRoute(@RequestBody Route route){
        //Route should have in its id field the id of the route from which it is to be created
        Long activityId = route.getId();
        if (!activityService.exists(activityId)) return ResponseEntity.notFound().build();
        Activity activity = activityService.get(activityId);
        if (activity==null) return ResponseEntity.badRequest().build();
        Route resultRoute = routeService.newRoute(activity, activityService);
        return ResponseEntity.ok(new RouteWithActivityDTO(resultRoute, List.of(new ActivityOfRouteDTO(activity))));

    }

    @DeleteMapping("/{routeId}/activities/{activityId}")
    public ResponseEntity<Boolean> removeActivityFromRoute(Principal principal, @PathVariable Long routeId, @PathVariable Long activityId) {
        Route route;
        Activity activity;
        User user = principalVerification(principal);
        try {
            route = routeService.findById(routeId).orElseThrow();
            activity = activityService.findById(activityId).orElseThrow();
            if (!activityService.getByRoute(route).contains(activity)) return ResponseEntity.notFound().build();
            if (!user.equals(activity.getUser()) && !user.hasRole("ADMIN")) throw new HttpException(403, "A user can only remove their own activities from a route");

            //Confirmed that route and activity exists, and that they're related
            activity.setRoute(null);
            activityService.save(activity);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{routeId}/activities/")
    public ResponseEntity<Boolean> addActivitiesToRoute(Principal principal, @PathVariable Long routeId, @RequestBody List<Long> activityIds) {
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
                act.setRoute(route);
                activityService.save(act);
            }
            routeService.save(route);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteRoute(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);

        Route route = routeService.findById(id).orElseThrow(()->new HttpException(404));
        if (route.getCreatedBy()!=user && !user.hasRole("ADMIN")) throw new HttpException(403, "You cannot delete a route you are not the owner of. Public routes can only be deleted by administrators");

        if (route.getVisibility().isPublic() && !user.hasRole("ADMIN")) throw new HttpException(403, "Public routes can only be deleted by administrators"); //This is indirectly checked above by route.getOwner()!=user, since owner will be null for public routes
        if (route.getVisibility().isMuralSpecific() &&
            activityService.findByRoute(route).stream().anyMatch(a->a.getUser()!=user)) {
            throw new HttpException(422, "Cannot delete a route that other people are using");
        }
        for (var act : activityService.findByRoute(route)) {
            act.setRoute(null);
            activityService.save(act);
        }
        this.activityService.routeDeleted(id);

        this.deletionService.deleteRoute(id);
        return ResponseEntity.noContent().build();//getAllRoutes(principal, null, null, null, null);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Boolean> changeVisibility(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
            User user = principalVerification(principal);
        Route route = routeService.findById(id).orElseThrow(()->new HttpException(404));
        if (route.getVisibility().isPublic()) throw new HttpException(422, "The visibility of a public route cannot be changed");
        if (!user.getId().equals(route.getCreatedBy().getId()) && !user.hasRole("ADMIN")) throw new HttpException(403);

        UtilsService.changeVisibilityHelper(id, body, routeService); //throws error on not found or invalid visibility
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/visibility/mural")
    public ResponseEntity<String> makeRoutesNotVisibleToMural(Principal principal, @RequestParam("id") Long muralId, @RequestBody List<Long> selectedRoutesIds) {
        User user = principalVerification(principal);
        if (!muralService.exists(muralId)) throw new HttpException(404, "Mural not found");

        List<Route> routes = routeService.findById(selectedRoutesIds);
        routes.forEach(route -> {
            if (!route.getVisibility().isMuralSpecific()) return;
            if (!user.equals(route.getCreatedBy()) && !user.hasRole("ADMIN")) return;
            route.getVisibility().removeMural(muralId);

            routeService.save(route);
        });
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/OwnedInMural")
    public ResponseEntity<Collection<RouteWithoutActivityDTO>> getOwnedRoutesInMural(Principal principal, @RequestParam("muralId") Long muralId) {
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

