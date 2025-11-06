package codeurjc_students.atra.service;

import codeurjc_students.atra.model.auxiliary.VisibilityType;

import java.util.Collection;

public interface ChangeVisibilityInterface {
    /**
     *
     * @param activityId
     * @param newVisibility
     * @return false if activityId doesn't match any existing activities, true otherwise
     */
    public boolean changeVisibility(Long activityId, VisibilityType newVisibility); //feel free to change this to just take a Visibility instead of VisibilityType and Collection<Long>

    public boolean changeVisibility(Long entityId, VisibilityType newVisibility, Collection<Long> allowedMuralsCol);
}
