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
import utils.*;

/**
 *
 * @author alex
 */
public class IntResol_Linearity implements PlugInFilter {

    private ImagePlus imp;
    private Roi roi;
    private String Method;

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
        roi = imp.getRoi();
        if (roi == null) {
            IJ.error("Rectangular selection required");
            return DONE;
        }
        this.Method = arg;
        this.imp = imp;
        return DOES_ALL;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {

        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        double pixelsize = 0;
        int nbins = 0;
        String AxisRes = "";
        String AxisLin = "";
        double[][] counts = null;
        float[][] pixels = ip.getFloatArray();

        if (Method.contains("Horizontal")) {
            pixelsize = vh;
            AxisLin = "X";
            AxisRes = "Y";
            nbins = (int) Math.floor(roi.getFloatWidth() * vw / 30);

            counts = new double[nbins][(int) roi.getFloatHeight()];
            double dpos = roi.getFloatWidth() / (nbins - 1);

            for (int j = 0; j < roi.getFloatHeight(); j++) {
                for (int i = 0; i < nbins; i++) {
                    for (int k = 0; k < dpos; k++) {
                        counts[i][j] += pixels[i * k + (int) roi.getXBase()][j + (int) roi.getYBase()];
                    }
                    counts[i][j] /= (int) dpos;
                }
            }
        }
        if (Method.contains("Vertical")) {
            pixelsize = vw;
            AxisLin = "Y";
            AxisRes = "X";
            nbins = (int) Math.floor(roi.getFloatHeight() * vh / 30);

            counts = new double[nbins][(int) roi.getFloatWidth()];
            double dpos = roi.getFloatHeight() / (nbins - 1);

            for (int i = 0; i < roi.getFloatWidth(); i++) {
                for (int j = 0; j < nbins; j++) {
                    for (int k = 0; k < dpos; k++) {
                        counts[j][i] += pixels[i + (int) roi.getXBase()][j * k + (int) roi.getYBase()];
                    }
                    counts[j][i] /= (int) dpos;
                }
            }
        }

        double resol = 0;
        double meanresol = 0;
        int countpeaks = 0;
        double[][] peakpositions = new double[nbins][];
        double[][] x = new double[nbins][];
        int npeaks = 0;
        for (int i = 0; i < nbins; i++) {
            int[] peakpos = Fitter.findPeaks(counts[i]);
            if (peakpos.length > npeaks) {
                npeaks = peakpos.length;
            }
            peakpositions[i] = new double[peakpos.length];
            x[i] = new double[peakpos.length];
            int med = 0;
            for (int j = 0; j < peakpos.length - 1; j++) {
                int med1 = (int) (0.5 * (peakpos[j] + peakpos[j + 1]));
                double[] arr1 = new double[med1 - med];
                double[] x1 = new double[med1 - med];
                for (int k = 0; k < med1 - med; k++) {
                    arr1[k] = counts[i][k + med];
                    x1[k] = k + med;
                }
                peakpositions[i][j] = Fitter.peakpos(x1, arr1, false) * pixelsize;
                med = med1;
                x[i][j] = j;
                double tresol = Fitter.resolution(x1, arr1, pixelsize, false);
                resol = Math.max(resol, tresol);
                meanresol += tresol;
                countpeaks += 1;
            }
            double[] arr = new double[counts[i].length - med];
            double[] xf = new double[counts[i].length - med];
            for (int k = 0; k < counts[i].length - med; k++) {
                arr[k] = counts[i][k + med];
                xf[k] = k + med;
            }
            peakpositions[i][peakpos.length - 1] = Fitter.peakpos(xf, arr, false) * pixelsize;
            x[i][peakpos.length - 1] = peakpos.length - 1;
            double tresol = Fitter.resolution(xf, arr, pixelsize, false);
            resol = Math.max(resol, tresol);
            meanresol += tresol;
            countpeaks += 1;
        }

        double[] residuals = new double[npeaks * nbins];
        for (int j = 0; j < npeaks; j++) {
            double[] newx = new double[nbins];
            double[] newpos = new double[nbins];
            for (int i = 0; i < nbins; i++) {
                newx[i] = i;
                newpos[i] = peakpositions[i][j];
            }
            double[] tresiduals = Fitter.getResidualsinLinearFit(newx, newpos);
            System.arraycopy(tresiduals, 0, residuals, j * nbins, nbins);
        }
        double[] a = Tools.getMinMax(residuals);
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Test", "Number of bins: ");
        rt.addValue("Value", nbins);
        rt.incrementCounter();
        rt.addValue("Test", "Worst Intrinsic Resolution in " + AxisRes + "(mm): ");
        rt.addValue("Value", IJ.d2s(resol, 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Mean Intrinsic Resolution in " + AxisRes + "(mm): ");
        rt.addValue("Value", IJ.d2s(meanresol / countpeaks, 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Absolute Linearity in " + AxisLin + "(mm): ");
        rt.addValue("Value", IJ.d2s(a[1], 4, 9));
        rt.incrementCounter();
        rt.addValue("Test", "Differential Linearity in " + AxisLin + "(mm): ");
        rt.addValue("Value", IJ.d2s(Math.sqrt(MathUtils.sqrsum(residuals) / (residuals.length * (residuals.length - 1))), 4, 9));
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