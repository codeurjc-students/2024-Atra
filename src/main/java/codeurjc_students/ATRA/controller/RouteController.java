package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.*;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.service.*;
import codeurjc_students.ATRA.service.auxiliary.AtraUtils;
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
    private ActivityService activityService;


    @GetMapping("/{id}")
    public ResponseEntity<RouteWithoutActivityDTO> getRoute(Principal principal, @PathVariable Long id){
        User user = principal==null ? null:principalVerification(principal);
        return ResponseEntity.ok(new RouteWithoutActivityDTO(routeService.getRoute(user, id)));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityOfRouteDTO>> getActivitiesAssignedToRoute(Principal principal, @PathVariable Long id, @RequestParam("mural") Long muralId){
        User user = principalVerification(principal);
        return ResponseEntity.ok(ActivityOfRouteDTO.toDto(routeService.getActivitiesAssignedToRoute(id, user, muralId)));
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

        List<Route> routes = routeService.getAllRoutes(user, type, from, id, visibility);
        if ("noActivities".equals(type))  return ResponseEntity.ok(RouteWithoutActivityDTO.toDto(routes)); //ideally we'd just return Routes, but we kinda can't
        List<List<Activity>> activityList = activityService.getActivitiesFromRoutes(routes, user, from, id);

        return ResponseEntity.ok(RouteWithActivityDTO.toDto(routes, activityList.stream().map(ActivityOfRouteDTO::toDto).toList()));
    }

    @PostMapping
    public ResponseEntity<RouteWithActivityDTO> createRoute(Principal principal, @RequestBody Route route){
        //Route should have in its id field the id of the route from which it is to be created
        User user = principalVerification(principal);
        Long activityId = route.getId();

        return ResponseEntity.ok(new RouteWithActivityDTO(
                routeService.createRoute(user, activityId),
                List.of(new ActivityOfRouteDTO(activityService.get(activityId))
                )));

    }

    @DeleteMapping("/{routeId}/activities/{activityId}")
    public ResponseEntity<Boolean> removeActivityFromRoute(Principal principal, @PathVariable Long routeId, @PathVariable Long activityId) {
        User user = principalVerification(principal);
        activityService.removeActivityFromRoute(user, routeId, activityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{routeId}/activities/")
    public ResponseEntity<Boolean> addActivitiesToRoute(Principal principal, @PathVariable Long routeId, @RequestBody List<Long> activityIds) {
        User user = principalVerification(principal);
        routeService.addActivitiesToRoute(user, routeId, activityIds);
        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteRoute(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);

        routeService.deleteRoute(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Boolean> changeVisibility(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Visibility requestedVisibility = AtraUtils.parseVisibility(body);
        routeService.changeVisibility(user, id, requestedVisibility);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/visibility/mural")
    public ResponseEntity<String> makeRoutesNotVisibleToMural(Principal principal, @RequestParam("id") Long muralId, @RequestBody List<Long> selectedRoutesIds) {
        User user = principalVerification(principal);
        routeService.makeRoutesNotVisibleToMural(user, muralId, selectedRoutesIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/OwnedInMural")
    public ResponseEntity<Collection<RouteWithoutActivityDTO>> getOwnedRoutesInMural(Principal principal, @RequestParam("muralId") Long muralId) {
        User user = principalVerification(principal);
        Collection<Route> result = routeService.getOwnedRoutesInMural(user, muralId);
        return ResponseEntity.ok(result.stream().map(RouteWithoutActivityDTO::new).toList());
    }

    private User principalVerification(Principal principal) {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
        //404 should never happen. Maybe should be 500
    }


}

