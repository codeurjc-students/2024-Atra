package codeurjc_students.atra.model;

import codeurjc_students.atra.dto.ActivityDTO;
import codeurjc_students.atra.model.auxiliary.DataPoint;
import codeurjc_students.atra.service.ActivityService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class ActivitySummary {
    @Id
    private Long id;
    private Instant startTime;
    private Double totalDistance;
    private Long totalTime; //seconds
    private Double elevationGain;
    @ElementCollection
    @CollectionTable(name = "activity_summary_averages", joinColumns = @JoinColumn(name = "activity_id"))
    @MapKeyColumn(name = "metric")
    @Column(name = "value")
    private Map<String, Double> averages = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "activity_summary_records", joinColumns = @JoinColumn(name = "activity_id"))
    @MapKeyColumn(name = "record")
    @Column(name = "value")
    private Map<String, String> records = new HashMap<>();
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    @JsonIgnore
    private Activity activity;

    private static final String DISTANCE = "distance";

    private static final Logger logger =
            LoggerFactory.getLogger(ActivitySummary.class);



    public ActivitySummary(Activity activity) {
        startTime = activity.getStartTime();

        if (activity.getDataPoints().isEmpty()) throw new IllegalArgumentException("Activity datapoints can't be empty");
        Map<String, List<String>> streams = ActivityDTO.setUpStreams(activity.getDataPoints());

        totalDistance = Double.valueOf(streams.get(DISTANCE).get(streams.get(DISTANCE).size()-1));
        elevationGain = streams.get("elevation_gain").stream().map(Double::valueOf).filter(v -> v>=0).reduce(0.0, Double::sum);
        totalTime = calcTotalTime(activity.getStartTime(), activity.getDataPoints());

        averages = setUpAverages(streams);
        records = setUpRecords(streams);
        this.activity = activity;
    }

    public ActivitySummary(ActivityDTO activity) {
        id = activity.getId();
        startTime = activity.getStartTime();

        Map<String, List<String>> streams = activity.getStreams();

        totalDistance = Double.valueOf(streams.get(DISTANCE).get(streams.get(DISTANCE).size()-1));
        elevationGain = streams.get("elevation_gain").stream().map(Double::valueOf).filter(v -> v>=0).reduce(0.0, Double::sum);
        totalTime = calcTotalTime(activity.getStartTime(), activity.getDataPoints());

        averages = setUpAverages(streams);
        records = setUpRecords(streams);
    }

    private Map<String, String> setUpRecords(Map<String, List<String>> streams) {
        List<String> positionStream = streams.get("position");
        List<String> timeStream = streams.get("time");

        List<String> distanceRecordTitles = Arrays.asList("1km", "5km", "10km", "21km", "42km");
        List<String> timeRecordTitles = Arrays.asList("1min", "5min", "10min", "30min", "1hour");
        Map<String, String> recordsMap = new HashMap<>();

        recordsMap.put(distanceRecordTitles.get(0), distanceGoal(positionStream, timeStream, 1).toString());
        recordsMap.put(distanceRecordTitles.get(1), distanceGoal(positionStream, timeStream, 5).toString());
        recordsMap.put(distanceRecordTitles.get(2), distanceGoal(positionStream, timeStream, 10).toString());
        recordsMap.put(distanceRecordTitles.get(3), distanceGoal(positionStream, timeStream, 21).toString());
        recordsMap.put(distanceRecordTitles.get(4), distanceGoal(positionStream, timeStream, 42).toString());

        recordsMap.put(timeRecordTitles.get(0), timeGoal(positionStream, timeStream, 1).toString());
        recordsMap.put(timeRecordTitles.get(1), timeGoal(positionStream, timeStream, 5).toString());
        recordsMap.put(timeRecordTitles.get(2), timeGoal(positionStream, timeStream, 10).toString());
        recordsMap.put(timeRecordTitles.get(3), timeGoal(positionStream, timeStream, 30).toString());
        recordsMap.put(timeRecordTitles.get(4), timeGoal(positionStream, timeStream, 60).toString());

        return recordsMap;
    }

    //this one alongside distanceGoal should go in some service or be static or otherwise be accessible by others.
    //especially if we ever want to allow users to create their own goals (which is supposed to be a functionality)
    /**
     *
     * @param positionStream
     * @param timeStream
     * @param mins
     * @return (meters) best distance covered in the specified time.
     */
    private Double timeGoal(List<String> positionStream, List<String> timeStream, int mins) {

        //preprocess streams
        List<Double[]> latlon = parseLatLon(positionStream);
        List<Double> segmentDistances = calcSegmentDistances(latlon);
        List<Instant> times = timeStream.stream().map(Instant::parse).toList();

        int i = 0;
        double bestDistance = 0 ; //km
        double accumulatedDistance = 0; //km
        Instant startTime = times.get(i);
        for (int j = 1; j < latlon.size(); j++) {
            accumulatedDistance += segmentDistances.get(j-1);
            Instant currentTime = times.get(j);
            long deltaT = Duration.between(startTime, currentTime).toMinutes();
            while (deltaT>= mins) {
                if (accumulatedDistance>bestDistance) {
                    bestDistance = accumulatedDistance;
                }
                accumulatedDistance -= segmentDistances.get(i);
                i++;
                startTime = times.get(i);
                deltaT = Duration.between(startTime, currentTime).toMinutes();
            }
        }
        return bestDistance==0 ? -1:bestDistance; //km
    }

    //this one alongside timeGoal should go in some service or be static or otherwise be accessible by others.
    //especially if we ever want to allow users to create their own goals (which is supposed to be a functionality)
    /**
     *
     * @param positionStream
     * @param timeStream
     * @param goal
     * @return (seconds) best time in which the given distance is covered.
     */
    private Long distanceGoal(List<String> positionStream, List<String> timeStream, int goal) {
        //preprocess streams
        List<Double[]> latlon = parseLatLon(positionStream);
        List<Double> segmentDistances = calcSegmentDistances(latlon);
        List<Instant> times = timeStream.stream().map(Instant::parse).toList();

        int i = 0;
        long bestTime = Long.MAX_VALUE ; //seconds
        double accumulatedDistance = 0; //meters
        for (int j = 1; j < latlon.size(); j++) {
            accumulatedDistance += segmentDistances.get(j-1);
            while (accumulatedDistance>=goal) {
                Instant i1 = times.get(i);
                Instant i2 = times.get(j);

                long currentTime = Duration.between(i1, i2).getSeconds();
                bestTime = Math.min(bestTime, currentTime);

                if (i + 1 >= j) break;
                accumulatedDistance -= segmentDistances.get(i);
                i++;
            }
        }
        return bestTime==Long.MAX_VALUE ? -1:bestTime; //seconds
    }

    private List<Double[]> parseLatLon(List<String> positionStream) {
        return positionStream.stream()
                .map(item -> {
                    String[] latLon = item.split(";");
                    return new Double[] {
                            Double.parseDouble(latLon[0]),
                            Double.parseDouble(latLon[1])
                    };
                })
                .toList();
    }

    private List<Double> calcSegmentDistances(List<Double[]> latlon) {
        List<Double> segmentDistances = new ArrayList<>();
        for (int k = 1; k < latlon.size(); k++) {
            segmentDistances.add(
                    ActivityService.totalDistance(
                            latlon.get(k)[0], latlon.get(k)[1],
                            latlon.get(k-1)[0], latlon.get(k-1)[1]
                    )
            );
        }
        return segmentDistances;
    }

    private Map<String, Double> setUpAverages(Map<String, List<String>> streams) {
        List<String> averageableMetrics = Arrays.asList("altitude", "heartrate", "cadence", "pace");

        Map<String, Double> averagesMap = new HashMap<>();

        for (String metric : averageableMetrics) {
            if (metric.equals("pace")) { //cause "ratio of averages != average of ratios"
                averagesMap.put("pace", totalTime/totalDistance);
                continue;
            }
            List<String> values = streams.get(metric);
            if (values==null || values.isEmpty()) {
                logger.debug("Empty/missing averageable metric: {}", metric);
                continue;
            }
            double runningTotal = 0.0;
            for (var v : values) runningTotal += Double.parseDouble(v);
            averagesMap.put(metric, runningTotal/values.size());
        }
        return averagesMap;
    }


    private long calcTotalTime(Instant start, List<DataPoint> dataPoints) {
        Instant end = dataPoints.get(dataPoints.size()-1).getTime();
        Duration duration = Duration.between(start, end);
        return duration.toSeconds();
    }

}
