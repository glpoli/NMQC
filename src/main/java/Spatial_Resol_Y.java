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
 * @author alex
 */
public class Spatial_Resol_Y implements PlugInFilter {

    private ImagePlus imp;
    private Roi roi;
    private final int NEMAWIDTH = 5;

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
        //double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        float[][] pixels = ip.getFloatArray();
        double[] suma = new double[(int) (roi.getFloatHeight())];
        
        int init, fin, width;
        if (roi.getFloatWidth() > NEMAWIDTH) {
            init = (int) Math.floor((roi.getFloatWidth() - NEMAWIDTH) / 2);
            fin = init + NEMAWIDTH;
            width = NEMAWIDTH;
        }
        else {
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

        int[] peakpos = Fitter.findPeaks(suma);
        if (peakpos.length < 2) {
            IJ.error("Two bars phantom needed");
            return;
        }
        FPoint2D maximo1 = new FPoint2D(peakpos[0], suma[peakpos[0]]);
        FPoint2D maximo2 = new FPoint2D(maximo1);
        for (int value : peakpos) {
            if (suma[value] > maximo1.Y) {
                maximo2.assign(maximo1);
                maximo1.assign(value, suma[value]);
            } else if (suma[value] > maximo2.Y && suma[value] < maximo1.Y) {
                maximo2.assign(value, suma[value]);
            }
        }

        int med = (int) (0.5 * (maximo1.X + maximo2.X));
        double[] arr1 = new double[med];
        double[] x1 = new double[med];
        double[] arr2 = new double[(int) roi.getFloatHeight() - med + 1];
        double[] x2 = new double[(int) roi.getFloatHeight() - med + 1];
        for (int i = 0; i < med; i++) {
            arr1[i] = suma[i];
            x1[i] = i;
        }
        for (int i = med; i < roi.getFloatHeight(); i++) {
            arr2[i - med] = suma[i];
            x2[i - med] = i;
        }

        double res1 = Fitter.resolution(x1, arr1, vh);
        double res2 = Fitter.resolution(x2, arr2, vh);

        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Res1(mm)", res1);
        rt.addValue("Res2(mm)", res2);
        rt.showRowNumbers(true);
        rt.show("Spatial resolution in Y");
    }

    void showAbout() {
        IJ.showMessage("About Spatial Resolution...",
                "Este plugin determina la resolucion espacial en el eje Y de la imagen.\n\n"
              + "This plugin calculates the Spatial Resolution in the Y-axis of the image.");
    }
}
