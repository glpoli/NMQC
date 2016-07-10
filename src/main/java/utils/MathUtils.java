/*
 * Copyright 2016 ImageJ.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.descriptive.rank.*;

/**
 *
 * @author alex
 */
public class MathUtils {

    /**
     * mean
     */
    private static final UnivariateStatistic MEAN = new Mean();
    
    /** min */
    private static final UnivariateStatistic MIN = new Min();

    /** max */
    private static final UnivariateStatistic MAX = new Max();

    /**
     * variance
     */
    private static final Variance VARIANCE = new Variance();
    
    /**
     * skewness
     */
    private static final Skewness SKEWNESS = new Skewness();
    
    /**
     * kurtosis
     */
    private static final Kurtosis KURTOSIS = new Kurtosis();

    /**
     * Standard Deviation
     */
    private static final StandardDeviation STDDEV = new StandardDeviation();

    /**
     *
     * @param array
     * @return the mean of the array values
     */
    public static double averag(final double[] array)
            throws MathIllegalArgumentException {
        return MEAN.evaluate(array);
    }
    
    /**
     *
     * @param array
     * @return the min of the array values
     */
    public static double Min(final double[] array)
            throws MathIllegalArgumentException {
        return MIN.evaluate(array);
    }
    
    /**
     *
     * @param array
     * @return the max of the array values
     */
    public static double Max(final double[] array)
            throws MathIllegalArgumentException {
        return MAX.evaluate(array);
    }

    /**
     *
     * @param array
     * @return the variance of the array values
     */
    public static double Variance(final double[] array)
            throws MathIllegalArgumentException {
        return VARIANCE.evaluate(array);
    }
    
    /**
     *
     * @param array
     * @return the skewness of the array values
     */
    public static double Skewness(final double[] array)
            throws MathIllegalArgumentException {
        return SKEWNESS.evaluate(array);
    }
    
    /**
     *
     * @param array
     * @return the kurtosis of the array values
     */
    public static double Kurtosis(final double[] array)
            throws MathIllegalArgumentException {
        return KURTOSIS.evaluate(array);
    }

    /**
     *
     * @param array
     * @return the stddev of the array values
     */
    public static double StdDev(final double[] array)
            throws MathIllegalArgumentException {
        return STDDEV.evaluate(array);
    }

    /**
     *
     * @param mean
     * @param stddev
     * @return the Modulation Transfer Function given the mean and stddev of a
     * distribution
     */
    public static double MTF(double mean, double stddev) {
        return mean == 0 ? 0 : Math.sqrt(2 * (stddev * stddev - mean)) / mean;
    }

    /**
     *
     * @param min
     * @param max
     * @return the contrast between min and max values
     */
    public static double Contrast(double min, double max) {
        return (min + max) == 0 ? 0 : Math.abs(max - min) * 100 / (min + max);
    }

    /**
     * Returns the angle in degrees between the specified line and a horizontal
     * line. in [0 - 360] interval
     *
     * @param x1 x position of the first point
     * @param y1 y position of the first point
     * @param x2 x position of the second point
     * @param y2 y position of the second point
     * @return the angle in 0-360 range between the vector (x2-x1, y1-y2) and
     * the horizontal line
     */
    public static double getFloatAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y1 - y2;
        double angle = (180.0 / Math.PI) * Math.atan2(dy, dx);
        return angle < 0 ? 360 + angle : angle;
    }

}
