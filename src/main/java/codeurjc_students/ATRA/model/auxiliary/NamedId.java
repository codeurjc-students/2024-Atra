package codeurjc_students.ATRA.model.auxiliary;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Embeddable
public interface NamedId {
    public Long getId();
    public String getName();
    //private Long id;
    //private String name;

    //public NamedId() {
    //    this.id = -1L;
    //    this.name = "Something_Went_Wrong";
    //}
    //public NamedId(Long id) {
    //    this.id = id;
    //    this.name = "Something_Went_Wrong";
    //}
    //public NamedId(Long id, String name) {
    //    this.id = id;
    //    this.name = name;
    //}
}
