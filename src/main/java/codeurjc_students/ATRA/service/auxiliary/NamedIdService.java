package codeurjc_students.ATRA.service.auxiliary;

import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NamedIdService {

    @Autowired
    private RouteService routeService;
    @Autowired
    private ActivityService activityService;

    public List<NamedId> getNamedIds(List<NamedId> namedIdList){
        ArrayList<NamedId> ids = new ArrayList<>();
        for (var r : namedIdList) {
            ids.add(new BasicNamedId(r.getId(),r.getName()));
        }
        return ids;
    }

}
