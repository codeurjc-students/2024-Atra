package codeurjc_students.ATRA.model.auxiliary;

import codeurjc_students.ATRA.service.MapToStringConverter;
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
	private Instant _time;

	private Double _lat;
	private Double _long;
	private Double _ele;

	private int hr;
	private int cad;

	@Convert(converter = MapToStringConverter.class) //this means that, when serializing, it will be converted into a String
	private Map<String, String> other = new HashMap<>();

	public DataPoint(Double lat, Double lon){
		_lat=lat;
		_long=lon;
	}

	public void put(String key, String value){
		switch (key) {
			case "lat" : _lat  = Double.valueOf(value);  break;
			case "long": _long = Double.valueOf(value);  break;
			case "ele" : _ele  = Double.valueOf(value);  break;

			case "time" : _time  = Instant.parse(value);  break;

			case "hr"  : hr  = Integer.parseInt(value); break;
			case "cad" : cad = Integer.parseInt(value); break;

			default: other.put(key,value);
		}
	}

}