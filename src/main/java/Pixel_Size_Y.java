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
public class Pixel_Size_Y implements PlugInFilter {

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

        float[][] pixels = ip.getFloatArray();
        double[] suma = new double[(int) roi.getFloatHeight()];

        for (int j = 0; j < roi.getFloatHeight(); j++) {
            for (int i = 0; i < roi.getFloatWidth(); i++) {
                suma[j] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
            }
        }

        for (int i = 0; i < roi.getFloatHeight(); i++) {
            suma[i] = suma[i] / roi.getFloatWidth();
        }

        FPoint2D maximo = new FPoint2D(1, suma[0]);
        FPoint2D maximo2 = new FPoint2D(0, 0);
        boolean foundmax = false;
        for (int i = 2; i < roi.getFloatHeight(); i++) {
            if (suma[i] > maximo.Y) {
                maximo.assign(i, suma[i]);
            }
            if ((suma[i] <= (0.1) * maximo.Y) && !(foundmax)) {
                maximo2.assign(maximo);
                maximo.assign(i, suma[i]);
                foundmax = true;
            }
        }

        int med = (int) (0.5 * (maximo.X + maximo2.X));
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
        double c1 = Fitter.peakpos(x1, arr1, false);
        double c2 = Fitter.peakpos(x2, arr2, false);

        double c = c2 - c1;

        GenericDialog gd = new GenericDialog("Pixel Size in Y");
        gd.addNumericField("Enter distance between sources (cm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double d = gd.getNextNumber()*10;
        double size = d / c;
        
        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        double vd = cal.pixelDepth;

        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Pixel size(mm/px)", IJ.d2s(size, 4, 9));
        rt.addValue("Header Pixel size(mm/px)", IJ.d2s(vh, 4, 9));
        rt.addValue("Difference(%)", IJ.d2s((1-(vh/size))*100, 4, 9));
        rt.showRowNumbers(true);
        rt.show("Pixel size in Y");
    }

    void showAbout() {
        IJ.showMessage("  About Pixel Size in Y...",
                "Este plugin es para hallar el tamano del pixel en Y.");
    }
}
