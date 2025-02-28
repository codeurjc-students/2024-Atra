package codeurjc_students.ATRA.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {
    private Double lat;
    private Double lon;

    public static List<Coordinates> fromActivity(Activity activity) {
        List<Coordinates> coords = new ArrayList<>();
        for (var dp : activity.getDataPoints()) {
            Double lat = dp.get_lat();
            Double lon = dp.get_long();
            coords.add(new Coordinates(lat, lon));
        }
        return coords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates that)) return false;
        return Objects.equals(lat, that.lat) && Objects.equals(lon, that.lon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }
}
