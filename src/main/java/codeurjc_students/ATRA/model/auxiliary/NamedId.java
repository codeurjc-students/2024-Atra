package codeurjc_students.atra.model.auxiliary;

import jakarta.persistence.Embeddable;



@Embeddable
public interface NamedId {
    Long getId();
    String getName();

}
