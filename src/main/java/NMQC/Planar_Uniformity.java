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
import ij.plugin.*;
import ij.plugin.filter.*;
import utils.FPoint2D;
import java.awt.Color;
import utils.*;

/**
 *
 * @author alex
 */
public class Planar_Uniformity implements PlugInFilter {

    private ImagePlus imp;
    private FPoint2D minvalue;
    private FPoint2D maxvalue;

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
        this.minvalue = new FPoint2D(0, 0);
        this.maxvalue = new FPoint2D(0, 0);
        return DOES_ALL;
    }

    public class outputvalues {

        double IU;
        double DU;
        double min;
        double max;
        double mean;

        public outputvalues() {
            IU = 0;
            DU = 0;
            min = 0;
            max = 0;
            mean = 0;
        }
    }

    private outputvalues getUniformity(ImagePlus imp, Roi sFOV, int shrinkfactor) {
        Binner bin = new Binner();
        ImageProcessor ip2 = bin.shrink(imp.getProcessor(), shrinkfactor, shrinkfactor, Binner.SUM);
        ImagePlus imp2 = new ImagePlus("Convolved " + sFOV, ip2);
        double scale = 1.0 / shrinkfactor;
        Roi lFOV = RoiScaler.scale(sFOV, scale, scale, false);
        lFOV = RoiEnlarger.enlarge(lFOV, -1);//To avoid boundaries
        lFOV.setStrokeColor(Color.yellow);
        outputvalues result = new outputvalues();
        imp2.setRoi(lFOV);
        ImageStatistics is = imp2.getStatistics();
        result.min = is.min;
        result.max = is.max;
        result.mean = is.mean;
        imp2.deleteRoi();
        Overlay list = new Overlay();
        list.add(lFOV);
        float[] kernel = {1, 2, 1, 2, 4, 2, 1, 2, 1};
        Convolver cv = new Convolver();
        cv.setNormalize(true);
        cv.convolve(ip2, kernel, 3, 3);
        imp2.updateAndDraw();
        ImageStatistics is0 = ip2.getStatistics();

        float[][] pixels = ip2.getFloatArray();
        int w = ip2.getWidth();
        int h = ip2.getHeight();

        double globalmin = is0.max;
        double globalmax = is0.min;
        FPoint2D PBase = new FPoint2D(sFOV.getBounds().x / shrinkfactor, sFOV.getBounds().y / shrinkfactor);

        for (int j = (int) Math.round(PBase.getY()); j < lFOV.getFloatHeight() + PBase.getY(); j++) {
            for (int i = (int) Math.round(PBase.getX()); i < lFOV.getFloatWidth() + PBase.getX(); i++) {
                if (lFOV.contains(i, j)) {
                    if (pixels[i][j] < globalmin) {
                        globalmin = pixels[i][j];
                        minvalue.assign(i, j);
                    }
                    if (pixels[i][j] > globalmax) {
                        globalmax = pixels[i][j];
                        maxvalue.assign(i, j);
                    }
                    // By rows
                    float localmin = pixels[i][j];
                    float localmax = pixels[i][j];
                    for (int k = -2; k <= 2; k++) {
                        int x = Math.max(0, Math.min(w - 1, i + k));
                        if (lFOV.contains(x, j)) {
                            if (pixels[x][j] < localmin) {
                                localmin = pixels[x][j];
                            }
                            if (pixels[x][j] > localmax) {
                                localmax = pixels[x][j];
                            }
                        }
                    }
                    result.DU = Math.max(result.DU, MathUtils.Contrast(localmin, localmax));
                    // By columns
                    localmin = pixels[i][j];
                    localmax = pixels[i][j];
                    for (int l = -2; l <= 2; l++) {
                        int y = Math.max(0, Math.min(h - 1, j + l));
                        if (lFOV.contains(i, y)) {
                            if (pixels[i][y] < localmin) {
                                localmin = pixels[i][y];
                            }
                            if (pixels[i][y] > localmax) {
                                localmax = pixels[i][y];
                            }
                        }
                    }
                    result.DU = Math.max(result.DU, MathUtils.Contrast(localmin, localmax));
                }
            }
        }

        result.IU = MathUtils.Contrast(globalmin, globalmax);

        PointRoi minPointRoi = new PointRoi(minvalue.getX(), minvalue.getY());
        minPointRoi.setStrokeColor(Color.blue);
        list.add(minPointRoi);
        PointRoi maxPointRoi = new PointRoi(maxvalue.getX(), maxvalue.getY());
        maxPointRoi.setStrokeColor(Color.red);
        list.add(maxPointRoi);

        ip2.setMinAndMax(globalmin, globalmax);
        imp2.setOverlay(list);
        imp2.show();
        //RM.runCommand(imp2, "Show All");

        return result;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
        ResultsTable rt = new ResultsTable();
        Overlay list = new Overlay();

        Roi FOV;
        int shrinkfactor = Math.max(1, (int) Math.round(imp.getHeight() / 64));
        double gmax = imp.getStatistics().max;

        FOV = Commons.getThreshold(imp, gmax * 0.1, 0.95);
        FOV.setStrokeColor(Color.yellow);
        list.add(FOV);
        outputvalues UFOV = getUniformity(imp, FOV, shrinkfactor);
        FOV = Commons.getThreshold(imp, gmax * 0.1, 0.75);
        FOV.setStrokeColor(Color.red);
        list.add(FOV);
        outputvalues CFOV = getUniformity(imp, FOV, shrinkfactor);

        rt.incrementCounter();
        rt.addValue("Test", "Differential Uniformity (%)");
        rt.addValue("UFOV", UFOV.DU);
        rt.addValue("CFOV", CFOV.DU);
        rt.incrementCounter();
        rt.addValue("Test", "Integral Uniformity (%)");
        rt.addValue("UFOV", UFOV.IU);
        rt.addValue("CFOV", CFOV.IU);
        rt.incrementCounter();
        rt.addValue("Test", "Average Pixel Value");
        rt.addValue("UFOV", UFOV.mean);
        rt.addValue("CFOV", CFOV.mean);
        rt.incrementCounter();
        rt.addValue("Test", "Maximum Pixel Value");
        rt.addValue("UFOV", UFOV.max);
        rt.addValue("CFOV", CFOV.max);
        rt.incrementCounter();
        rt.addValue("Test", "Minimum Pixel Value");
        rt.addValue("UFOV", UFOV.min);
        rt.addValue("CFOV", CFOV.min);

        rt.showRowNumbers(true);
        rt.show("Planar Uniformity: " + imp.getTitle());

        imp.setOverlay(list);
    }

    void showAbout() {
        IJ.showMessage("About Planar Uniformity...",
                "Este plugin es para hallar la uniformidad planar de imágenes planas.\n"
                + "This plugin finds the planar uniformity in planar images");
    }

}
