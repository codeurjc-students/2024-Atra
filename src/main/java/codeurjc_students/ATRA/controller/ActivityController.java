package codeurjc_students.atra.controller;

import codeurjc_students.atra.dto.ActivityDTO;
import codeurjc_students.atra.dto.ActivityEditDTO;
import codeurjc_students.atra.exception.*;
import codeurjc_students.atra.model.Activity;
import codeurjc_students.atra.model.User;
import codeurjc_students.atra.model.auxiliary.GetActivitiesParams;
import codeurjc_students.atra.model.auxiliary.PagedActivities;
import codeurjc_students.atra.model.auxiliary.Visibility;
import codeurjc_students.atra.service.*;
import codeurjc_students.atra.service.auxiliary.AtraUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity management endpoints")
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Retrieve an activity's information by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> getActivity(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable("id") Long id, 
        @Parameter(description = "Optional mural ID") @RequestParam(value="mural", required=false) Long muralId){
        User user = principalVerification(principal);
        Activity activity = activityService.getActivity(user, id, muralId);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @GetMapping("/byIds")
    @Operation(summary = "Get multiple activities by IDs", description = "Retrieve multiple activities by their IDs. Returns only activities the user has access to; check ATRA-requested-forbidden header to see if any requested activities were filtered")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activities retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ActivityDTO>> getActivitiesByIds(
        Principal principal, 
        @Parameter(description = "List of activity IDs") @RequestParam List<Long> ids, 
        @Parameter(description = "Optional mural ID") @RequestParam(required=false) Long muralId) {
        User user = principalVerification(principal);
        List<Activity> activities = activityService.getActivitiesByIds(user, ids, muralId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("ATRA-requested-forbidden", Boolean.toString(ids.size()!=activities.size()));
        return new ResponseEntity<>(ActivityDTO.toDto(activities), headers, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get activities with pagination", description = "Retrieve activities with optional filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activities retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ActivityDTO>> getActivitiesPaged(
            Principal principal,
            @Parameter(description = "Fetch all activities without pagination") @RequestParam boolean fetchAll,
            @Parameter(description = "Filter by source (user, mural, etc)") @RequestParam(required=false) String from,
            @Parameter(description = "Filter by source ID") @RequestParam(required=false) Long id,
            @Parameter(description = "Starting page number") @RequestParam(defaultValue = "0", name = "startPage") Integer startPage,
            @Parameter(description = "Number of pages to fetch") @RequestParam(defaultValue = "1", name = "pagesToFetch") Integer pagesToFetch,
            @Parameter(description = "Number of entries per page") @RequestParam(required = false, name = "pageSize") Integer pageSize,
            @Parameter(description = "Filter condition") @RequestParam(required = false, name = "cond") String cond,
            @Parameter(description = "Filter by visibility") @RequestParam(required = false, name = "visibility") String visibility
    ) {
        User user = principalVerification(principal);

        GetActivitiesParams params = new GetActivitiesParams(from, id, startPage, pagesToFetch, pageSize, cond, visibility);
        Collection<Activity> activities;
        HttpHeaders headers = new HttpHeaders();
        if (fetchAll) {
            activities = activityService.getActivities(user, params);
            headers.add("ATRA-Total-Pages",   Integer.toString(-1));
            headers.add("ATRA-Total-Entries", Long.toString(activities.size()));
            headers.add("ATRA-Start-Page",  Integer.toString(0));
            headers.add("ATRA-Pages-Sent",    Long.toString(-1));
            headers.add("ATRA-Entries-Sent",  Integer.toString(activities.size()));
            headers.add("ATRA-Page-Size",  Integer.toString(-1));
        } else {
            PagedActivities pActivities = activityService.getActivitiesPaged(user, params);
            activities = pActivities.getActivities();
            headers.add("ATRA-Total-Pages",   Integer.toString(pActivities.getTotalPages()));
            headers.add("ATRA-Total-Entries", Long.toString(pActivities.getTotalEntries()));
            headers.add("ATRA-Start-Page",  Integer.toString(startPage));
            headers.add("ATRA-Pages-Sent",    Long.toString(pActivities.getPagesSent()));
            headers.add("ATRA-Entries-Sent",  Integer.toString(pActivities.getEntriesSent()));
            headers.add("ATRA-Page-Size",  Integer.toString(pageSize));
        }
        return ResponseEntity.ok().headers(headers).body(ActivityDTO.toDto(activities));

    }

    @PostMapping
    @Operation(summary = "Create new activity", description = "Create a new activity from an uploaded GPX file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ActivityDTO> createActivity(
        Principal principal, 
        @Parameter(description = "GPX file to upload") @RequestParam("file") MultipartFile file){
        User user = principalVerification(principal);

        Activity activity = activityService.newActivity(file, user);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }


    @DeleteMapping("/{id}/route")
    @Operation(summary = "Remove route from activity", description = "Remove an associated route from an activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> removeRoute(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable Long id) {
        User user = principalVerification(principal);
        Activity activity = activityService.removeRoute(user, id);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PostMapping("/{activityId}/route")
    @Operation(summary = "Add route to activity", description = "Associate a route with an activity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route added successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity or route not found")
    })
    public ResponseEntity<ActivityDTO> addRoute(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable Long activityId, 
        @RequestBody Long routeId) {
        User user = principalVerification(principal);

        Activity activity = activityService.addRoute(user, activityId, routeId);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity", description = "Delete an activity by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> deleteActivity(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable Long id) {
        User user = principalVerification(principal);
        Activity activity = activityService.deleteActivity(user, id);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Change activity visibility", description = "Update the visibility settings of an activity. Request body should contain 'visibility' (PUBLIC, PRIVATE, MURAL_PUBLIC, MURAL_SPECIFIC) and optionally 'allowedMuralsList' as a comma-separated string in brackets for MURAL_SPECIFIC visibility")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visibility changed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> changeVisibility(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable Long id, 
        @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Visibility requestedVisibility = AtraUtils.parseVisibility(body);
        Activity updatedAct = activityService.changeVisibility(user, id, requestedVisibility);
        return ResponseEntity.ok(new ActivityDTO(updatedAct));
    }

    @PatchMapping("/visibility/mural")
    @Operation(summary = "Hide activities from mural", description = "Make selected activities not visible to a specific mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Activities visibility updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<String> makeActivitiesNotVisibleToMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @RequestParam("id") Long muralId, 
        @RequestBody List<Long> selectedActivitiesIds) {
        User user = principalVerification(principal);
        activityService.makeActivitiesNotVisibleToMural(user, muralId, selectedActivitiesIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/OwnedInMural")
    @Operation(summary = "Get owned activities in mural", description = "Retrieve activities owned by the current user in a specific mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activities retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<Collection<ActivityDTO>> getOwnedActivitiesInMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @RequestParam("muralId") Long muralId) {
        User user = principalVerification(principal);
        List<Activity> activities = activityService.getOwnedActivitiesInMural(user, muralId);
        return ResponseEntity.ok(ActivityDTO.toDto(activities));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit activity", description = "Update activity details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activity updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> editActivity(
        Principal principal, 
        @Parameter(description = "Activity ID") @PathVariable Long id, 
        @RequestBody ActivityEditDTO activity) {
        User user = principalVerification(principal);
        Activity act = activityService.editActivity(user, id, activity);
        return ResponseEntity.ok(new ActivityDTO(act));
    }
    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
        //404 should never happen. Maybe should be 500
    }

}

