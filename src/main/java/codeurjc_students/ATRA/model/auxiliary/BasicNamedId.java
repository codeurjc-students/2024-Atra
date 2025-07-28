package codeurjc_students.ATRA.model.auxiliary;

import com.fasterxml.jackson.databind.util.Named;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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

    public static List<NamedId> from(Collection<NamedId> entities) {
        List<NamedId> result = new ArrayList<>();
        for (var i : entities) {
            result.add(new BasicNamedId(i));
        }
        return result;
    }
}
