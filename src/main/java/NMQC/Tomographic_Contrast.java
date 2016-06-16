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
        ResultsTable rt = new ResultsTable();
        //rt.incrementCounter();
        //rt.addValue("ROI", "UFOV");

        int ns = imp.getStackSize();
        ImageStack stack = imp.getImageStack();
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
        
        // Finding the mean 
        ImageProcessor ip1 = stack.getProcessor(sinit).duplicate();
        ImagePlus imp2 = new ImagePlus("Uniformity image", ip1);
        ImageStatistics is2 = imp2.getStatistics();
        Roi FOV = Commons.getThreshold(imp2, 0.1 * is2.max, 0.9); // 10% of max value for threshold
        FOV.setStrokeColor(Color.blue);
        imp2.setRoi(FOV);
        is2 = imp2.getStatistics();
        double unif = is2.mean;

        // Building a temporary matrix to find the peaks
        ip1 = stack.getProcessor(send);
        float[][] pixels = ip1.getFloatArray();
        float[][] ip2mat = new float[ip1.getWidth()][ip1.getHeight()];
        for (int i = 0; i < ip1.getWidth(); i++) {
            for (int j = 0; j < ip1.getHeight(); j++) {
                // If spheres are cold put them hot, if they are hot keep them as hot
                ip2mat[i][j] += coldsph ? unif - pixels[i][j] : pixels[i][j] - unif;
            }
        }

        FloatProcessor ip2 = new FloatProcessor(ip2mat);
        imp2 = new ImagePlus("Spheres image", ip2);
        imp2.setRoi(FOV);

        // Finding all the peaks
        MaximumFinder mf = new MaximumFinder();
        Polygon maxs = mf.getMaxima(ip2, unif, true);
        Overlay list = new Overlay();
        list.add(FOV);
        TextRoi.setFont(Font.SERIF, 5, Font.PLAIN, true);
        TextRoi.setGlobalJustification(TextRoi.CENTER);
        //TextRoi.setColor(Color.red);
        for (int i = 0; i < maxs.npoints; i++) {
            double contrast = MathUtils.Contrast(unif, ip2.getPixelValue(maxs.xpoints[i], maxs.ypoints[i]));
            // Exclude all peaks below 50% contrast
            if (contrast > 50) {
                PointRoi tpoint = new PointRoi(maxs.xpoints[i], maxs.ypoints[i]);
                tpoint.setFillColor(Color.yellow);
                list.add(tpoint, "Sphere " + (i + 1));
                TextRoi text = new TextRoi(maxs.xpoints[i], maxs.ypoints[i], "" + (i + 1));
                text.setStrokeColor(Color.red);
                list.add(text);
                rt.incrementCounter();
                rt.addValue("Sphere", i + 1);
                rt.addValue("x", maxs.xpoints[i]);
                rt.addValue("y", maxs.ypoints[i]);
                rt.addValue("Contrast", contrast);
            }
        }
        list.drawNames(true);
        ImagePlus imp1 = new ImagePlus(imp.getTitle() + ":Frame " + send, ip1.duplicate());
        imp1.setOverlay(list);
        imp1.show();

        rt.showRowNumbers(false);
        rt.show("Tomographic Contrast " + imp.getTitle() + ": Frame " + send);

    }

    void showAbout() {
        IJ.showMessage("About Tomographic Contrast...",
                "Este plugin es para hallar el contraste tomográfico en reconstrucciones 3D.\n"
                + "This plugin finds the tomographic contrast in 3D reconstructions");
    }

}
