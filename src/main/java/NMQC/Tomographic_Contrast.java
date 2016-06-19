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
import ij.gui.GenericDialog;
import ij.measure.*;
import ij.plugin.filter.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
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
        // Variables
        ResultsTable rt = new ResultsTable();
        int ns = imp.getStackSize();
        ImageStack stack = imp.getImageStack();
        int sinit;
        int send;
        boolean coldsph = true;
        // Dialog
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

        // Finding the mean and the tolerance 
        ImageProcessor ip1 = stack.getProcessor(sinit).duplicate();
        ImagePlus imp1 = new ImagePlus("Uniformity image", ip1);
        ImageStatistics is1 = imp1.getStatistics();
        Roi FOV = Commons.getThreshold(imp1, 0.1 * is1.max, 0.9); // 10% of max value for threshold
        FOV.setStrokeColor(Color.blue);
        imp1.setRoi(FOV);
        is1 = imp1.getStatistics();
        double unif = is1.mean;
        double tolerance = is1.stdDev * 2;

        // Building a temporary matrix to find the peaks
        ImageProcessor ip2 = stack.getProcessor(send);
        float[][] pixels = ip2.getFloatArray();
        float[][] ip2mat = new float[ip2.getWidth()][ip2.getHeight()];
        for (int i = 0; i < ip2.getWidth(); i++) {
            for (int j = 0; j < ip2.getHeight(); j++) {
                if (FOV.contains(i, j)) {
                    // If spheres are cold put them hot, if they are hot keep them as hot
                    ip2mat[i][j] += coldsph ? unif - pixels[i][j] : pixels[i][j] - unif;
                }
            }
        }
        FloatProcessor ipt = new FloatProcessor(ip2mat);

        // Finding all the peaks
        MaximumFinder mf = new MaximumFinder();
        Polygon maxs = mf.getMaxima(ipt, tolerance, true);
        
        // Final processing
        Overlay list = new Overlay();
        list.add(FOV);
        TextRoi.setFont(Font.SERIF, 5, Font.PLAIN, true);
        TextRoi.setGlobalJustification(TextRoi.CENTER);
        for (int i = 0; i < maxs.npoints; i++) {
            // The contrast is calculated with original matrix using the positions of the calculated maximas
            double contrast = MathUtils.Contrast(unif, ip2.getPixelValue(maxs.xpoints[i], maxs.ypoints[i]));
            // Adding points in the position of the maximas
            PointRoi tpoint = new PointRoi(maxs.xpoints[i], maxs.ypoints[i]);
            tpoint.setFillColor(Color.yellow);
            list.add(tpoint, "Sphere " + (i + 1));
            TextRoi text = new TextRoi(maxs.xpoints[i], maxs.ypoints[i], "" + (i + 1));
            text.setStrokeColor(Color.orange);
            list.add(text);
            // Creating the results table
            rt.incrementCounter();
            rt.addValue("Sphere", i + 1);
            rt.addValue("x", maxs.xpoints[i]);
            rt.addValue("y", maxs.ypoints[i]);
            rt.addValue("value", ip2.getPixelValue(maxs.xpoints[i], maxs.ypoints[i]));
            rt.addValue("mean", unif);
            rt.addValue("Contrast", contrast);
        }
        list.drawNames(true);
        ImagePlus imp2 = new ImagePlus(imp.getTitle() + ":Frame " + send, ip2.duplicate());
        imp2.setOverlay(list);
        imp2.show();

        rt.showRowNumbers(false);
        rt.show("Tomographic Contrast " + imp.getTitle() + ": Frame " + send);

    }

    void showAbout() {
        IJ.showMessage("About Tomographic Contrast...",
                "Este plugin es para hallar el contraste tomográfico en reconstrucciones 3D.\n"
                + "This plugin finds the tomographic contrast in 3D reconstructions");
    }

}
