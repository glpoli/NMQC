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
import NMQC.utils.*;

/**
 *
 * @author alex.vergara
 */
public class Four_Bar_Phantom implements PlugInFilter {

    private ImagePlus imp;
    private Roi roi;

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
            IJ.error("Selection required");
            return DONE;
        }
        this.imp = imp;
        return DOES_ALL;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {

        //int w = ip.getWidth();
        //int h = ip.getHeight();
        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        float[][] pixels = ip.getFloatArray();
        double[] sumaX = new double[(int) (roi.getFloatWidth())];
        double[] sumaY = new double[(int) (roi.getFloatHeight())];

        int init, fin, width;
        if (roi.getFloatHeight() > Constants.NEMAWIDTH) {
            init = (int) Math.floor((roi.getFloatHeight() - Constants.NEMAWIDTH) / 2);
            fin = init + Constants.NEMAWIDTH;
            width = Constants.NEMAWIDTH;
        } else {
            init = 0;
            fin = (int) Math.floor(roi.getFloatHeight());
            width = fin;
        }

        for (int j = init; j <= fin; j++) {
            for (int i = 0; i < roi.getFloatWidth(); i++) {
                sumaX[i] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
            }
        }

        for (int i = 0; i < roi.getFloatWidth(); i++) {
            sumaX[i] = sumaX[i] / width;
        }

        if (roi.getFloatWidth() > Constants.NEMAWIDTH) {
            init = (int) Math.floor((roi.getFloatWidth() - Constants.NEMAWIDTH) / 2);
            fin = init + Constants.NEMAWIDTH;
            width = Constants.NEMAWIDTH;
        } else {
            init = 0;
            fin = (int) Math.floor(roi.getFloatWidth());
            width = fin;
        }

        for (int j = 0; j < roi.getFloatHeight(); j++) {
            for (int i = init; i <= fin; i++) {
                sumaY[j] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
            }
        }

        for (int i = 0; i < roi.getFloatHeight(); i++) {
            sumaY[i] = sumaY[i] / width;
        }

        int medX = Constants.findMiddlePointinTwoPeaks(sumaX);
        double[] arrX1 = new double[medX];
        double[] x1 = new double[medX];
        double[] arrX2 = new double[(int) roi.getFloatWidth() - medX + 1];
        double[] x2 = new double[(int) roi.getFloatWidth() - medX + 1];
        for (int i = 0; i < medX; i++) {
            arrX1[i] = sumaX[i];
            x1[i] = i;
        }
        for (int i = medX; i < roi.getFloatWidth(); i++) {
            arrX2[i - medX] = sumaX[i];
            x2[i - medX] = i;
        }

        double resX1 = Fitter.resolution(x1, arrX1, vw, false);
        double resX2 = Fitter.resolution(x2, arrX2, vw, false);
        double resX = (resX1 + resX2) / 2;

        double c1 = Fitter.peakpos(x1, arrX1, false);
        double c2 = Fitter.peakpos(x2, arrX2, false);

        double cX = c2 - c1;

        int medY = Constants.findMiddlePointinTwoPeaks(sumaY);
        double[] arrY1 = new double[medY];
        double[] y1 = new double[medY];
        double[] arrY2 = new double[(int) roi.getFloatHeight() - medY + 1];
        double[] y2 = new double[(int) roi.getFloatHeight() - medY + 1];
        for (int i = 0; i < medY; i++) {
            arrY1[i] = sumaY[i];
            y1[i] = i;
        }
        for (int i = medY; i < roi.getFloatHeight(); i++) {
            arrY2[i - medY] = sumaY[i];
            y2[i - medY] = i;
        }

        double resY1 = Fitter.resolution(y1, arrY1, vh, false);
        double resY2 = Fitter.resolution(y2, arrY2, vh, false);
        double resY = (resY1 + resY2) / 2;

        c1 = Fitter.peakpos(y1, arrY1, false);
        c2 = Fitter.peakpos(y2, arrY2, false);

        double cY = c2 - c1;

        GenericDialog gd = new GenericDialog("Pixel Size in X");
        gd.addNumericField("Enter distance between sources (cm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double d = gd.getNextNumber() * 10;
        double sizeX = d / cX;
        double sizeY = d / cY;

        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Test", "Spatial Resolution");
        rt.addValue("X (mm)", resX);
        rt.addValue("Y (mm)", resY);
        rt.addValue("Difference (%)",(1-resX/resY)*100);
        rt.incrementCounter();
        rt.addValue("Test", "Pixel Size");
        rt.addValue("X (mm)", sizeX);
        rt.addValue("Y (mm)", sizeY);
        rt.addValue("Difference (%)",(1-sizeX/sizeY)*100);
        rt.showRowNumbers(true);
        rt.show("Four bar phantom: "+imp.getTitle());
    }

    void showAbout() {
        IJ.showMessage("About Four Bar Phantom...",
                "Este plugin determina la resolucion espacial y el tamaño de pixel en ambos ejes de la imagen.\n\n"
                + "This plugin calculates the Spatial Resolution and Pixel Sizes in both axis of the image.");
    }

}
