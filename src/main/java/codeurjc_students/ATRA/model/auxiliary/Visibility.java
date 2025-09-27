package codeurjc_students.ATRA.model.auxiliary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Embeddable
public class Visibility {
    @Getter
    @Column(name = "visibility_type")
    @Enumerated(EnumType.STRING)
    private VisibilityType type = VisibilityType.PRIVATE;

    @ElementCollection
    private Set<Long> allowedMurals = null;


    public Visibility(VisibilityType type) {
        this.type = type;
        if (type==VisibilityType.MURAL_SPECIFIC) this.allowedMurals = new HashSet<>();
    }

    public Visibility(Collection<Long> allowedMurals) {
        this.type = VisibilityType.MURAL_SPECIFIC;
        this.allowedMurals = new HashSet<>(allowedMurals);
    }

    public Visibility(VisibilityType type, Collection<Long> allowedMurals) {
        this.type = type;
        if (type==VisibilityType.MURAL_SPECIFIC) this.allowedMurals = new HashSet<>(allowedMurals);
    }

    public Collection<Long> getAllowedMurals() {
        return allowedMurals;
    }

    @Transient
    public Collection<Long> getAllowedMuralsNonNull() {
        return allowedMurals == null ? Set.of() : new HashSet<>(allowedMurals);
    }

    public boolean isPublic() {
        return type.equals(VisibilityType.PUBLIC);
    }

    public boolean isPrivate() {
        return type.equals(VisibilityType.PRIVATE);
    }

    public boolean isMuralSpecific() {
        return type.equals(VisibilityType.MURAL_SPECIFIC);
    }

    public boolean isMuralPublic() {
        return type.equals(VisibilityType.MURAL_PUBLIC);
    }

    public boolean isVisibleByMural(Long id) {
        return isMuralPublic() || isPublic() || (allowedMurals!=null && allowedMurals.contains(id));
    }

    public void addMural(Long id) {
        if (!isMuralSpecific()) throw new IllegalStateException("visibility.addMural() can only be called when visibility is MURAL_SPECIFIC. Current visibility is " + type);
        allowedMurals.add(id);
    }
    public void removeMural(Long id) {
        if (!isMuralSpecific()) throw new IllegalStateException("visibility.removeMural() can only be called when visibility is MURAL_SPECIFIC. Current visibility is " + type);
        allowedMurals.remove(id);
    }
    public void addMural(Collection<Long> id) {
        if (!isMuralSpecific()) throw new IllegalStateException("visibility.addMural() can only be called when visibility is MURAL_SPECIFIC. Current visibility is " + type);
        allowedMurals.addAll(id);
    }
    public void removeMural(Collection<Long> id) {
        if (!isMuralSpecific()) throw new IllegalStateException("visibility.removeMural() can only be called when visibility is MURAL_SPECIFIC. Current visibility is " + type);
        allowedMurals.removeAll(id);
    }

    public void changeTo(VisibilityType visibilityType) {
        type = visibilityType;
        if (visibilityType==VisibilityType.MURAL_SPECIFIC) this.allowedMurals = new HashSet<>();
        else this.allowedMurals = null;
    }

    public void changeTo(VisibilityType visibilityType, Collection<Long> allowedMurals) {
        type = visibilityType;
        if (visibilityType==VisibilityType.MURAL_SPECIFIC) {
            this.allowedMurals = new HashSet<>();
            if (allowedMurals!=null) this.allowedMurals.addAll(allowedMurals);
        }
        else this.allowedMurals = null;
    }
    /*
    * A user can only see their own activities
    * A user can only see public routes, or private ones if created by them
    *
    * A Mural can see activities that
    *       1. Belong to a member user
    *       2. Have this mural's id in its visibility list, or are public
    * A Mural can see only see routes such that
    *       - one or more of the Mural's activities have that as its route
    *       - AND the route is public
    *
    * A user may be able to see a non-public activity through a route.
    * HOWEVER, this does not mean they can see that activity
    * There will be at least two endpoints to fetch an activity
    *   1. /api/activities/id, returns the activity if it's yours
    *   2. /api/murals/id/activities/id, returns the activity if the mural can see it and you're in the mural
    *
    */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Visibility that)) return false;
        return type == that.type && Objects.equals(allowedMurals, that.allowedMurals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, allowedMurals);
    }
}
