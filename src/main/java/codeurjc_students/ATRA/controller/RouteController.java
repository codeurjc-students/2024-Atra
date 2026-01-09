package codeurjc_students.atra.controller;

import codeurjc_students.atra.dto.*;
import codeurjc_students.atra.exception.*;
import codeurjc_students.atra.model.*;
import codeurjc_students.atra.model.auxiliary.Visibility;
import codeurjc_students.atra.model.auxiliary.VisibilityType;
import codeurjc_students.atra.service.*;
import codeurjc_students.atra.service.auxiliary.AtraUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
@Tag(name = "Routes", description = "Route management endpoints")
public class RouteController {

	private final UserService userService;
    private final RouteService routeService;
    private final ActivityService activityService;


    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID", description = "Retrieve a route's information by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route found"),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<RouteWithoutActivityDTO> getRoute(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long id){
        User user = principal==null ? null:principalVerification(principal);
        return ResponseEntity.ok(new RouteWithoutActivityDTO(routeService.getRoute(user, id)));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Get activities in route", description = "Retrieve all activities assigned to a specific route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activities retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<List<ActivityOfRouteDTO>> getActivitiesAssignedToRoute(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long id, 
        @Parameter(description = "Mural ID") @RequestParam("mural") Long muralId){
        User user = principalVerification(principal);
        return ResponseEntity.ok(ActivityOfRouteDTO.toDto(routeService.getActivitiesAssignedToRoute(id, user, muralId)));
    }

    @GetMapping
    @Operation(summary = "Get all routes", description = "Retrieve routes with optional filtering by type, visibility, and source")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Routes retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid visibility type"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<? extends RouteDtoInterface>> getAllRoutes(
            Principal principal,
            @Parameter(description = "Type of routes: noActivities, withActivities") @RequestParam(name="type", required = false) String type,
            @Parameter(description = "Filter by source (user, mural, etc)") @RequestParam(name="from", required = false) String from,
            @Parameter(description = "Filter by source ID") @RequestParam(name="id", required = false) Long id,
            @Parameter(description = "Filter by visibility: PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC") @RequestParam(name="visibility", required = false) String visibility
    ){
        User user = principalVerification(principal);

        VisibilityType visibilityType;
        try {visibilityType = visibility==null?null:VisibilityType.valueOf(visibility.toUpperCase(Locale.ROOT));}
        catch (IllegalArgumentException e) {throw new IncorrectParametersException("Visibility was either not specified or not a valid VisibilityType. Valid values are PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC. \"");		}

        List<Route> routes = routeService.getAllRoutes(user, type, from, id, visibilityType);
        if ("noActivities".equals(type))  return ResponseEntity.ok(RouteWithoutActivityDTO.toDto(routes));
        List<List<Activity>> activityList = activityService.getActivitiesFromRoutes(routes, user, from, id);

        return ResponseEntity.ok(RouteWithActivityDTO.toDto(routes, activityList.stream().map(ActivityOfRouteDTO::toDto).toList()));
    }

    @PostMapping
    @Operation(summary = "Create new route", description = "Create a new route from an existing activity. Pass the activity ID in the route object's id field; it will be cleared and the route will be persisted with a new ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route created successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<RouteWithActivityDTO> createRoute(
        Principal principal, 
        @Parameter(description = "The route object must hold the target activity ID in its id field.") @RequestBody Route route){
        User user = principalVerification(principal);
        Long activityId = route.getId();
        route.setId(null);

        return ResponseEntity.ok(new RouteWithActivityDTO(
                routeService.createRoute(user, activityId, route),
                List.of(new ActivityOfRouteDTO(activityService.get(activityId))
                )));

    }

    @DeleteMapping("/{routeId}/activities/{activityId}")
    @Operation(summary = "Remove activity from route", description = "Remove an activity from a route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Activity removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Route or activity not found")
    })
    public ResponseEntity<Boolean> removeActivityFromRoute(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long routeId, 
        @Parameter(description = "Activity ID") @PathVariable Long activityId) {
        User user = principalVerification(principal);
        activityService.removeActivityFromRoute(user, routeId, activityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{routeId}/activities/")
    @Operation(summary = "Add activities to route", description = "Add multiple activities to a route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Activities added successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<Boolean> addActivitiesToRoute(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long routeId, 
        @RequestBody List<Long> activityIds) {
        User user = principalVerification(principal);
        routeService.addActivitiesToRoute(user, routeId, activityIds);
        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete route", description = "Delete a route by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Route deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<Boolean> deleteRoute(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long id) {
        User user = principalVerification(principal);

        routeService.deleteRoute(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Change route visibility", description = "Update the visibility settings of a route. Request body should contain 'visibility' (PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC) and optionally 'allowedMuralsList' as a comma-separated string in brackets for MURAL_SPECIFIC visibility")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Visibility changed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<Boolean> changeVisibility(
        Principal principal, 
        @Parameter(description = "Route ID") @PathVariable Long id, 
        @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Visibility requestedVisibility = AtraUtils.parseVisibility(body);
        routeService.changeVisibility(user, id, requestedVisibility);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/visibility/mural")
    @Operation(summary = "Hide routes from mural", description = "Make routes not visible to a specific mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Routes visibility updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<String> makeRoutesNotVisibleToMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @RequestParam("id") Long muralId, 
        @RequestBody List<Long> selectedRoutesIds) {
        User user = principalVerification(principal);
        routeService.makeRoutesNotVisibleToMural(user, muralId, selectedRoutesIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/OwnedInMural")
    @Operation(summary = "Get owned routes in mural", description = "Retrieve routes owned by the current user in a specific mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Routes retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<Collection<RouteWithoutActivityDTO>> getOwnedRoutesInMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @RequestParam("muralId") Long muralId) {
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

