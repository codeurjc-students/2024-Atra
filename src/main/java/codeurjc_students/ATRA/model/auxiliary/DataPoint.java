package codeurjc_students.atra.model.auxiliary;

import codeurjc_students.atra.service.MapToStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Parallel to a GPX's trkpt, stores values for each metric at a specific time.
 */
@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DataPoint {
	private Instant time;

	private Double lat;
	private Double lon;
	private Double ele;

	private int hr;
	private int cad;

	@Convert(converter = MapToStringConverter.class) //this means that, when serializing, it will be converted into a String
	private Map<String, String> other = new HashMap<>();

	public DataPoint(Double lat, Double lon){
		this.lat =lat;
		this.lon =lon;
	}

	public void put(String key, String value){
		switch (key) {
			case "lat" : lat = Double.valueOf(value);  break;
			case "long": lon = Double.valueOf(value);  break;
			case "ele" : ele = Double.valueOf(value);  break;

			case "time" : time = Instant.parse(value);  break;

			case "hr"  : hr  = Integer.parseInt(value); break;
			case "cad" : cad = Integer.parseInt(value); break;

			default: other.put(key,value);
		}
	}

}