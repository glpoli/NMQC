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
import ij.util.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import java.util.*;
import utils.*;

/**
 *
 * @author alex
 */
public class IntResol_Linearity implements PlugInFilter {

    private ImagePlus imp;
    private String Method;
    final private int NemaSep = 30;//Nema Phantom line separation in mm

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
            result.nbins = (int) Math.floor(lroi.getFloatWidth() * vw / NemaSep);

            result.counts = new double[result.nbins][(int) lroi.getFloatHeight()];
            double dpos = lroi.getFloatWidth() / (result.nbins - 1);

            for (int j = 0; j < lroi.getFloatHeight(); j++) {
                for (int i = 0; i < result.nbins; i++) {
                    for (int k = 0; k < dpos; k++) {
                        int xi = i * k + (int) lroi.getXBase();
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
                for (int j = 0; j < result.nbins; j++) {
                    for (int k = 0; k < dpos; k++) {
                        int xi = i + (int) lroi.getXBase();
                        int yi = j * k + (int) lroi.getYBase();
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

    public class myoutput {

        double resol;
        double meanresol;
        myReturnedObjects data;
        double[] residuals;

        public myoutput() {
            this.resol = 0;
            this.meanresol = 0;
            this.data = null;
            this.residuals = null;
        }
    }

    public myoutput Calculate(Overlay list, double cutoff) {
        ImageStatistics is = imp.getStatistics();
        myoutput result = new myoutput();
        Roi UFOV = Constants.getThreshold(imp, 0.1 * is.max, cutoff);
        list.add(UFOV);

        result.data = getCounts(Method, UFOV);
        int npeaks = result.data.counts[(int) result.data.nbins / 2].length;
        double[] central = new double[npeaks];
        System.arraycopy(result.data.counts[(int) result.data.nbins / 2], 0, central, 0, npeaks);
        int[] lpeakpos = Fitter.findPeaks(central);
        npeaks = lpeakpos.length;
        double[][] peakpositions = new double[result.data.nbins][npeaks];
        double[][] x = new double[result.data.nbins][npeaks];
        /*for (int i = 0; i < result.data.nbins; i++) {
            for (int j = 0; j < npeaks; j++) {
                x[i][j] = j;
                peakpositions[i][j] = lpeakpos[j];
            }
        }*/

        int countpeaks = 0;
        for (int i = 0; i < result.data.nbins; i++) {
            IJ.showProgress(i / result.data.nbins / 2);
            int[] peakpos = Fitter.findPeaks(result.data.counts[i]);
            int xshift = 0;
            while ((peakpos[0] - lpeakpos[xshift]) * result.data.pixelsize > NemaSep / 2) {
                xshift += 1;
                if (xshift > npeaks) {
                    IJ.error("Imágen no válida");
                    return null;
                }
            }
            int med = 0;
            for (int j = 0; j < peakpos.length - 1; j++) {
                int med1 = (int) (0.5 * (peakpos[j] + peakpos[j + 1]));
                double[] arr1 = new double[med1 - med];
                double[] x1 = new double[med1 - med];
                for (int k = 0; k < med1 - med; k++) {
                    arr1[k] = result.data.counts[i][k + med];
                    x1[k] = k + med;
                }
                peakpositions[i][j + xshift] = Fitter.peakpos(x1, arr1, false) * result.data.pixelsize;
                med = med1;
                x[i][j + xshift] = j + xshift;
                double tresol = Fitter.resolution(x1, arr1, result.data.pixelsize, false);
                result.resol = Math.max(result.resol, tresol);
                result.meanresol += tresol;
                countpeaks += 1;
            }
            double[] arr = new double[result.data.counts[i].length - med];
            double[] xf = new double[result.data.counts[i].length - med];
            for (int k = 0; k < result.data.counts[i].length - med; k++) {
                arr[k] = result.data.counts[i][k + med];
                xf[k] = k + med;
            }
            peakpositions[i][peakpos.length - 1] = Fitter.peakpos(xf, arr, false) * result.data.pixelsize;
            x[i][peakpos.length - 1] = peakpos.length - 1;
            double tresol = Fitter.resolution(xf, arr, result.data.pixelsize, false);
            result.resol = Math.max(result.resol, tresol);
            result.meanresol += tresol;
            countpeaks += 1;
        }
        result.meanresol /= countpeaks;

        result.residuals = new double[npeaks * result.data.nbins];
        for (int j = 0; j < npeaks; j++) {
            IJ.showProgress(0.5 + j / npeaks / 2);
            ArrayList<double> lnewx = new ArrayList<double>();
            ArrayList<double> lnewpos = new ArrayList<double>();
            //double[] newx = new double[result.data.nbins];
            //double[] newpos = new double[result.data.nbins];
            for (int i = 0; i < result.data.nbins; i++) {
                if (peakpositions[i][j] != 0) {
                    lnewx.add(i);
                    lnewpos.add(peakpositions[i][j]);
                }
            }
            double[] newx = (double[])lnewx.toArray();
            double[] newpos = (double[])lnewpos.toArray();
            double[] tresiduals = Fitter.getResidualsinLinearFit(newx, newpos, true);
            System.arraycopy(tresiduals, 0, result.residuals, j * result.data.nbins, result.data.nbins);
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
        myoutput r1 = Calculate(list, 0.95);
        double[] a1 = Tools.getMinMax(r1.residuals);
        myoutput r2 = Calculate(list, 0.75);
        double[] a2 = Tools.getMinMax(r2.residuals);
        imp.setOverlay(list);
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Test", "Number of bins: ");
        rt.addValue("UFOV", r1.data.nbins);
        rt.addValue("CFOV", r2.data.nbins);
        rt.incrementCounter();
        rt.addValue("Test", "Worst Intrinsic Resolution in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.resol, 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.resol, 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Mean Intrinsic Resolution in " + r1.data.AxisRes + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(r1.meanresol, 4, 9));
        rt.addValue("CFOV", IJ.d2s(r2.meanresol, 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Absolute Linearity in " + r1.data.AxisLin + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(a1[1], 4, 9));
        rt.addValue("CFOV", IJ.d2s(a2[1], 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Differential Linearity in " + r1.data.AxisLin + "(mm): ");
        rt.addValue("UFOV", IJ.d2s(Math.sqrt(MathUtils.sqrsum(r1.residuals) / (r1.residuals.length * (r1.residuals.length - 1))), 4, 9));
        rt.addValue("CFOV", IJ.d2s(Math.sqrt(MathUtils.sqrsum(r2.residuals) / (r2.residuals.length * (r2.residuals.length - 1))), 4, 9));
        rt.showRowNumbers(false);
        rt.show("Intrinsic Resolution and Linearity: " + imp.getTitle());
    }

    void showAbout() {
        IJ.showMessage("About Intrinsic Resolution and Linearity...",
                "Este plugin determina el peor valor y el promedio de los valores de resolucion intrinseca\n"
                + " y determina la linealidad absoluta y diferencial.\n\n"
                + "This plugin determinate the worst value and the mean of the values of intrinsic resolution\n"
                + " and determinate the absolute linearity and differential linearity.\n");
    }
}
