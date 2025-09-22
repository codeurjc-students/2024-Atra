package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.exception.*;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.GetActivitiesParams;
import codeurjc_students.ATRA.model.auxiliary.PagedActivities;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.service.*;
import codeurjc_students.ATRA.service.auxiliary.AtraUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivity(Principal principal, @PathVariable("id") Long id, @RequestParam(value="mural", required=false) Long muralId){
        User user = principalVerification(principal);
        Activity activity = activityService.getActivity(user, id, muralId);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @GetMapping("/byIds")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByIds(Principal principal, @RequestParam List<Long> ids, @RequestParam(required=false) Long muralId) {
        User user = principalVerification(principal);
        List<Activity> activities = activityService.getActivitiesByIds(user, ids, muralId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("ATRA-requested-forbidden", Boolean.toString(ids.size()!=activities.size()));
        return new ResponseEntity<>(ActivityDTO.toDto(activities), headers, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getActivitiesPaged(
            Principal principal,
            @RequestParam boolean fetchAll,
            @RequestParam(required=false) String from,
            @RequestParam(required=false) Long id,
            @RequestParam(defaultValue = "0", name = "startPage") Integer startPage,
            @RequestParam(defaultValue = "1", name = "pagesToFetch") Integer pagesToFetch,
            @RequestParam(required = false, name = "pageSize") Integer pageSize,
            @RequestParam(required = false, name = "cond") String cond,
            @RequestParam(required = false, name = "visibility") String visibility
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
    public ResponseEntity<ActivityDTO> createActivity(Principal principal, @RequestParam("file") MultipartFile file){
        User user = principalVerification(principal);

        Activity activity = activityService.newActivity(file, user);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }


    @DeleteMapping("/{id}/route")
    public ResponseEntity<ActivityDTO> removeRoute(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        Activity activity = activityService.removeRoute(user, id);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PostMapping("/{activityId}/route")
    public ResponseEntity<ActivityDTO> addRoute(Principal principal, @PathVariable Long activityId, @RequestBody Long routeId) {
        User user = principalVerification(principal);

        Activity activity = activityService.addRoute(user, activityId, routeId);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ActivityDTO> deleteActivity(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        Activity activity = activityService.deleteActivity(user, id);
        return ResponseEntity.ok(new ActivityDTO(activity));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ActivityDTO> changeVisibility(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Visibility requestedVisibility = AtraUtils.parseVisibility(body);
        Activity updatedAct = activityService.changeVisibility(user, id, requestedVisibility);
        return ResponseEntity.ok(new ActivityDTO(updatedAct));
    }

    @PatchMapping("/visibility/mural")
    public ResponseEntity<String> makeActivitiesNotVisibleToMural(Principal principal, @RequestParam("id") Long muralId, @RequestBody List<Long> selectedActivitiesIds) {
        User user = principalVerification(principal);
        activityService.makeActivitiesNotVisibleToMural(user, muralId, selectedActivitiesIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/OwnedInMural")
    public ResponseEntity<Collection<ActivityDTO>> getOwnedActivitiesInMural(Principal principal, @RequestParam("muralId") Long muralId) {
        User user = principalVerification(principal);
        List<Activity> activities = activityService.getOwnedActivitiesInMural(user, muralId);
        return ResponseEntity.ok(ActivityDTO.toDto(activities));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ActivityDTO> editActivity(Principal principal, @PathVariable Long id, @RequestBody Activity activity) {
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

