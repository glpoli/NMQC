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
public class Bar_Quadrant implements PlugInFilter {

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

        GenericDialog gd = new GenericDialog("Bar Quadrant Phantom");
        gd.addNumericField("Enter bar width (mm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double wd = gd.getNextNumber();

        ImageStatistics is0 = imp.getStatistics();
        double mean = is0.mean;
        double stddev = is0.stdDev;

        double MTF = Math.sqrt(2 * (stddev * stddev - mean)) / mean;
        double FWHM = wd * Math.sqrt((16 * Math.log(2) / (Math.PI * Math.PI)) * Math.log(1 / MTF));

        ResultsTable rt = ResultsTable.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
        }
        rt.incrementCounter();
        rt.addValue("MTF", IJ.d2s(MTF, 5, 9));
        rt.addValue("FWHM (mm)", IJ.d2s(FWHM, 5, 9));
        rt.showRowNumbers(true);
        rt.show("Quadrant bar phantom: " + imp.getTitle());
    }

    void showAbout() {
        IJ.showMessage("About Quadrant Bar Phantom...",
                "Este plugin determina la resolucion espacial y el MTF en una adquisición de cuadrante.\n\n"
                + "This plugin calculates the Spatial Resolution and MTF in a quadrant bar phantom.");
    }

}
