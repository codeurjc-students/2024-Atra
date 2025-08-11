package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() throws IOException {
        //smolInit();
        beegInit();
    }

    private void smolInit() throws IOException {
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
            Activity createdAct = activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), users.get(i%users.size()).getUsername(), true);
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
    @Transactional
    public void beegInit() {

        //<editor-fold desc="Users">
        User admin = new User("admin", passwordEncoder.encode("admin"));
        admin.setName("asd");
        admin.setRoles(List.of("ADMIN"));
        userService.save(admin);
        User asd = new User("asd", passwordEncoder.encode("asd"));
        asd.setName("asd");
        userService.save(asd);
        User qwe = new User("qwe", passwordEncoder.encode("qwe"));
        qwe.setName("qwe");
        userService.save(qwe);
        User zxc = new User("zxc", passwordEncoder.encode("zxc"));
        zxc.setName("zxc");
        userService.save(zxc);
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User userx = new User("user"+(i+1), passwordEncoder.encode("asd"));
            userx.setName("user"+(i+1));
            userService.save(userx);
            users.add(userx);
        }
        //</editor-fold>

        System.out.println("Users initialized");

        //<editor-fold desc="Murals">
        Mural asdMural1 = new Mural("Mural1 asd", asd, new ArrayList<>());
        Mural asdMural2 = new Mural("Mural2 asd", asd, new ArrayList<>());
        Mural qweMural = new Mural("Mural1 qwe", qwe, new ArrayList<>());
        Mural zxcMural = new Mural("Mural1 zxc", zxc, new ArrayList<>());
        ArrayList<Mural> murals = new ArrayList<>();
        for (int i = 0; i < 3; i++) murals.add(new Mural(users.get(i).getUsername() + " Mural", users.get(i), new ArrayList<>()));

        asdMural1.addMember(qwe);
        asdMural1.addMember(zxc);
        for (int i = 0; i < 5; i++) asdMural1.addMember(users.get(i));
        for (int i = 2; i < 10; i++) asdMural2.addMember(users.get(i));
        qweMural.addMember(asd);
        qweMural.addMember(zxc);
        zxcMural.addMember(asd);
        zxcMural.addMember(qwe);

        for (Mural mural : List.of(asdMural1, asdMural2, qweMural, zxcMural)) muralService.save(mural);
        for (Mural mural : murals) muralService.save(mural);
        //</editor-fold>

        System.out.println("Murals initialized");

        //<editor-fold desc="Activities">
        List<Activity> activities = new ArrayList<>();
        for (int i=0;i<19;i++) {
            activities.add(activityService.newActivity(Paths.get("target\\classes\\static\\track" + i + ".gpx"), asd.getUsername(), false));
        }        //all activities are initially created as property of asd, though asd does not know this

        System.out.println("Activities created");

        //<editor-fold desc="set asd activities">
        activities.get(0).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(1).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(2).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(3).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(4).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, new ArrayList<>());
        activities.get(5).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId(), qweMural.getId(), zxcMural.getId()));
        //puede ser por algo no guardado en bd
        for (int i=0;i<7;i++) {
            Activity activity = activities.get(i);
            activity.setName("act" + i +" "+ asd.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
            activity.setUser(asd);
            activityService.save(activity);
        }
        //</editor-fold>
        System.out.println("asd Activities set");
        //<editor-fold desc="set qwe activities">
        activities.get(7).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(8).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(9).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(10).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, new ArrayList<>());
        activities.get(11).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId(), qweMural.getId(), zxcMural.getId()));
        for (int i=7;i<13;i++) {
            Activity activity = activities.get(i);
            activity.setName("act " + i + qwe.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
            activity.setUser(qwe);
            activityService.save(activity);
        }
        //</editor-fold>
        System.out.println("qwe Activities set");
        //<editor-fold desc="set zxc activities">
        activities.get(13).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(14).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(15).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(16).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, new ArrayList<>());
        activities.get(17).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId(), qweMural.getId(), zxcMural.getId()));
        for (int i=13;i<19;i++) {
            Activity activity = activities.get(i);
            activity.setName("act " + i + zxc.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
            activity.setUser(zxc);
            activityService.save(activity);
        }
        //</editor-fold>
        System.out.println("zxc Activities set");
        //<editor-fold desc="set userx activities">
        for (int i=0; i<users.size();i++) {
            User user = users.get(i);
            Activity activity = activityService.newActivity(Paths.get("target\\classes\\static\\track" + 19 + ".gpx"), user.getUsername(), true);
            switch (i % 4) {
                case 0 -> activity.changeVisibilityTo(VisibilityType.PUBLIC);
                case 1 -> activity.changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
                case 2 -> activity.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, murals.stream().map(Mural::getId).toList());
                default -> activity.changeVisibilityTo(VisibilityType.PRIVATE);
            }
            activity.setName("act " + i + user.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
            activityService.save(activity);
        }
        //</editor-fold>
        //</editor-fold>

        System.out.println("Activities initialized");

        //<editor-fold desc="Routes">
        //<editor-fold desc="asd routes>
        Route route = routeService.newRoute(activityService.findByUser(asd).get(0), activityService);
        route.changeVisibilityTo(VisibilityType.PUBLIC);
        route.setOwner(null);
        route.setName("r1 asd (PU)");
        Activity extraActivity = activityService.findByUser(qwe).get(0);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);
        route = routeService.newRoute(activityService.findByUser(asd).get(1), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId(), qweMural.getId(), zxcMural.getId()));
        route.setName("r2 asd (MS)");
        extraActivity = activityService.findByUser(qwe).get(1);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(asd).get(2), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r3 asd (MS)");
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(asd).get(3), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r3 asd (MS)");
        extraActivity = activityService.findByUser(asd).get(4);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);

        extraActivity = activityService.findByUser(asd).get(6);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);
        //</editor-fold>
        //<editor-fold desc = qwe routes>
        route = routeService.newRoute(activityService.findByUser(qwe).get(0), activityService);
        route.changeVisibilityTo(VisibilityType.PUBLIC);
        route.setOwner(null);
        route.setName("r1 qwe (PU)");
        extraActivity = activityService.findByUser(zxc).get(0);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);
        route = routeService.newRoute(activityService.findByUser(qwe).get(1), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId(), qweMural.getId(), zxcMural.getId()));
        route.setName("r2 qwe (MS)");
        extraActivity = activityService.findByUser(zxc).get(1);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(qwe).get(2), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r3 qwe (MS)");
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(qwe).get(3), activityService);
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r3 qwe (MS)");
        extraActivity = activityService.findByUser(qwe).get(4);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);

        extraActivity = activityService.findByUser(qwe).get(5);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);
        //</editor-fold>
        //</editor-fold>

        System.out.println("Routes initialized");
        System.out.println("Initialization complete");
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
