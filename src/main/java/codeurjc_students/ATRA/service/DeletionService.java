package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeletionService {

    @Autowired
    private UserService userService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private MuralService muralService;

    public void deleteUser(User user) {
        if (user==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

        //this should happen automatically with cascade.
        user.getActivities().forEach(activity -> {
            activityService.delete(activity.getId());
        });
        user.getOwnedMurals().forEach(mural -> {
            mural.removeOwner(user);
            muralService.save(mural);
        });
        user.getMemberMurals().forEach(mural -> {
            mural.removeMember(user);
            muralService.save(mural);
        });
        userService.delete(user);
    }

    public void deleteUser(long id) {
        User user = userService.findById(id).orElse(null);
        deleteUser(user);
    }

    public void deleteActivity(long id) {
        Optional<Activity> activityOpt = activityService.findById(id);//.orElse(null);
        if (activityOpt.isEmpty()) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now
        Activity activity = activityOpt.get();
        activity.getUser().removeActivity(activity);  //we're assuming activity.user is not null, which it must not be
        userService.save(activity.getUser());

        activity.getMurals().forEach(mural -> {
            mural.removeActivity(activity);
            muralService.save(mural);
        });
        if (activity.hasRoute()) {
            activity.getRoute().removeActivity(activity);
            routeService.save(activity.getRoute());
        }
        activityService.delete(id);
    }

    public void deleteRoute(long id) {
        Route route = routeService.findById(id).orElse(null);
        if (route==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

        route.getActivities().forEach(activity -> {
            activity.removeRoute();
            activityService.save(activity);
        });
        Visibility visibility = route.getVisibility();
        if (visibility.isMuralSpecific()) {
            for (Long muralId : visibility.getAllowedMurals()) {
                Mural mural = muralService.findById(muralId).orElse(null);
                if (mural==null) continue;
                mural.removeRoute(route);
                muralService.save(mural);
            }
        }

        routeService.delete(id);
    }

    public void deleteMural(long id) {
        Mural mural = muralService.findById(id).orElse(null);
        if (mural==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now
        mural.getOwner().removeOwnedMural(mural);
        userService.save(mural.getOwner());

        mural.getMembers().forEach(user -> {
            user.removeMemberMural(mural);
            userService.save(user);
        });
        mural.getActivities().forEach(activity -> {
            activity.removeMural(mural);
            activityService.save(activity);
        });
        mural.getRoutes().forEach(route -> {
            Visibility visibility = route.getVisibility();
            if (visibility.isMuralSpecific()) {
                visibility.removeMural(id);
            }
            routeService.save(route);
        });
        muralService.delete(id);
    }


}
