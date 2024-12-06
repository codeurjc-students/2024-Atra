package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.ActivityDTO;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.ActivityService;
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

    public Activity getActivity(){
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivity(@PathVariable("id") Long id, Principal principal){
        ResponseEntity<List<ActivityDTO>> activities = getActivities(id, principal);
        return ResponseEntity.status(activities.getStatusCode()).body(activities.getBody().get(0));
    }

    @GetMapping
    public ResponseEntity<List<ActivityDTO>> getActivities(@RequestParam(value="id", required = false) Long id, Principal principal){
        List<Activity> activities = new ArrayList<>();


        if (principal==null) return ResponseEntity.status(403).build();
        Optional<User> userOpt = userService.findByUserName(principal.getName());
        if (userOpt.isEmpty()) return  ResponseEntity.status(403).build(); //this should never happen. Maybe should be 500
        User user = userOpt.get();

        if (id!=null) {
            //check that the user has permission to access this activity
            if (!user.hasActivity(id)) return ResponseEntity.status(403).build();
            //fetch and return the activity
            Optional<Activity> actOpt = activityService.findById(id);
            if (actOpt.isEmpty()) return ResponseEntity.notFound().build();
            activities.add(actOpt.get());
            return ResponseEntity.ok(ActivityDTO.toDTO(activities));
        }
        return ResponseEntity.ok(ActivityDTO.toDTO(activityService.get(user.getActivities())));
    }

    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestParam("file") MultipartFile file, Principal principal){
        if (principal==null) {
            return  ResponseEntity.badRequest().build();
        }

        activityService.newActivity(file, principal.getName());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Activity> modifyActivity(){return null;}

    public ResponseEntity<Activity> deleteActivity(){
        return null;
    }

}

