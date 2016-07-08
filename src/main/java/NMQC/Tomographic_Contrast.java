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
import ij.io.FileInfo;
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
    private ImageProcessor ip2;
    private String Method;
    private Roi FOV;
    private Polygon maxs;
    private double unif;
    private double tolerance;
    private int send;

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
        this.Method = arg;
        if (Method.contains("Manual")) {
            if ((imp.getRoi() == null) || !(imp.getRoi() instanceof PointRoi)) {
                IJ.error(Commons.LANGUAGES.getString("POINT_ROI_REQUIRED"));
                return ROI_REQUIRED;
            }
        }
        this.imp = imp;
        return DOES_ALL;
    }

    private Polygon Calculate() {
        int ns = imp.getStackSize();
        int sinit;
        send = imp.getCurrentSlice();
        boolean coldsph = true;
        // Dialog
        if (ns > 1) {
            GenericDialog gd = new GenericDialog(Commons.LANGUAGES.getString("TOMOGRAPHIC_CONTRAST"));
            gd.addNumericField(Commons.LANGUAGES.getString("UNIFORMITY_FRAME"), 1, 0);
            gd.addCheckbox(Commons.LANGUAGES.getString("COLD_SPHERES"), true);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return null;
            }
            sinit = (int) Math.round(gd.getNextNumber());
            coldsph = gd.getNextBoolean();
        } else {
            sinit = 1;
        }

        getThresholdValues(sinit);

        // Building a temporary matrix to find the peaks
        ip2 = imp.getImageStack().getProcessor(send);
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
        return mf.getMaxima(ipt, tolerance, true);
    }

    private void getThresholdValues(int sinit) {
        // Finding the mean and the tolerance 
        ImageProcessor ip1 = imp.getImageStack().getProcessor(sinit).duplicate();
        ImagePlus imp1 = new ImagePlus(Commons.LANGUAGES.getString("UNIFORMITY_IMAGE"), ip1);
        ImageStatistics is1 = imp1.getStatistics();
        FOV = Commons.getThreshold(imp1, 0.1 * is1.max, 0.9); // 10% of max value for threshold
        FOV.setStrokeColor(Color.blue);
        imp1.setRoi(FOV);
        is1 = imp1.getStatistics();
        unif = is1.mean;
        tolerance = is1.stdDev * 2;
    }

    private boolean MethodAutomatic() {
        maxs = Calculate();
        return maxs != null;
    }

    private double PointDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
    }

    private boolean MethodManual() {

        Polygon tmaxs = Calculate();
        ip2 = imp.getImageStack().getProcessor(send);
        Polygon proi = imp.getRoi().getPolygon();
        int npoints = proi.npoints;
        double maxr = FOV.getFeretsDiameter() / npoints;
        maxs = new Polygon();
        for (int i = 0; i < npoints; i++) {
            for (int j = 0; j < tmaxs.npoints; j++) {
                if (PointDistance(proi.xpoints[i], proi.ypoints[i], tmaxs.xpoints[j], tmaxs.ypoints[j]) < maxr) {
                    maxs.addPoint(tmaxs.xpoints[j], tmaxs.ypoints[j]);
                    break;
                }
            }
        }
        return maxs.npoints > 0;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
        // Variables
        ResultsTable rt = new ResultsTable();
        if (Method.contains("Automatic")) {
            if (!MethodAutomatic()) {
                return;
            }
        } else if (Method.contains("Manual")) {
            if (!MethodManual()) {
                return;
            }
        } else {
            return;
        }
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
            list.add(tpoint, Commons.LANGUAGES.getString("SPHERE") + (i + 1));
            TextRoi text = new TextRoi(maxs.xpoints[i], maxs.ypoints[i], "" + (i + 1));
            text.setStrokeColor(Color.red);
            list.add(text);
            // Creating the results table
            rt.incrementCounter();
            rt.addValue(Commons.LANGUAGES.getString("SPHERE"), i + 1);
            rt.addValue("x", maxs.xpoints[i]);
            rt.addValue("y", maxs.ypoints[i]);
            rt.addValue(Commons.LANGUAGES.getString("VALUE"), ip2.getPixelValue(maxs.xpoints[i], maxs.ypoints[i]));
            rt.addValue(Commons.LANGUAGES.getString("MEAN"), unif);
            rt.addValue(Commons.LANGUAGES.getString("CONTRAST"), contrast);
        }
        list.drawNames(true);
        String lname = imp.getTitle() + Commons.LANGUAGES.getString("FRAME") + send + "-" + Method;
        ImagePlus imp2 = new ImagePlus(lname, ip2.duplicate());

        imp2.setOverlay(list);

        imp2.show();

        rt.showRowNumbers(
                false);
        rt.show(Commons.LANGUAGES.getString("TOMOGRAPHIC_CONTRAST") + lname);

        FileInfo fi = imp.getOriginalFileInfo();

        Commons.saveRT(rt, fi.directory, lname);

    }

    void showAbout() {
        IJ.showMessage(Commons.LANGUAGES.getString("ABOUT_TOMOGRAPHIC_CONTRAST"),
                Commons.LANGUAGES.getString("DESCRIPTION_TOMOGRAPHIC_CONTRAST"));
    }

}
