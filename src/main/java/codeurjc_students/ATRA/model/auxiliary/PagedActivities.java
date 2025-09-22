package codeurjc_students.ATRA.model.auxiliary;

import codeurjc_students.ATRA.model.Activity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class PagedActivities {
    private List<Activity> activities = new ArrayList<>();
    private int totalPages;
    private long totalEntries;
    private int startPage;
    private int pagesSent;
    private int entriesSent;
    private int pageSize;

    public void addAll(Collection<Activity> acts) {
        activities.addAll(acts);
    }

    public int size() {
        return activities.size();
    }
}
