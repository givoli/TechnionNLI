package com.ofergivoli.ojavalib.math;

import java.util.Collection;

public class MathUtils {

    public static double sum(Collection<Double> c){
        double result = 0;
        for (Double value : c)
            result += value;
        return result;
    }
}
