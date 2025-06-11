package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.fasterxml.jackson.databind.type.LogicalType.Array;

@Service
public class DatabaseInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private MuralService muralService;
    @Autowired
    private RouteService routeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() throws IOException {
        User user = new User("asd", passwordEncoder.encode("asd"));
        user.setName("pepito");
        userService.save(user);

        User user2 = new User("qwe", passwordEncoder.encode("qwe"));
        user2.setName("juanito");
        userService.save(user2);

        User muralGuy = new User("zxc", passwordEncoder.encode("zxc"));
        muralGuy.setName("muralGuy");
        userService.save(muralGuy);

        List<User> users = Arrays.asList(user,user2,muralGuy);
        Map<String, Activity> activityMap = new HashMap<>();
        for (int i=0;i<20;i++) {
            Activity createdAct = activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), users.get(i%users.size()).getUsername());
            activityMap.put(createdAct.getName(),createdAct);
            muralService.newMural(new Mural(Integer.toString(i), muralGuy, List.of(user2)));
        }
        activityMap.forEach((name,activity)->{
            if (name.contains("Morning Run")) routeService.addRouteToActivity("Usual 10k", activity, activityService);
            if (name.contains("vuelta"))      routeService.addRouteToActivity("Miercoles vuelta", activity, activityService);
        });
        for (int i = 0; i < 3; i++) {
            createMural("mural"+i, muralGuy, List.of(user2));
        }

        createMural(null, user, List.of(user2));
        createMural(null, user2, List.of(user));

        userService.save(user);
        userService.save(user2);
    }

    private void createMural(String name, User owner, Collection<User> members) throws IOException {
        File file = new File("target/classes/static/defaultThumbnailImage.png");
        byte[] thumbnailBytes = Files.readAllBytes(file.toPath());
        file = new File("target/classes/static/defaultBannerImage.png");
        byte[] bannerBytes = Files.readAllBytes(file.toPath());

        Mural mural = name!=null ? new Mural(name, owner, members): new Mural(owner, members);
        mural.setBanner(bannerBytes);
        mural.setThumbnail(thumbnailBytes);

        muralService.newMural(mural);
    }
}
