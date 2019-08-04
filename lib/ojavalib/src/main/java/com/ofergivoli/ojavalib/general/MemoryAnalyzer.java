package com.ofergivoli.ojavalib.general;

import java.text.NumberFormat;
import java.util.Locale;


public class MemoryAnalyzer {

    public static long getUsedMemoryByJvmInBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * @return precision of one digit in the fraction portion of the number.
     */
    public static String getUsedMemoryByJvmInMiBAsFormattedStr() {
        return getMemoryInMiBAsFormattedStr(getUsedMemoryByJvmInBytes());
    }

    public static String getMemoryInMiBAsFormattedStr(long bytes){
        double memoryUsedInMiB = (double)bytes / (1024 * 1024);
        NumberFormat nubmerFormat = NumberFormat.getNumberInstance(Locale.US);
        nubmerFormat.setMinimumFractionDigits(1);
        nubmerFormat.setMaximumFractionDigits(1);
        return nubmerFormat.format(memoryUsedInMiB);
    }

}
