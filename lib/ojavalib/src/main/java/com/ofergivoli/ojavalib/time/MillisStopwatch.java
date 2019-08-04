package com.ofergivoli.ojavalib.time;

/**
 * Based on: System.currentTimeMillis().
 * WARNING: Undefined behavior in case the system's time changed during the lifetime of the object, in any way other
 * than the normal way time passes :).
 */
@SuppressWarnings("unused")
public class MillisStopwatch extends Stopwatch {
    @Override
    protected long getCurrentTimeStamp() {
        return System.currentTimeMillis();
    }

    @Override
    protected double getTimeInSecondsBetweenTwoTimeStamps(long from, long to) {
        return (double)((to-from)) / 1000.0;
    }
}
