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

import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author alex
 */
public class MathUtils {

    /**
     *
     * @param array
     * @return the mean of the array values
     */
    public static double averag(double[] array) {
        return StatUtils.mean(array);
    }

    /**
     *
     * @param array
     * @return the stddev of the array values
     */
    public static double StdDev(double[] array) {
        double var = StatUtils.variance(array);
        return var > 0 ? Math.sqrt(var) : 0.0;
    }

    /**
     *
     * @param array
     * @return the squared sum of the array values
     */
    public static double sqrsum(double[] array) {
        return StatUtils.sumSq(array);
    }

    /**
     *
     * @param mean
     * @param stddev
     * @return the Modulation Transfer Function given the mean and stddev of a
     * distribution
     */
    public static double MTF(double mean, double stddev) {
        return Math.sqrt(2 * (stddev * stddev - mean)) / mean;
    }

    /**
     *
     * @param min
     * @param max
     * @return the contrast between min and max values
     */
    public static double Contrast(double min, double max) {
        return Math.abs(max - min) * 100 / (min + max);
    }

    /**
     * Returns the angle in degrees between the specified line and a horizontal
     * line. in [0 - 360] interval
     */
    public static double getFloatAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y1 - y2;
        double angle = (180.0 / Math.PI) * Math.atan2(dy, dx);
        return angle < 0 ? 360 + angle : angle;
    }

}
