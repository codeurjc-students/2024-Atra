package codeurjc_students.ATRA.model.auxiliary;

import jakarta.persistence.Embeddable;



@Embeddable
public interface NamedId {
    Long getId();
    String getName();

}
