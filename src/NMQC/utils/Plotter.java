package NMQC.utils;

import ij.*;
import ij.gui.*;
import ij.util.*;
import ij.measure.*;
import ij.plugin.filter.*;
import java.util.*;
import java.awt.*;

public class Plotter {

    public static double averag(double[] array) {
        double suma = 0;
        for (int i = 0; i < array.length; i++) {
            suma = suma + array[i];
        }
        return suma / array.length;
    }

    public static double StdDev(double[] array) {
        double suma = 0;
        for (int i = 0; i < array.length; i++) {
            suma = suma + array[i] * array[i];
        }
        return Math.sqrt(suma / (array.length * (array.length - 1)));
    }

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
        int npoints = 100;
        if (npoints < x.length) {
            npoints = x.length; //or 2*x.length-1; for 2 values per data point
        }
        if (npoints > 1000) {
            npoints = 1000;
        }
        double[] a = Tools.getMinMax(x);
        double xmin = a[0], xmax = a[1];
        if (eightBitCalibrationPlot) {
            npoints = 256;
            xmin = 0;
            xmax = 255;
        }
        a = Tools.getMinMax(y);
        double ymin = a[0], ymax = a[1]; //y range of data points
        float[] px = new float[npoints];
        float[] py = new float[npoints];
        double inc = (xmax - xmin) / (npoints - 1);
        double tmp = xmin;
        for (int i = 0; i < npoints; i++) {
            px[i] = (float) tmp;
            tmp += inc;
        }
        double[] params = cf.getParams();
        for (int i = 0; i < npoints; i++) {
            py[i] = (float) cf.f(params, px[i]);
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

    public static double[] LinearFit(double[] xi, double[] yi, boolean showplot) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(0, false);
        if (showplot) {
            plot(cf, false);
        }
        return cf.getParams();
    }

    public static double[] LinearFit(double[] xi, double[] yi) {
        return LinearFit(xi, yi, true);
    }

    public static double[] getResidualsinLinearFit(double[] xi, double[] yi) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(0, false);
        return cf.getResiduals();
    }

    private static final double fac = 2 * Math.sqrt(2 * Math.log(2));

    public static double[] GaussianFit(double[] xi, double[] yi, boolean showplot) {
        CurveFitter cf = new CurveFitter(xi, yi);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        cf.doFit(12, false);
        if (showplot) {
            plot(cf, false);
        }
        return cf.getParams();
    }

    public static double[] GaussianFit(double[] xi, double[] yi) {
        return GaussianFit(xi, yi, true);
    }

    public static double resolution(double[] xi, double[] yi, double pixwidth, boolean showplot) {
        double[] params = GaussianFit(xi, yi, showplot);
        return params[3] * fac * pixwidth;
    }

    public static double resolution(double[] xi, double[] yi, double pixwidth) {
        return resolution(xi, yi, pixwidth, true);
    }

    public static double peakpos(double[] xi, double[] yi, boolean showplot) {
        double[] params = GaussianFit(xi, yi, showplot);
        return params[2];
    }

    public static double peakpos(double[] xi, double[] yi) {
        return peakpos(xi, yi, true);
    }

    public static int[] findPeaks(double[] yi) {
        double[] a = Tools.getMinMax(yi);
        double tolerance = 0.1 * a[1];
        boolean includeEnds = false;
        int[] peakpos = MaximumFinder.findMaxima(yi, tolerance, includeEnds);
        Arrays.sort(peakpos);
        //PlotProfile(yi);
        return peakpos;
    }

    public static void PlotProfile(double[] y) {
        double[] x = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            x[i] = i;
        }
        Plot plot = new Plot("Profile", "X", "Y", x, y);
        plot.setColor(Color.RED);
        plot.show();
    }

}
