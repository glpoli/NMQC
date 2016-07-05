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
import java.awt.Color;
import utils.*;

/**
 *
 * @author alex.vergara
 */
public class Four_Bar_Phantom implements PlugInFilter {

    private ImagePlus imp;

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
        this.imp = imp;
        return DOES_ALL;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {

        Roi roi = Commons.getThreshold(imp, imp.getStatistics().max * 0.1, 0.95);
        roi = RoiEnlarger.enlarge(roi, roi.getFloatWidth() / 4);
        roi = new Roi(roi.getBounds());
        roi.setStrokeColor(Color.yellow);
        imp.setRoi(roi);
        GenericDialog gd = new GenericDialog("Four Bars Phantom");
        gd.addNumericField("Enter distance between sources (cm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double d = gd.getNextNumber() * 10;
        SSR_PxSz.outputvalues resv = SSR_PxSz.Calculate(imp, roi, "Vertical", d);
        SSR_PxSz.outputvalues resh = SSR_PxSz.Calculate(imp, roi, "Horizontal", d);

        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Test", "FWHM");
        rt.addValue("X (mm)", resv.resolution.getX());
        rt.addValue("Y (mm)", resh.resolution.getX());
        rt.addValue("Difference (%)", (1 - resv.resolution.getX() / resh.resolution.getX()) * 100);
        rt.incrementCounter();
        rt.addValue("Test", "FWTM");
        rt.addValue("X (mm)", resv.resolution.getY());
        rt.addValue("Y (mm)", resh.resolution.getY());
        rt.addValue("Difference (%)", (1 - resv.resolution.getY() / resh.resolution.getY()) * 100);
        rt.incrementCounter();
        rt.addValue("Test", "Pixel Size");
        rt.addValue("X (mm)", resv.PixelSize);
        rt.addValue("Y (mm)", resh.PixelSize);
        rt.addValue("Difference (%)", (1 - resv.PixelSize / resh.PixelSize) * 100);
        rt.showRowNumbers(true);
        rt.show("Four bar phantom: " + imp.getTitle());
        
        FileInfo fi = imp.getOriginalFileInfo();
        Commons.saveRT(rt, fi.directory, fi.fileName);
    }

    void showAbout() {
        IJ.showMessage("About Four Bar Phantom...",
                "Este plugin determina la resolucion espacial y el tamaño de pixel en ambos ejes de la imagen.\n\n"
                + "This plugin calculates the Spatial Resolution and Pixel Sizes in both axis of the image.");
    }

}
