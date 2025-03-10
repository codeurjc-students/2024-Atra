package codeurjc_students.ATRA.model.auxiliary;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
@Embeddable
public class BasicNamedId implements NamedId{

    private Long id;
    private String name;

    public BasicNamedId() {
        this.id = -1L;
        this.name = "Something_Went_Wrong";
    }
    public BasicNamedId(Long id) {
        this.id = id;
        this.name = "Something_Went_Wrong";
    }
    public BasicNamedId(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public BasicNamedId(NamedId namedId) {
        this.id = namedId.getId();
        this.name = namedId.getName();
    }
}
