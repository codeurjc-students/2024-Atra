package codeurjc_students.atra.model;

import codeurjc_students.atra.dto.ActivityDTO;
import codeurjc_students.atra.model.auxiliary.DataPoint;
import codeurjc_students.atra.service.ActivityService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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


    public ActivitySummary(Activity activity) {
        //this.id = activity.getId();
        startTime = activity.getStartTime();

        if (activity.getDataPoints().isEmpty()) throw new IllegalArgumentException("Activity datapoints can't be empty");
        Map<String, List<String>> streams = ActivityDTO.setUpStreams(activity.getDataPoints());

        totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
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

        totalDistance = Double.valueOf(streams.get("distance").get(streams.get("distance").size()-1));
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
        Map<String, String> records = new HashMap<>();

        records.put(distanceRecordTitles.get(0), distanceGoal(positionStream, timeStream, 1).toString());
        records.put(distanceRecordTitles.get(1), distanceGoal(positionStream, timeStream, 5).toString());
        records.put(distanceRecordTitles.get(2), distanceGoal(positionStream, timeStream, 10).toString());
        records.put(distanceRecordTitles.get(3), distanceGoal(positionStream, timeStream, 21).toString());
        records.put(distanceRecordTitles.get(4), distanceGoal(positionStream, timeStream, 42).toString());

        records.put(timeRecordTitles.get(0), timeGoal(positionStream, timeStream, 1).toString());
        records.put(timeRecordTitles.get(1), timeGoal(positionStream, timeStream, 5).toString());
        records.put(timeRecordTitles.get(2), timeGoal(positionStream, timeStream, 10).toString());
        records.put(timeRecordTitles.get(3), timeGoal(positionStream, timeStream, 30).toString());
        records.put(timeRecordTitles.get(4), timeGoal(positionStream, timeStream, 60).toString());

        return records;
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
        int i = 0;
        int j = 1;
        double bestDistance = 0 ; //km
        double accumulatedDistance = 0; //km
        Instant startTime = Instant.parse(timeStream.get(i));
        for (j = 1; j < positionStream.size(); j++) {
            double distance = ActivityService.totalDistance(
                    Double.parseDouble(positionStream.get(j).split(";")[0]),
                    Double.parseDouble(positionStream.get(j).split(";")[1]),
                    Double.parseDouble(positionStream.get(j-1).split(";")[0]),
                    Double.parseDouble(positionStream.get(j-1).split(";")[1])
            );
            accumulatedDistance += distance;
            Instant currentTime = Instant.parse(timeStream.get(j));
            long deltaT = Duration.between(startTime, currentTime).toMinutes();
            if (deltaT>= mins) {
                if (accumulatedDistance>bestDistance) {
                    bestDistance = accumulatedDistance;
                }
                i++;
                startTime = Instant.parse(timeStream.get(i));
                accumulatedDistance -= ActivityService.totalDistance(
                        Double.parseDouble(positionStream.get(i).split(";")[0]),
                        Double.parseDouble(positionStream.get(i).split(";")[1]),
                        Double.parseDouble(positionStream.get(i-1).split(";")[0]),
                        Double.parseDouble(positionStream.get(i-1).split(";")[1])
                );
            }
        }
        return bestDistance==Long.MAX_VALUE ? -1:bestDistance; //km
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
        //goal *= 1000;
        int i = 0;
        int j = 1;
        long bestTime = Long.MAX_VALUE ; //seconds
        double accumulatedDistance = 0; //meters
        for (j = 1; j < positionStream.size(); j++) {
            double distance = ActivityService.totalDistance(
                    Double.parseDouble(positionStream.get(j).split(";")[0]),
                    Double.parseDouble(positionStream.get(j).split(";")[1]),
                    Double.parseDouble(positionStream.get(j-1).split(";")[0]),
                    Double.parseDouble(positionStream.get(j-1).split(";")[1])
            );
            accumulatedDistance += distance;
            while (accumulatedDistance>=goal) {
                Instant i1 = Instant.parse(timeStream.get(i));
                Instant i2 = Instant.parse(timeStream.get(j));

                long currentTime = Duration.between(i1, i2).getSeconds();
                bestTime = Math.min(bestTime, currentTime);

                if (i + 1 >= j) break;
                i++;
                accumulatedDistance -= ActivityService.totalDistance(
                        Double.parseDouble(positionStream.get(i).split(";")[0]),
                        Double.parseDouble(positionStream.get(i).split(";")[1]),
                        Double.parseDouble(positionStream.get(i-1).split(";")[0]),
                        Double.parseDouble(positionStream.get(i-1).split(";")[1])
                );
            }
        }
        return bestTime==Long.MAX_VALUE ? -1:bestTime; //seconds
    }

    private Map<String, Double> setUpAverages(Map<String, List<String>> streams) {
        List<String> averageableMetrics = Arrays.asList("altitude", "heartrate", "cadence", "pace");

        Map<String, Double> averages = new HashMap<>();

        for (String metric : averageableMetrics) {
            if (metric.equals("pace")) { //cause "ratio of averages != average of ratios"
                averages.put("pace", totalTime/totalDistance);
                continue;
            }
            List<String> values = streams.get(metric);
            if (values==null || values.isEmpty()) {
                System.out.println("Empty/missing averageable metric: " + metric);
                continue;
            }
            double runningTotal = 0.0;
            for (var v : values) runningTotal += Double.parseDouble(v);
            averages.put(metric, runningTotal/values.size());
        }
        return averages;
    }

    private long calcTotalTime(Instant start, List<DataPoint> dataPoints) {
        Instant end = dataPoints.get(dataPoints.size()-1).getTime();
        Duration duration = Duration.between(start, end);
        return duration.toSeconds();
    }

}
