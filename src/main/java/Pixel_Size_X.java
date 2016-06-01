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
public class Pixel_Size_X implements PlugInFilter {

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
        double[] suma = new double[(int) roi.getFloatWidth()];

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
                suma[i] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
            }
        }

        for (int i = 0; i < roi.getFloatWidth(); i++) {
            suma[i] = suma[i] / width;
        }

        int med = Constants.findMiddlePointinTwoPeaks(suma);
        double[] arr1 = new double[med];
        double[] x1 = new double[med];
        double[] arr2 = new double[(int) roi.getFloatWidth() - med + 1];
        double[] x2 = new double[(int) roi.getFloatWidth() - med + 1];
        for (int i = 0; i < med; i++) {
            arr1[i] = suma[i];
            x1[i] = i;
        }
        for (int i = med; i < roi.getFloatWidth(); i++) {
            arr2[i - med] = suma[i];
            x2[i - med] = i;
        }
        double c1 = Fitter.peakpos(x1, arr1, false);
        double c2 = Fitter.peakpos(x2, arr2, false);

        double c = c2 - c1;

        GenericDialog gd = new GenericDialog("Pixel Size in X");
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
        rt.addValue("Real Pixel size(mm/px)", IJ.d2s(size, 4, 9));
        rt.addValue("Header Pixel size(mm/px)", IJ.d2s(vw, 4, 9));
        rt.addValue("Difference(%)", IJ.d2s((size-vw)*100/size, 4, 9));
        rt.showRowNumbers(true);
        rt.show("Pixel size in X");
    }

    void showAbout() {
        IJ.showMessage(" About Pixel Size in X...",
                "Este plugin es para hallar el tamano del pixel en X.");
    }
}
