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
import ij.gui.*;
import ij.util.*;
import ij.measure.*;
import java.awt.*;

/**
 *
 * @author alex
 */
public class Plotter {

    /**
     *
     * @param cf Current curve fitter
     * @param eightBitCalibrationPlot resample image to 8 bit 
     */
    public static void plot(CurveFitter cf, boolean eightBitCalibrationPlot) {
        double[] x = cf.getXPoints();
        double[] y = cf.getYPoints();
        if (cf.getParams().length < cf.getNumParams()) {
            Plot plot = new Plot(cf.getFormula(), "X", "Y", x, y);
            plot.setColor(Color.RED);
            plot.addLabel(0.02, 0.1, cf.getName());
            plot.addLabel(0.02, 0.2, cf.getStatusString());
            plot.show();
            return;
        }
        int npoints = Math.min(Math.max(x.length, 100), 1000);
        double[] a = Tools.getMinMax(x);
        double xmin = a[0], xmax = a[1];
        if (eightBitCalibrationPlot) {
            npoints = 256;
            xmin = 0;
            xmax = 255;
        }
        a = Tools.getMinMax(y);
        double ymin = a[0], ymax = a[1]; //y range of data points
        double[] px = new double[npoints];
        double[] py = new double[npoints];
        double inc = (xmax - xmin) / (npoints - 1);
        double tmp = xmin;
        for (int i = 0; i < npoints; i++) {
            px[i] = tmp;
            tmp += inc;
        }
        double[] params = cf.getParams();
        for (int i = 0; i < npoints; i++) {
            py[i] = cf.f(params, px[i]);
        }
        a = Tools.getMinMax(py);
        double dataRange = ymax - ymin;
        ymin = Math.max(ymin - dataRange, Math.min(ymin, a[0])); //expand y range for curve, but not too much
        ymax = Math.min(ymax + dataRange, Math.max(ymax, a[1]));
        Plot plot = new Plot(cf.getFormula(), "X", "Y", px, py);
        plot.setLimits(xmin, xmax, ymin, ymax);
        plot.setColor(Color.RED);
        plot.addPoints(x, y, PlotWindow.CIRCLE);
        plot.setColor(Color.BLUE);

        StringBuilder legend = new StringBuilder(100);
        legend.append(cf.getName());
        legend.append('\n');
        legend.append(cf.getFormula());
        legend.append('\n');
        double[] p = cf.getParams();
        int n = cf.getNumParams();
        char pChar = 'a';
        for (int i = 0; i < n; i++) {
            legend.append(pChar).append(" = ").append(IJ.d2s(p[i], 5, 9)).append('\n');
            pChar++;
        }
        legend.append("R^2 = ").append(IJ.d2s(cf.getRSquared(), 4));
        legend.append('\n');
        plot.addLabel(0.02, 0.1, legend.toString());
        plot.setColor(Color.BLUE);
        plot.show();
    }

    /**
     *
     * @param y array containing profile data
     */
    public static void PlotProfile(double[] y) {
        double[] x = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            x[i] = i;
        }
        Plot plot = new Plot(Commons.LANGUAGES.getString("PROFILE"), "X", "Y", x, y);
        plot.setColor(Color.RED);
        plot.show();
    }

}
