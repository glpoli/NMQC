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
import ij.gui.GenericDialog; // Enable this line if you want to enable the dialog
import ij.measure.*;
import ij.plugin.filter.*;
import java.awt.Polygon;
import utils.FPoint2D;
import utils.*;

/**
 *
 * @author alex
 */
public class Tomographic_Contrast implements PlugInFilter {

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
        ResultsTable rt = new ResultsTable();
        //rt.incrementCounter();
        //rt.addValue("ROI", "UFOV");

        int ns = imp.getStackSize();
        int sinit;
        int send;
        boolean coldsph = true;
        if (ns > 1) {
            GenericDialog gd = new GenericDialog("Tomographic Contrast.");
            gd.addNumericField("Entre el corte de uniformidad", 1, 0);
            gd.addNumericField("Entre el corte a procesar", ns, 0);
            gd.addCheckbox("Cold Spheres?", true);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            sinit = (int) Math.round(gd.getNextNumber());
            send = (int) Math.round(gd.getNextNumber());
            coldsph = gd.getNextBoolean();
        } else {
            sinit = 1;
            send = 1;
        }
        float[][] ip2mat = new float[imp.getWidth()][imp.getHeight()];

        imp.setSlice(sinit);
        float[][] pixels = imp.getProcessor().getFloatArray();
        for (int i = 0; i < imp.getWidth(); i++) {
            for (int j = 0; j < imp.getHeight(); j++) {
                ip2mat[i][j] += pixels[i][j];
            }
        }

        FloatProcessor ip2 = new FloatProcessor(ip2mat);
        ImagePlus imp2 = new ImagePlus("Uniformity image", ip2);
        Roi FOV = Constants.getThreshold(imp2, 0.1, 0.9); // 10% of max value for threshold
        double unif = imp2.getStatistics().mean;

        imp.setSlice(send);
        pixels = imp.getProcessor().getFloatArray();
        for (int i = 0; i < imp.getWidth(); i++) {
            for (int j = 0; j < imp.getHeight(); j++) {
                ip2mat[i][j] += coldsph ? unif - pixels[i][j] : pixels[i][j];
            }
        }

        ip2 = new FloatProcessor(ip2mat);
        imp2 = new ImagePlus("Spheres image", ip2);
        imp2.setRoi(FOV);

        MaximumFinder mf = new MaximumFinder();
        Polygon maxs = mf.getMaxima(ip2, unif, true);
        Overlay list = new Overlay();
        list.add(FOV);
        for (int i = 0; i < maxs.npoints; i++) {
            double contrast = MathUtils.Contrast(unif, ip2.getPixelValue(maxs.xpoints[i], maxs.ypoints[i]));
            if (contrast > 50) {
                PointRoi tpoint = new PointRoi(maxs.xpoints[i], maxs.ypoints[i]);
                tpoint.setName("Sphere " + i);
                list.add(tpoint);
                rt.incrementCounter();
                rt.addValue("Sphere", i);
                rt.addValue("x", maxs.xpoints[i]);
                rt.addValue("y", maxs.ypoints[i]);
                rt.addValue("Contrast", contrast);
            }
        }

        ImagePlus ip1 = new ImagePlus(imp.getTitle() + ":Frame " + send, imp.getProcessor().duplicate());
        list.drawNames(true);
        ip1.setOverlay(list);
        ip1.show();

        rt.showRowNumbers(false);
        rt.show("Tomographic Contrast " + imp.getTitle() + ": Frame " + send);

    }

    void showAbout() {
        IJ.showMessage("About Tomographic Contrast...",
                "Este plugin es para hallar el contraste tomográfico en reconstrucciones 3D.\n"
                + "This plugin finds the tomographic contrast in 3D reconstructions");
    }

}
