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
package NMQC.utils;

import ij.util.*;
import ij.measure.*;
import ij.plugin.filter.*;
import java.util.*;

/**
 *
 * @author alex
 */
public class Fitter {

/**
     *
     * @param xi array with x values
     * @param yi array with y values
     * @param showplot boolean to show the plot
     * @return an array containing the parameters of the fit
     */
    public static double[] LinearFit(double[] xi, double[] yi, boolean showplot) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(CurveFitter.STRAIGHT_LINE, false);
        if (showplot) {
            Plotter.plot(cf, false);
        }
        return cf.getParams();
    }

    /**
     * 
     * Default using of Linear fit: always plot
     * @param xi array with x values
     * @param yi array with y values
     * @return an array containing the parameters of the fit
     */
    public static double[] LinearFit(double[] xi, double[] yi) {
        return LinearFit(xi, yi, true);
    }

    /**
     * 
     * Returns the residual values in a linear fit
     * @param xi array with x values
     * @param yi array with y values
     * @return an array containing the residuals of the fit
     */
    public static double[] getResidualsinLinearFit(double[] xi, double[] yi) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(CurveFitter.STRAIGHT_LINE, false);
        return cf.getResiduals();
    }

    private static final double fac = 2 * Math.sqrt(2 * Math.log(2));

    /**
     *
     * @param xi array with x values
     * @param yi array with y values
     * @param showplot boolean to show the plot
     * @return an array containing the parameters of the fit
     */
    public static double[] GaussianFit(double[] xi, double[] yi, boolean showplot) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(CurveFitter.GAUSSIAN, false);
        if (showplot) {
            Plotter.plot(cf, false);
        }
        return cf.getParams();
    }

     /**
     * 
     * Default using of Gaussian fit: always plot
     * @param xi array with x values
     * @param yi array with y values
     * @return an array containing the parameters of the fit
     */
    public static double[] GaussianFit(double[] xi, double[] yi) {
        return GaussianFit(xi, yi, true);
    }

    /**
     * 
     * @param xi array with x values
     * @param yi array with y values
     * @param pixwidth pixel width
     * @param showplot boolean to show the plot
     * @return the resolution based on gaussian fit of the data
     */
    public static double resolution(double[] xi, double[] yi, double pixwidth, boolean showplot) {
        double[] params = GaussianFit(xi, yi, showplot);
        return params[3] * fac * pixwidth;
    }

    /**
     * 
     * Default using of resolution: always plot
     * @param xi array with x values
     * @param yi array with y values
     * @param pixwidth pixel width
     * @return the resolution based on gaussian fit of the data
     */
    public static double resolution(double[] xi, double[] yi, double pixwidth) {
        return resolution(xi, yi, pixwidth, true);
    }

    /**
     *
     * @param xi
     * @param yi
     * @param showplot
     * @return
     */
    public static double peakpos(double[] xi, double[] yi, boolean showplot) {
        double[] params = GaussianFit(xi, yi, showplot);
        return params[2];
    }

    /**
     *
     * @param xi
     * @param yi
     * @return
     */
    public static double peakpos(double[] xi, double[] yi) {
        return peakpos(xi, yi, true);
    }

    /**
     *
     * @param yi
     * @return
     */
    public static int[] findPeaks(double[] yi) {
        double[] a = Tools.getMinMax(yi);
        double tolerance = 0.1 * a[1];
        boolean includeEnds = false;
        int[] peakpos = MaximumFinder.findMaxima(yi, tolerance, includeEnds);
        Arrays.sort(peakpos);
        //PlotProfile(yi);
        return peakpos;
    }

}