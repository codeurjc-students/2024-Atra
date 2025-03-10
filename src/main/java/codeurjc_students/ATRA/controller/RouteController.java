package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Coordinates;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.RouteService;
import codeurjc_students.ATRA.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


@RestController
@RequestMapping("/api/routes")
public class RouteController {

	@Autowired
	private UserService userService;
    @Autowired
	private RouteService routeService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private DtoService dtoService;


    @GetMapping("/{id}")
    public ResponseEntity<Route> getRoute(@PathVariable Long id){
        Optional<Route> routeOpt = routeService.findById(id);
        if (routeOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(routeOpt.get());
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityOfRouteDTO>> getActivitiesAssignedToRoute(@PathVariable Long id){
        if (id==null) return ResponseEntity.badRequest().build();

        Optional<Route> routeOpt = routeService.findById(id);
        if (routeOpt.isEmpty()) return ResponseEntity.badRequest().build();
        Route route = routeOpt.get();
        List<Activity> activities = activityService.findById(route.getActivities());


        return ResponseEntity.ok((List<ActivityOfRouteDTO>) dtoService.toDTO(activities, DtoType.ACTIVITY_OF_ROUTE));
    }
    @GetMapping
    public ResponseEntity<List<? extends RouteDtoInterface>> getAllRoutes(@RequestParam(name="type", required = false) String type){
        List<Route> routes = routeService.findAll();
        //probably could/should add some authentication, but for now this works
        List<List<Activity>> activitiesList = new ArrayList<>();
        routes.forEach(route -> {
            List<Activity> activities = activityService.findById(route.getActivities());
            activitiesList.add(activities);
        });
        if ("noActivities".equals(type))  return ResponseEntity.ok(dtoService.toDto(routes, DtoType.ROUTE_WITHOUT_ACTIVITY)); //ideally we'd just return Routes, but we kinda can't
        return ResponseEntity.ok(dtoService.toDtoRoute(routes));
    }
    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody Route route){
        //Route should have in its id field the id of the route from which it is to be created
        Long activityId = route.getId();
        if (!activityService.exists(activityId)) return ResponseEntity.notFound().build();
        Activity activity = activityService.get(activityId);
        route.setCoordinates(Coordinates.fromActivity(activity));

        if (route.getName()==null || route.getName().isEmpty()){
            route.setName("Route from Activity " + activityId);
        }
        if (route.getTotalDistance()==null || route.getTotalDistance()==0) {
            route.setTotalDistance(activityService.totalDistance(activity));
        }

        route.addActivity(activityId);
        route.setId(null);
        this.routeService.save(route);

        activity.setRoute(route.getId());
        this.activityService.save(activity);
        return ResponseEntity.ok(route);
    }

    public ResponseEntity<User> modifyRoute(){return null;}

    public ResponseEntity<Route> deleteRoute(){
        return null;
    }

    @DeleteMapping("/{routeId}/activities/{activityId}")
    public ResponseEntity<RouteWithActivityDTO> removeActivityFromRoute(@PathVariable Long routeId, @PathVariable Long activityId) {
        Route route;
        Activity activity;
        try {
            route = routeService.findById(routeId).orElseThrow();
            activity = activityService.findById(activityId).orElseThrow();
            if (!route.getActivities().contains(activityId)) return ResponseEntity.notFound().build();

            //Confirmed that route and activity exists, and that they're related
            route.removeActivity(activityId);
            activity.setRoute(null);
            routeService.save(route);
            activityService.save(activity);
            return ResponseEntity.ok(dtoService.toDTO(route));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{routeId}/activities/")
    public ResponseEntity<RouteWithActivityDTO> addActivitiesToRoute(@PathVariable Long routeId, @RequestBody List<Long> activityIds) {
        Route route;
        List<Activity> activities;
        try {
            route = routeService.findById(routeId).orElseThrow();
            activities = activityService.findById(activityIds);
            if (activities.isEmpty()) return ResponseEntity.notFound().build();

            //Confirmed that route and activity exists, and that they're related
            for (var act : activities) {
                if (act.getRoute()!=null) { //delete the activity from its previous route
                    Optional<Route> routeOpt = routeService.findById(act.getRoute());
                    routeOpt.ifPresent(value -> value.removeActivity(act.getId()));
                }
                route.addActivity(act.getId());
                act.setRoute(route.getId());
                activityService.save(act);
            }
            routeService.save(route);
            return ResponseEntity.ok(dtoService.toDTO(route));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends RouteDtoInterface>> deleteRoute(@PathVariable Long id) {
        Route route = routeService.findById(id).orElse(null);
        if (route==null) return ResponseEntity.notFound().build();
        for (var act : activityService.findById(route.getActivities())) {
            act.setRoute(null);
            activityService.save(act);
        }
        this.activityService.routeDeleted(id);

        this.routeService.delete(id);
        return getAllRoutes("");
    }
}

