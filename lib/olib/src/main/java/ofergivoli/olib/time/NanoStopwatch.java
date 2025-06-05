package ofergivoli.olib.time;

/**
 * Based on System.nanoTime().
 * WARNING: unexpected behavior observed on Windows 8.1 with Intel Core i7:
 *          When you sample the resolution for few seconds, at some point the resolution is 4.7 seconds!!
 */
@SuppressWarnings("unused")
public class NanoStopwatch extends Stopwatch {

    public NanoStopwatch() {
        throw new RuntimeException("do not use this class until figuring out the unexpected behaviour noted in class documentation.");
    }

    @Override
    protected long getCurrentTimeStamp() {
        return System.nanoTime();
    }

    @Override
    protected double getTimeInSecondsBetweenTwoTimeStamps(long from, long to) {
        return ((double)(to-from)) / 1e9;
    }
}
