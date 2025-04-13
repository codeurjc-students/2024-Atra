package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class DtoService {

    @Autowired
    private RouteService routeService;
    @Autowired
    private ActivityService activityService;

    public ActivityDTO toDTO(Activity activity) {
        Route route = activity.getRoute();
        if (route==null) return new ActivityDTO(activity);
        return new ActivityDTO(activity, new BasicNamedId(route));
    }

    public RouteWithActivityDTO toDTO(Route route) {
        List<Activity> activities = route.getActivities();
        List<ActivityOfRouteDTO> activityDTOs = (List<ActivityOfRouteDTO>) toDTO(activities, DtoType.ACTIVITY_OF_ROUTE);
        return new RouteWithActivityDTO(route, activityDTOs);
    }

    public List<? extends ActivityDtoInterface> toDTO(List<Activity> activities, DtoType dtoType) {
        if (dtoType.equals(DtoType.ACTIVITY)) return toDtoActivity(activities);
        if (dtoType.equals(DtoType.ACTIVITY_OF_ROUTE)) {
            List<ActivityOfRouteDTO> result = new ArrayList<>();
            for (var activity : activities) {
                result.add(new ActivityOfRouteDTO(activity));
            }
            return result;
        }
        throw new RuntimeException("DtoType " + dtoType + " is not a DTO of Route, or is not being accounted for.");
    }

    public List<RouteWithActivityDTO> toDtoRoute(List<Route> routes) {
        ArrayList<RouteWithActivityDTO> result = new ArrayList<>();
        for (var route : routes) {
            result.add(toDTO(route));
        }
        return result;
    }

    public List<ActivityDTO> toDtoActivity(List<Activity> activities) {
        ArrayList<ActivityDTO> result = new ArrayList<>();
        for (var activity : activities) {
            result.add(toDTO(activity));
        }
        return result;
    }

    public List<? extends RouteDtoInterface> toDto(List<Route> routes, DtoType dtoType) {
        if (dtoType.equals(DtoType.ROUTE_WITH_ACTIVITY)) return toDtoRoute(routes);
        if (dtoType.equals(DtoType.ROUTE_WITHOUT_ACTIVITY)) {
            List<RouteWithoutActivityDTO> result = new ArrayList<>();
            for (var route : routes) {
                result.add(new RouteWithoutActivityDTO(route));
            }
            return result;
        }
        throw new RuntimeException("DtoType " + dtoType + " is not a DTO of Route, or is not being accounted for.");
    }

    public MuralDTO toDto(Mural mural){
        return new MuralDTO(mural);
    }

    public List<MuralDTO> toDto(Collection<Mural> murals){
        List<MuralDTO> returnValue = new ArrayList<>();
        murals.forEach(mural -> returnValue.add(new MuralDTO(mural)));
        return returnValue;
    }
}
