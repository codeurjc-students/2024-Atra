package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DatabaseInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private MuralService muralService;

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

        for (int i=0;i<10;i++) {
            if (i%2==0) activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), user.getUsername());
            else activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), user2.getUsername());
            muralService.newMural(new Mural(Integer.toString(i), muralGuy, List.of(user2)));
        }
        for (int i = 0; i < 3; i++) {
            muralService.newMural(new Mural("mural"+i, muralGuy, List.of(user2)));
        }
        File file = new File("target/classes/static/defaultThumbnailImage.png");
        byte[] thumbnailBytes = Files.readAllBytes(file.toPath());
        file = new File("target/classes/static/defaultBannerImage.png");
        byte[] bannerBytes = Files.readAllBytes(file.toPath());

        Mural mural = new Mural(user, List.of(user2));
        mural.setBanner(bannerBytes);
        mural.setThumbnail(thumbnailBytes);
        mural.getActivities().addAll(activityService.findAll().subList(0,3));
        System.out.println("Mural initialized with " + activityService.findAll().stream().filter(a->"Morning Run".equals(a.getName())).toList().size() + " activities");
        Mural mural2 = new Mural(user2, List.of(user));

        muralService.newMural(mural);
        muralService.newMural(mural2);

        userService.save(user);
        userService.save(user2);


    }
}
