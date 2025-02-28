package codeurjc_students.ATRA.controller;

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


    @GetMapping("/{id}")
    public ResponseEntity<Route> getRoute(@PathVariable Long id){
        Optional<Route> routeOpt = routeService.findById(id);
        if (routeOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(routeOpt.get());
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

        route.setId(null);
        this.routeService.save(route);
        return ResponseEntity.ok(route);
    }

    public ResponseEntity<User> modifyRoute(){return null;}

    public ResponseEntity<Route> deleteRoute(){
        return null;
    }

}

