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
package NMQC;

import ij.*;
import ij.gui.*;
import ij.io.FileInfo;
import ij.process.*;
import ij.measure.*;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.PlugInFilter;
import ij.util.Tools;
import java.awt.Color;
import java.util.*;
import org.apache.commons.math3.stat.StatUtils;
import utils.*;

/**
 *
 * @author alex
 */
public class IntResol_Linearity implements PlugInFilter {

    private ImagePlus imp;
    private String Method;
    final private int NemaSep = 30;//Nema Phantom line separation in mm
    final private int HalfNemaSep = 15;

    /**
     *
     * @param arg Optional to show about
     * @param imp The active image
     * @return
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        if (imp == null) {
            IJ.noImage();
            return DONE;
        }
        this.Method = arg;
        this.imp = imp;
        return DOES_ALL;
    }

    private class myReturnedObjects {

        public double pixelsize;
        public String AxisLin;
        public String AxisRes;
        public int nbins;
        public double[][] counts;

        public myReturnedObjects() {
            this.pixelsize = 0;
            this.AxisLin = "";
            this.AxisRes = "";
            this.nbins = 0;
            this.counts = null;
        }

    }

    private myReturnedObjects getCounts(String Orientation, Roi lroi) {
        myReturnedObjects result = new myReturnedObjects();
        Calibration cal = imp.getCalibration();
        float[][] pixels = imp.getProcessor().getFloatArray();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        if (Orientation.contains("Horizontal")) {
            result.pixelsize = vh;
            result.AxisLin = "X";
            result.AxisRes = "Y";
            result.nbins = (int) Math.round(lroi.getFloatWidth() * vw / NemaSep);

            result.counts = new double[result.nbins][(int) lroi.getFloatHeight()];
            double dpos = lroi.getFloatWidth() / (result.nbins - 1);

            for (int j = 0; j < lroi.getFloatHeight(); j++) {
                for (int i = 0; i < result.nbins; i++) {
                    for (int k = 0; k < dpos; k++) {
                        int xi = (int) Math.round(i * dpos + k + lroi.getXBase());
                        int yi = j + (int) lroi.getYBase();
                        if (lroi.contains(xi, yi)) {
                            result.counts[i][j] += pixels[xi][yi];
                        }
                    }
                    result.counts[i][j] /= (int) dpos;
                }
            }
        }

        if (Orientation.contains("Vertical")) {
            result.pixelsize = vw;
            result.AxisLin = "Y";
            result.AxisRes = "X";
            result.nbins = (int) Math.floor(lroi.getFloatHeight() * vh / NemaSep);

            result.counts = new double[result.nbins][(int) lroi.getFloatWidth()];
            double dpos = lroi.getFloatHeight() / (result.nbins - 1);

            for (int i = 0; i < lroi.getFloatWidth(); i++) {
                int xi = i + (int) lroi.getXBase();
                for (int j = 0; j < result.nbins; j++) {
                    int yo = (int) Math.round(j * dpos + lroi.getYBase());
                    for (int k = 0; k < dpos; k++) {
                        int yi = yo + k;
                        if (lroi.contains(xi, yi)) {
                            result.counts[j][i] += pixels[xi][yi];
                        }
                    }
                    result.counts[j][i] /= (int) dpos;
                }
            }
        }
        return result;
    }

    /**
     * This are the output values from Calculate contains the maximum
     * resolution, the mean resolution, some data and the residuals.
     *
     */
    public class myoutput {

        FPoint2D resol;
        FPoint2D meanresol;
        myReturnedObjects data;
        double stddevresidual;
        double maxresidual;

        public myoutput() {
            this.resol = new FPoint2D(0, 0);
            this.meanresol = new FPoint2D(0, 0);
            this.data = null;
            this.stddevresidual = 0;
            this.maxresidual = 0;
        }
    }

