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
 * @author alex.vergara
 */
public class Constants {
    
    public static int NEMAWIDTH = 5;
    
    public static int findMiddlePointinTwoPeaks(double[] array){
        int[] peakpos = Fitter.findPeaks(array);
        if (peakpos.length < 2) {
            IJ.error("Two bars phantom needed");
            return 0;
        }
        FPoint2D maximo1 = new FPoint2D(0, 0);
        FPoint2D maximo2 = new FPoint2D(0, 0);
        for (int value : peakpos) {
            if (array[value] > maximo1.Y) {
                maximo2.assign(maximo1);
                maximo1.assign(value, array[value]);
            } else if (array[value] > maximo2.Y && array[value] < maximo1.Y) {
                maximo2.assign(value, array[value]);
            }
        }

        return (int) (0.5 * (maximo1.X + maximo2.X));
    }
    
}
