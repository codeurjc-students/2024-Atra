package codeurjc_students.atra.service.auxiliary;

import codeurjc_students.atra.model.Mural;
import codeurjc_students.atra.model.Route;
import codeurjc_students.atra.model.User;
import codeurjc_students.atra.model.auxiliary.Visibility;
import codeurjc_students.atra.model.auxiliary.VisibilityType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AtraUtils {

    public static boolean isRouteVisibleBy(Route route, User user) {
        return route.getVisibility().isPublic()
                || route.getCreatedBy().equals(user)
                || (user.isAdmin() && !route.getVisibility().isPrivate());
    }
    public static boolean isRouteVisibleByUserOrOwnedMurals(Route route, User user) {
        return isRouteVisibleBy(route, user)
                || user.getMemberMurals().stream().anyMatch(mural -> isRouteVisibleBy(route, mural));
    }

    public static boolean isRouteVisibleBy(Route route, Mural mural) {
        return route.getVisibility().isVisibleByMural(mural.getId());
    }

    public static Visibility parseVisibility(Map<String, String> body) {
        String visibilityString = body.get("visibility");
        VisibilityType visibilityType = VisibilityType.valueOf(visibilityString);
        List<Long> allowedMuralsIds = null;
        if (visibilityType.equals(VisibilityType.MURAL_SPECIFIC)) {
            String allowedMuralsList = body.get("allowedMuralsList");
            String csvMuralIds = (allowedMuralsList==null||allowedMuralsList.isEmpty()) ? "":allowedMuralsList.substring(1, allowedMuralsList.length()-1);
            if (!csvMuralIds.isEmpty()) allowedMuralsIds = Arrays.stream(csvMuralIds.split(",")).map(Long::parseLong).toList();
        }
        return new Visibility(visibilityType, allowedMuralsIds);
    }
}
