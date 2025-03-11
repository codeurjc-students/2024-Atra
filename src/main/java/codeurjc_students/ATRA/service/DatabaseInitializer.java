package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DatabaseInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        User user = new User("asd", passwordEncoder.encode("asd"));
        user.setDisplayname("pepito");
        userService.save(user);

        for (int i=0;i<10;i++) {
            Activity act = activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), user.getUsername());
            //user.addActivity(act.getId());
        }
        userService.save(user);


    }
}
