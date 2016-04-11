package NMQC.utils;

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
        for (int i = 0; i < array.length; i++) {
            suma = suma + array[i] * array[i];
        }
        return Math.sqrt(suma / (array.length * (array.length - 1)));
    }

}
