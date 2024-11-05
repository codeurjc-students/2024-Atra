package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@RestController
@RequestMapping("/api/activities")
public class ActivityController {

	@Autowired
	private ActivityService activityService;

    public Activity getActivity(){
        return null;
    }

    @PostMapping
    public ResponseEntity<Activity> createActivity(Principal principal){
        activityService.newActivity("target\\classes\\static\\track.gpx", principal.getName());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Activity> modifyActivity(){return null;}

    public ResponseEntity<Activity> deleteActivity(){
        return null;
    }

}

