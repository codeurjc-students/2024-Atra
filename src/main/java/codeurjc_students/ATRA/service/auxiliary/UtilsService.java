package codeurjc_students.ATRA.service.auxiliary;

import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.ChangeVisibilityInterface;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UtilsService {

    public static void changeVisibilityHelper(Long id, Map<String, String> body, ChangeVisibilityInterface service) {
        String visibilityString = body.get("visibility");
        try {
            VisibilityType visibilityType = VisibilityType.valueOf(visibilityString);
            List<Long> allowedMuralsIds = null;
            if (visibilityType.equals(VisibilityType.MURAL_SPECIFIC)) {
                String allowedMuralsList = body.get("allowedMuralsList");
                String csvMuralIds = (allowedMuralsList==null||allowedMuralsList.isEmpty()) ? "":allowedMuralsList.substring(1, allowedMuralsList.length()-1);
                if (!csvMuralIds.isEmpty()) allowedMuralsIds = Arrays.stream(csvMuralIds.split(",")).map(Long::parseLong).toList();;
            }
            service.changeVisibility(id, visibilityType, allowedMuralsIds);
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, visibilityString + " is not a valid Visibility Type");
        }
    }
}
