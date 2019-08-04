package com.ofergivoli.ojavalib.general;

/**
 * Allows to record values and then get their mean/total.
 *
 */
public class MeasurementKeeper {

    double totalSoFar = 0;
    private long measurementsNum = 0;

    public void recordMeasurement(double measuredVal) {
        totalSoFar += measuredVal;
        measurementsNum++;
    }

    /**
     * @throws RuntimeException if no measurements were recorded so far.
     */
    public double getMeanSoFar() {
        if (measurementsNum == 0)
            throw new RuntimeException("No measurements recorded so far");
        return totalSoFar / measurementsNum;
    }

    public double getTotalSoFar() {
        return totalSoFar;
    }

    public long getMeasurementsNumSoFar() {
        return measurementsNum;
    }
}