    /**
     *
     * @param list the overlay on which we add the calculated ROIs
     * @param cutoff the cuttoff to calculate the ROIs
     * @param avoidnonrectangular tell me if you want to avoid non rectangular
     * shape effects
     * @return several values
     */
    public myoutput Calculate(Overlay list, double cutoff, boolean avoidnonrectangular) {
        ImageStatistics is = imp.getStatistics();
        myoutput result = new myoutput();
        Roi UFOV = Commons.getThreshold(imp, 0.1 * is.max, cutoff);
        UFOV.setStrokeColor(Color.yellow);

        // We check that the calculated roi doesnt collide with a line strip
        result.data = getCounts(Method, UFOV);
        while (result.data.counts[(int) result.data.nbins / 2][0] > is.max * 0.1) {
            UFOV = RoiEnlarger.enlarge(UFOV, -1); // the roi is colliding with a line strip, so we reduce it 1 pixel
            result.data = getCounts(Method, UFOV); // the roi has been reduced so we calculate the data again
        }
        list.add(UFOV);

        /**
         * Getting the central array as reference for the number of peaks, the
         * peak positions and the peak height
         */
        int npeaks = result.data.counts[(int) result.data.nbins / 2].length;
        double[] central = new double[npeaks];
        System.arraycopy(result.data.counts[(int) result.data.nbins / 2], 0, central, 0, npeaks);
        int[] lpeakpos = Fitter.findPeaks(central);
        double maxreference = MathUtils.averag(central) - 2 * MathUtils.StdDev(central);
        npeaks = lpeakpos.length;
        double[][] peakpositions = new double[result.data.nbins][npeaks];
        double[][] x = new double[result.data.nbins][npeaks];

        int countpeaks = 0;
        for (int i = 0; i < result.data.nbins; i++) {
            IJ.showProgress(i / result.data.nbins / 2);
            int[] peakpos = Fitter.findPeaks(result.data.counts[i]);
            // We avoid non rectangular zones if the option is true
            if (avoidnonrectangular) {
                if (peakpos.length != npeaks) {
                    continue;
                }
            }
            // Drop peaks that are at lower position than the first reference
            while (npeaks < peakpos.length) {
                if (peakpos[0] - lpeakpos[0] < -HalfNemaSep) {
                    peakpos = Arrays.copyOfRange(peakpos, 1, peakpos.length);
                }
            }

            int lnpeaks = peakpos.length;
            /**
             * We split the array by finding the middle between two consecutive
             * points, for the last point we just take the end of the array
             */
            int med = 0;
            for (int j = 0; j < lnpeaks; j++) {
                int med1 = j < lnpeaks - 1 ? (int) (0.5 * (peakpos[j] + peakpos[j + 1])) : result.data.counts[i].length;
                int lsize = med1 - med;
                double[] arr1 = new double[lsize];
                double[] x1 = new double[lsize];
                double tlevel = result.data.counts[i][med];
                for (int k = 0; k < lsize; k++) {
                    arr1[k] = result.data.counts[i][k + med];
                    x1[k] = k + med;
                    tlevel = Math.max(tlevel, arr1[k]);
                }
                double ppos = Fitter.peakpos(x1, arr1, false) * result.data.pixelsize;
                // Check the right position for the peak in the array
                int l = 0;
                while (peakpos[0] - lpeakpos[l] > HalfNemaSep) {
                    l += 1;
                }
                /**
                 * Check if the peak is well conformed: 1. the peak is centered
                 * in the array, borders are lower than 10% of maxima 2. the
                 * peak has enough counts, the maxima is higher than 2 * stddev
                 * below reference level
                 */
                boolean conformed = (arr1[0] < tlevel * 0.1) && (arr1[lsize - 1] < tlevel * 0.1) && (tlevel > maxreference);
                med = med1;
                /**
                 * If conformed then add it to the matrix Only count conformed
                 * peaks
                 */
                if (conformed) {
                    peakpositions[i][j + l] = ppos;
                    x[i][j + l] = j + l;
                    //Find the resolution
                    FPoint2D tresol = Fitter.resolution(x1, arr1, result.data.pixelsize, false);
                    if (tresol.getX() > result.resol.getX()) {
                        result.resol = tresol;
                    }
                    result.meanresol.add(tresol);
                    countpeaks += 1;
                }
            }
        }
        result.meanresol.divide(countpeaks);

        //MathUtils.PrintMatrix(peakpositions);
        // Final step to get residuals in linear fit for Linearity
        for (int j = 0; j < npeaks; j++) {
            IJ.showProgress(0.5 + j / npeaks / 2);
            ArrayList<Double> lnewx = new ArrayList();
            ArrayList<Double> lnewpos = new ArrayList();
            for (int i = 0; i < result.data.nbins; i++) {
                if (peakpositions[i][j] != 0) {
                    lnewx.add(x[i][j]);
                    lnewpos.add(peakpositions[i][j]);
                }
            }
            double[] newx = Commons.toPrimitive(lnewx.toArray(new Double[0]));
            double[] newpos = Commons.toPrimitive(lnewpos.toArray(new Double[0]));
            double[] tresiduals = Fitter.getResidualsinLinearFit(newx, newpos, false);
            double a = StatUtils.max(tresiduals);
            /*double mean = MathUtils.averag(newpos);
            double maxi = Math.abs(newpos[0] - mean);
            for (double it : newpos) {
                maxi = Math.max(maxi, Math.abs(it - mean));
            }*/

            result.maxresidual = Math.max(result.maxresidual, a);
            result.stddevresidual = Math.max(result.stddevresidual, MathUtils.StdDev(newpos));
        }
        IJ.showProgress(1.0);
        return result;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
        Overlay list = new Overlay();
        boolean avoidrect = Method.contains("exclude");
        String forTitle = avoidrect ? " rectangular" : " shaped";
        myoutput r1 = Calculate(list, 0.95, avoidrect);
        myoutput r2 = Calculate(list, 0.75, avoidrect);
        imp.setOverlay(list);
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Test", "Number of bins: ");
        rt.addValue("UFOV", r1.data.nbins);
        rt.addValue("CFOV", r2.data.nbins);
        rt.incrementCounter();
        rt.addValue("Test", "Worst Intrinsic FWHM in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.resol.getX(), 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.resol.getX(), 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Worst Intrinsic FWTM in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.resol.getY(), 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.resol.getY(), 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Mean Intrinsic FWHM in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.meanresol.getX(), 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.meanresol.getX(), 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Mean Intrinsic FWTM in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.meanresol.getY(), 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.meanresol.getY(), 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Absolute Linearity in " + r1.data.AxisLin + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.maxresidual, 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.maxresidual, 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Differential Linearity in " + r1.data.AxisLin + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.stddevresidual, 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.stddevresidual, 4, 9));
        rt.showRowNumbers(false);
        rt.show("Intrinsic Resolution and Linearity: " + imp.getTitle() + forTitle);
        
        FileInfo fi = imp.getOriginalFileInfo();
        Commons.saveRT(rt, fi.directory, fi.fileName + "-" + forTitle);
    }

    void showAbout() {
        IJ.showMessage("About Intrinsic Resolution and Linearity...",
                "Este plugin determina el peor valor y el promedio de los valores de resolucion intrinseca\n"
                + " y determina la linealidad absoluta y diferencial.\n\n"
                + "This plugin determinate the worst value and the mean of the values of intrinsic resolution\n"
                + " and determinate the absolute linearity and differential linearity.\n");
    }
}
