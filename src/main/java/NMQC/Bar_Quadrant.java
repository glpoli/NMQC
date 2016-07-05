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
import ij.io.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import java.awt.Color;
import java.awt.Font;
import java.util.*;
import utils.*;

/**
 *
 * @author alex.vergara
 */
public class Bar_Quadrant implements PlugInFilter {

    private ImagePlus imp;
    //private Roi roi;
    private final double[] barwidth = {2.12, 2.54, 3.18, 4.23};

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
        /*roi = imp.getRoi();
        if (roi == null) {
            IJ.error("Selection required");
            return DONE;
        }*/
        this.imp = imp;
        return DOES_ALL;
    }

    private Roi getQuadrant(Roi FOV, int quadrant) {
        double[] middle = FOV.getContourCentroid();
        FloatPolygon fp;
        FloatPolygon poly = new FloatPolygon();
        if (IJ.getVersion().contains("1.51")) {
            fp = FOV.getContainedFloatPoints();
        } else {
            fp = Commons.getContainedFloatPoints(FOV);
        }

        for (int i = 0; i < fp.npoints; i++) {
            double angle = Commons.getFloatAngle(middle[0], middle[1], fp.xpoints[i], fp.ypoints[i]);
            if ((angle >= (quadrant - 1) * 90) && (angle <= quadrant * 90)) {
                poly.addPoint(fp.xpoints[i], fp.ypoints[i]);
            }
        }

        PolygonRoi result = new PolygonRoi(poly, Roi.POLYGON);
        return new PolygonRoi(result.getConvexHull(), Roi.POLYGON);
    }

    private Roi DetectBars(ImagePlus imp1, Roi FOV, float mean, int quadrant) {
        ImageProcessor ip1 = imp1.getProcessor();
        float[][] Pixels = ip1.getFloatArray();
        float[][] tarray = new float[ip1.getWidth()][ip1.getHeight()];
        double max = 0;
        for (int i = 0; i < ip1.getWidth(); i++) {
            for (int j = 0; j < ip1.getHeight(); j++) {
                if (FOV.contains(i, j)) {
                    tarray[i][j] = mean - Pixels[i][j];
                    if (tarray[i][j] < 0) {
                        tarray[i][j] = 0;
                    }
                    if (tarray[i][j] > max) {
                        max = tarray[i][j];
                    }
                }
            }
        }
        ImageProcessor ip2 = new FloatProcessor(tarray);
        ImagePlus imp2 = new ImagePlus("temporal " + quadrant, ip2);
        Roi result = Commons.getThreshold(imp2, max * 0.5, 1);
        //imp2.setRoi(result);
        //imp2.show();
        return result;
    }

    private double[] getBarWidth(double[] lmtf) {
        SortedMap<Double, Double> map = new TreeMap<>();
        for (int i = 0; i < 4; i++) {
            map.put(lmtf[i], barwidth[i]);
        }
        double[] result = new double[4];
        int i = 0;
        for (Map.Entry<Double, Double> entry : map.entrySet()) {
            result[i] = entry.getValue();
            i++;
        }
        return result;
    }

    private int[] getQuadrantOrder(double[] lmtf) {
        SortedMap<Double, Integer> map = new TreeMap<>();
        for (int i = 0; i < 4; i++) {
            map.put(lmtf[i], i);
        }
        int[] result = new int[4];
        int i = 0;
        for (Map.Entry<Double, Integer> entry : map.entrySet()) {
            result[i] = entry.getValue();
            i++;
        }
        return result;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {

        ImagePlus imp2 = imp.duplicate();
        imp.deleteRoi();
        imp2.deleteRoi();
        Roi FOV = Commons.getThreshold(imp2, imp2.getStatistics().max * 0.1, 0.95);
        imp2.setRoi(FOV);
        double mean = imp2.getStatistics().mean;
        imp2.deleteRoi();

        ResultsTable rt = new ResultsTable();
        Overlay list = new Overlay();
        Roi[] lFOV = new Roi[4];
        double[] lmtf = new double[4];
        TextRoi.setFont(Font.SERIF, 12, Font.PLAIN, true);
        TextRoi.setGlobalJustification(TextRoi.CENTER);
        TextRoi.setColor(Color.yellow);
        for (int i = 0; i < 4; i++) {
            lFOV[i] = getQuadrant(FOV, i + 1);
            lFOV[i] = DetectBars(imp2, lFOV[i], (float) mean, i);
            //lFOV[i] = new Roi(lFOV[i].getBounds());
            lFOV[i].setStrokeColor(Color.yellow);
            list.add(lFOV[i]);
            imp2.setRoi(lFOV[i]);
            ImageStatistics is2 = imp2.getStatistics();
            double lmean = is2.mean;
            double lstddev = is2.stdDev;
            imp2.deleteRoi();
            lmtf[i] = MathUtils.MTF(lmean, lstddev);
        }
        int[] bw = getQuadrantOrder(lmtf);
        Arrays.sort(lmtf);
        for (int i = 0; i < 4; i++) {
            double FWHM = barwidth[i] * Math.sqrt((16 * Math.log(2) / (Math.PI * Math.PI)) * Math.log(1 / lmtf[i]));
            double FWTM = barwidth[i] * Math.sqrt((16 * Math.log(10) / (Math.PI * Math.PI)) * Math.log(1 / lmtf[i]));
            rt.incrementCounter();
            rt.addValue("Quadrant", "" + (i + 1));
            rt.addValue("Barwidth (mm)", "" + barwidth[i]);
            rt.addValue("MTF", IJ.d2s(lmtf[i], 5, 9));
            rt.addValue("FWHM (mm)", IJ.d2s(FWHM, 5, 9));
            rt.addValue("FWTM (mm)", IJ.d2s(FWTM, 5, 9));
            double[] center = lFOV[bw[i]].getContourCentroid();
            TextRoi tr = new TextRoi(center[0], center[1], "" + (i + 1));
            list.add(tr);
        }

        imp.setOverlay(list);
        //imp.show();

        rt.showRowNumbers(false);
        rt.show("Quadrant bar phantom: " + imp.getTitle());

        FileInfo fi = imp.getOriginalFileInfo();
        Commons.saveRT(rt, fi.directory, fi.fileName);
    }

    void showAbout() {
        IJ.showMessage("About Quadrant Bar Phantom...",
                "Este plugin determina la resolucion espacial y el MTF en una adquisición de cuadrante.\n"
                + "This plugin calculates the Spatial Resolution and MTF in a quadrant bar phantom.\n\n"
                + "Hander et al.: Rapid objective measurement of gamma camera resolution.\n"
                + "Medical Physics, Vol. 24, No. 2, February 1997");
    }

}
