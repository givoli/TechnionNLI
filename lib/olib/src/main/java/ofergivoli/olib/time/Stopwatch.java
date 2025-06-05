package ofergivoli.olib.time;

import com.google.common.collect.Sets;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * All time values are in seconds.
 * This class allows you not measure a sequence of time spans, and get statistics about those measurements (average
 * etc.).
 * Also, this class supports the notion of "tags". Each measurement can have multiple tags, and you can get statistics
 * about all measurements associated with a given tag. This is useful when you want time statistics about some
 * action that is done many times, in few distinct modes (each mode will be associated with a tag). Such a case arise
 * when the action is inside nested loops iterating over constant ranges (and then each iterator value can be a tag).
 */
public abstract class Stopwatch {

    /**
     * null iff not currently measuring time.
     */
    @Nullable
    private Long startTimeStamp;

    /**
     * An entry exists for key 'tag' from the moment {@link #start(Collection)} is called with that tag.
     */
    private final SafeMap<String,Double> tagToTotalTimeMeasured = new SafeHashMap<>();

    /**
     * An entry exists for key 'tag' from the moment {@link #start(Collection)} is called with that tag.
     * The value of key 'tag' is the number of times a sequence of [{@link #start()}, {@link #stop()}] calls were made,
     * with 'tag' being sent to {@link #start(Collection)}.
     */
    private final SafeMap<String,Integer> tagToMeasurementsNum = new SafeHashMap<>();


    /**
     * null iff {@link #startTimeStamp} is null.
     */
    @Nullable
    private Set<String> tagsOfCurrentMeasurement;

    @SuppressWarnings("WeakerAccess")
    public static final String defaultTag = "";

    /**
     * Uses the default tag.
     */
    public void start() {
        start(null);
    }

    /**
     * @param tags The tags associated with the measurement that now begins. If null, the default tag
     *             {@link #defaultTag} is used.
     *             A copy of the collection is saved.
     */
    public void start(@Nullable Collection<String> tags) {
        if (startTimeStamp != null)
            throw new RuntimeException("Already started!");

        startTimeStamp = getCurrentTimeStamp();

        if (tags == null)
            tagsOfCurrentMeasurement = Sets.newHashSet(defaultTag);
        else
            tagsOfCurrentMeasurement = new HashSet<>(tags);

        tagsOfCurrentMeasurement.forEach(tag-> {
            if(!tagToMeasurementsNum.safeContainsKey(tag))
                tagToMeasurementsNum.put(tag,0);
            if (!tagToTotalTimeMeasured.safeContainsKey(tag))
                tagToTotalTimeMeasured.put(tag, 0.0);
        });

    }

    /**
     * @return The time in seconds since the last call to {@link #start()}.
     */
    public double stop() {
        double timeSinceLastStart = getTimeSinceLastStart();

        //noinspection ConstantConditions
        tagsOfCurrentMeasurement.forEach(tag-> {
            tagToTotalTimeMeasured.put(tag, tagToTotalTimeMeasured.safeGet(tag) + timeSinceLastStart);
            tagToMeasurementsNum.put(tag, tagToMeasurementsNum.safeGet(tag) + 1);
        });

        startTimeStamp = null;
        tagsOfCurrentMeasurement = null;

        return timeSinceLastStart;
    }

    /**
     * @return The time in seconds since the last call to {@link #start()}.
     */
    public double getTimeSinceLastStart() {
        if (startTimeStamp == null)
            throw new RuntimeException("Not started!");
        return getTimeInSecondsBetweenTwoTimeStamps(startTimeStamp, getCurrentTimeStamp());
    }


    /**
     * Like {@link #getTotalTimeMeasured(String)} but using the default tag.
     */
    public double getTotalTimeMeasured() {
        return getTotalTimeMeasured(defaultTag);
    }

    /**
     * @return the total times measured between consecutive calls to [{@link #start()}, {@link #stop()}], in seconds.
     */
    public double getTotalTimeMeasured(String tag) {
        return tagToTotalTimeMeasured.safeGet(tag);
    }


    /**
     * Like {@link #getAverageMeasurement(String)} but with the default tag.
     */
    public double getAverageMeasurement() {
        return getAverageMeasurement(defaultTag);
    }
    /**
     * @return The average time measured between consecutive calls to [{@link #start(Collection)}, {@link #stop()}],
     * in seconds.
     */
    public double getAverageMeasurement(String tag) {
        int measurementsNum = tagToMeasurementsNum.safeGet(tag);
        if (measurementsNum == 0)
            throw new RuntimeException("no measurements were completed so far for tag:" + tag);
        return tagToTotalTimeMeasured.safeGet(tag) / measurementsNum;
    }


    /**
     * This might be an expensive method (computation wise)
     * @return [maximum,average] time (in seconds) between two updates of the underlying clock - over the sampled time
     * span.
     */
    public Pair<Double,Double> calcResolution() {

        final long loopIterations = 20000000000L;
        final int minTimeAdvancesRequired = 1000;
        final long maxTimeAdvancesSizeToSample = 10000000;


        List<Long> deltas = new LinkedList<>();
        long last = getCurrentTimeStamp();
        for(long i = 0; i<loopIterations && deltas.size()<maxTimeAdvancesSizeToSample; i++) {
            long newTime = getCurrentTimeStamp();
            if(newTime>last)
                deltas.add(newTime - last);
            last = newTime;
        }

        if (deltas.size() < minTimeAdvancesRequired)
            throw new RuntimeException("Didn't see enough time advances.");

        @SuppressWarnings("ConstantConditions")
        double max = getTimeInSecondsBetweenTwoTimeStamps(0,deltas.stream().max(Long::compare).get());
        double average = getTimeInSecondsBetweenTwoTimeStamps(0, deltas.stream().reduce(0L,Long::sum) / deltas.size());

        return new ImmutablePair<>(max,average);
    }

    protected abstract long getCurrentTimeStamp();


    /**
     * The arguments represents the same quantity type as returned by {@link #getCurrentTimeStamp()}, but as double.
     * @return time in seconds.
     */
    protected abstract double getTimeInSecondsBetweenTwoTimeStamps(long from, long to);

}
