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
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import utils.*;

/**
 *
 * @author alex
 */
public class SSR_PxSz implements PlugInFilter {

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
            return ROI_REQUIRED;
        }
        this.Method = arg;
        this.imp = imp;
        return DOES_ALL;
    }

    /**
     * The output values for Calculate
     */
    public static class outputvalues {

        FPoint2D resolution;
        double PixelSize;
        double HeaderPixelSize;

        public outputvalues() {
            resolution = new FPoint2D(0, 0);
            PixelSize = 0;
            HeaderPixelSize = 0;
        }
    }

    /**
     *
     * @param imp1 The image
     * @param roi The Roi
     * @param Method The position of the bars
     * @param realdistance The real distance between the two bars
     * @return
     */
    public static outputvalues Calculate(ImagePlus imp1, Roi roi, String Method, double realdistance) {
        outputvalues result = new outputvalues();
        Calibration cal = imp1.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        int size = 0;
        double[] suma = null;
        float[][] pixels = imp1.getProcessor().getFloatArray();

        if (Method.contains("Vertical")) {
            result.HeaderPixelSize = vw;
            size = (int) (roi.getFloatWidth());
            suma = new double[size];

            int init, fin, width;
            if (roi.getFloatHeight() > Commons.NEMAWIDTH) {
                init = (int) Math.floor((roi.getFloatHeight() - Commons.NEMAWIDTH) / 2);
                fin = init + Commons.NEMAWIDTH;
                width = Commons.NEMAWIDTH;
            } else {
                init = 0;
                fin = (int) Math.floor(roi.getFloatHeight());
                width = fin;
            }

            for (int j = init; j <= fin; j++) {
                for (int i = 0; i < roi.getFloatWidth(); i++) {
                    suma[i] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
                }
            }

            for (int i = 0; i < roi.getFloatWidth(); i++) {
                suma[i] = suma[i] / width;
            }
        }
        if (Method.contains("Horizontal")) {
            result.HeaderPixelSize = vh;
            size = (int) (roi.getFloatHeight());
            suma = new double[size];

            int init, fin, width;
            if (roi.getFloatWidth() > Commons.NEMAWIDTH) {
                init = (int) Math.floor((roi.getFloatWidth() - Commons.NEMAWIDTH) / 2);
                fin = init + Commons.NEMAWIDTH;
                width = Commons.NEMAWIDTH;
            } else {
                init = 0;
                fin = (int) Math.floor(roi.getFloatWidth());
                width = fin;
            }

            for (int j = 0; j < roi.getFloatHeight(); j++) {
                for (int i = init; i <= fin; i++) {
                    suma[j] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
                }
            }

            for (int i = 0; i < roi.getFloatHeight(); i++) {
                suma[i] = suma[i] / width;
            }
        }

        int med = Fitter.findMiddlePointinTwoPeaks(suma);
        double[] arr1 = new double[med];
        double[] x1 = new double[med];
        double[] arr2 = new double[size - med + 1];
        double[] x2 = new double[size - med + 1];
        for (int i = 0; i < med; i++) {
            arr1[i] = suma[i];
            x1[i] = i;
        }
        for (int i = med; i < size; i++) {
            arr2[i - med] = suma[i];
            x2[i - med] = i;
        }

        double c1 = Fitter.peakpos(x1, arr1, false);
        double c2 = Fitter.peakpos(x2, arr2, false);

        double c = c2 - c1;

        result.PixelSize = realdistance / c;

        result.resolution.assign(Fitter.resolution(x1, arr1, result.PixelSize, false), Fitter.resolution(x2, arr2, result.PixelSize, false));

        return result;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {

        String Axis = Method.contains("Vertical") ? "X" : "Y";
        GenericDialog gd = new GenericDialog("Pixel Size in " + Axis);
        gd.addNumericField("Enter distance between sources (cm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double d = gd.getNextNumber() * 10;
        outputvalues res = Calculate(imp, roi, Method, d);
        ResultsTable rt1 = new ResultsTable();
        rt1.incrementCounter();
        rt1.addValue("Real Pixel size(mm/px)", IJ.d2s(res.PixelSize, 4, 9));
        rt1.addValue("Header Pixel size(mm/px)", IJ.d2s(res.HeaderPixelSize, 4, 9));
        rt1.addValue("Difference(%)", IJ.d2s((res.PixelSize - res.HeaderPixelSize) * 100 / res.PixelSize, 4, 9));
        rt1.showRowNumbers(true);
        rt1.show("Pixel size in " + Axis + ": " + imp.getTitle());

        ResultsTable rt2 = new ResultsTable();
        rt2.incrementCounter();
        rt2.addValue("Res1(mm)", res.resolution.getX());
        rt2.addValue("Res2(mm)", res.resolution.getY());
        rt2.showRowNumbers(true);
        rt2.show("Spatial resolution in " + Axis + ": " + imp.getTitle());
    }

    void showAbout() {
        IJ.showMessage("About Spatial Resolution and Pixel Size...",
                "Este plugin determina la resolucion espacial y el tamaño del pixel de una imagen de dos barras.\n\n"
                + "This plugin calculates the System Spatial Resolution and the Pixel Size in a two bar image.");
    }
}
