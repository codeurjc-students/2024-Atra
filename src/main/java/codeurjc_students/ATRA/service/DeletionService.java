package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
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
        activityService.findByUser(user).forEach(activity -> {
            activityService.delete(activity.getId());
        });
        muralService.findOwnedBy(user).forEach(mural -> {
            mural.removeOwner(user, null);
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
        if (!activityService.exists(id)) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now
        activityService.delete(id);
    }

    public void deleteRoute(long id) {
        Route route = routeService.findById(id).orElse(null);
        if (route==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

        activityService.findByRoute(route).forEach(activity -> {
            activity.removeRoute();
            activityService.save(activity);
        });

        routeService.delete(id);
    }

    public void deleteMural(long id) {
        Mural mural = muralService.findById(id).orElse(null);
        if (mural==null) return; //maybe throw an exception, maybe log a warning. repository.deleteById does nothing, so we do nothing for now

        mural.getMembers().forEach(user -> {
            user.removeMemberMural(mural);
            userService.save(user);
        });
        activityService.findVisibleTo(mural).forEach(activity -> {
            Visibility visibility = activity.getVisibility();
            if (visibility.isMuralSpecific()) {
                visibility.removeMural(mural.getId());
                activityService.save(activity);
            }
        });
        routeService.findVisibleTo(mural).forEach(route -> {
            Visibility visibility = route.getVisibility();
            if (visibility.isMuralSpecific()) {
                visibility.removeMural(id);
                routeService.save(route);
            }
        });
        muralService.delete(id);
    }


}
