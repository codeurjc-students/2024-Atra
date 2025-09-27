package codeurjc_students.ATRA.dto;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.ActivitySummary;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.ActivityService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEditDTO implements ActivityDtoInterface {
	private String name;
	private String type;

}