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

import ij.*;

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
        for (double m : array) {
            suma += m;
        }
        return suma / array.length;
    }

    /**
     *
     * @param array
     * @return the stddev of the array values
     */
    public static double StdDev(double[] array) {
        double var = Variance(array);
        return var > 0 ? Math.sqrt(var) : 0.0;
    }

    /**
     *
     * @param array
     * @return the stddev of the array values
     */
    public static double Variance(double[] array) {
        int n = array.length;
        if (n <= 1) {
            return 0;
        }

        double total = 0;
        double total2 = 0;

        for (double m : array) {
            total += m;
            total2 += m * m;
        }

        return (double) ((total2 - (total * total / n)) / (n - 1));
    }

    /**
     *
     * @param array
     * @return the squared sum of the array values
     */
    public static double sqrsum(double[] array) {
        double suma = 0;
        for (double m : array) {
            suma += m * m;
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
    
    public static void PrintMatrix(double[][] mat){
        for (double[] mat1 : mat) {
            String s = "";
            for (double m : mat1) {
                s += m + ", ";
            }
            IJ.log(s);
        }
    }

}
