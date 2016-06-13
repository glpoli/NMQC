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
        double suma = 0;
        for (int i = 0; i < array.length; i++) {
            suma = suma + array[i];
        }
        return suma / array.length;
    }

    /**
     *
     * @param array
     * @return the stddev of the array values
     */
    public static double StdDev(double[] array) {
        double suma = 0;
        double avg = averag(array);
        for (int i = 0; i < array.length; i++) {
            suma = suma + (array[i] - avg) * (array[i] - avg);
        }
        return Math.sqrt(suma / (array.length * (array.length - 1)));
    }

    /**
     *
     * @param array
     * @return the squared sum of the array values
     */
    public static double sqrsum(double[] array) {
        double suma = 0;
        for (int i = 0; i < array.length; i++) {
            suma = suma + array[i] * array[i];
        }
        return suma;
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

}
