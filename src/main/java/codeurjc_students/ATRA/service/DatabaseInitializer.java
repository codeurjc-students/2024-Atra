package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
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
            if (i%3==0) createdAct.changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
            activityMap.put(createdAct.getName(),createdAct);
            muralService.newMural(new Mural(Integer.toString(i), muralGuy, List.of(user2)));
        }
        boolean a = true;
        boolean b = true;
        Route routeA = null;
        Route routeB = null;
        for (var entry : activityMap.entrySet()) {
            Activity activity = entry.getValue();
            if (entry.getKey().contains("Morning Run")) {
                if (a) {
                    a=false;
                    routeA=routeService.newRoute(activity,activityService);
                    routeA.setOwner(user);
                    routeService.save(routeA);
                }
                routeService.addRouteToActivity(routeA, activity, activityService);
            }
            if (entry.getKey().contains("vuelta")) {
                if (b) {
                    b = false;
                    routeB = routeService.newRoute(activity, activityService);
                    routeB.setOwner(user);
                    routeService.save(routeB);
                }
                routeService.addRouteToActivity(routeB, activity, activityService);
            }

        }
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
