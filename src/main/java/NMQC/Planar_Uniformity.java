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

    private void getUniformity(ImagePlus imp, Roi sFOV, int shrinkfactor, ResultsTable rt) {
        Binner bin = new Binner();
        ImageProcessor ip2 = bin.shrink(imp.getProcessor(), shrinkfactor, shrinkfactor, Binner.SUM);
        ImagePlus imp2 = new ImagePlus("Convolved " + sFOV, ip2);
        imp2.deleteRoi();
        double scale = 1.0 / shrinkfactor;
        Roi lFOV = RoiScaler.scale(sFOV, scale, scale, false);
        lFOV = RoiEnlarger.enlarge(lFOV, -1);//To avoid boundaries
        Overlay list = new Overlay();
        list.add(lFOV);
        float[] kernel = {1, 2, 1, 2, 4, 2, 1, 2, 1};
        Convolver cv = new Convolver();
        cv.setNormalize(true);
        cv.convolve(ip2, kernel, 3, 3);
        imp2.updateAndDraw();
        ImageStatistics is0 = ip2.getStatistics();

        float[][] pixels = ip2.getFloatArray();
        double DU = 0;
        int w = ip2.getWidth();
        int h = ip2.getHeight();

        double globalmin = is0.max;
        double globalmax = is0.min;
        FPoint2D PBase = new FPoint2D(sFOV.getBounds().x / shrinkfactor, sFOV.getBounds().y / shrinkfactor);

        for (int j = (int) Math.round(PBase.Y); j < lFOV.getFloatHeight() + PBase.Y; j++) {
            for (int i = (int) Math.round(PBase.X); i < lFOV.getFloatWidth() + PBase.X; i++) {
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
                    DU = Math.max(DU, ((localmax - localmin) / (localmax + localmin)) * 100);
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
                    DU = Math.max(DU, ((localmax - localmin) / (localmax + localmin)) * 100);
                }
            }
        }

        double IU = ((globalmax - globalmin) / (globalmax + globalmin)) * 100;

        PointRoi minPointRoi = new PointRoi(minvalue.X, minvalue.Y);
        minPointRoi.setStrokeColor(Color.blue);
        list.add(minPointRoi);
        PointRoi maxPointRoi = new PointRoi(maxvalue.X, maxvalue.Y);
        maxPointRoi.setStrokeColor(Color.red);
        list.add(maxPointRoi);

        ip2.setMinAndMax(globalmin, globalmax);
        imp2.setOverlay(list);
        imp2.show();
        //RM.runCommand(imp2, "Show All");

        rt.addValue("Integral Uniformity", IU);
        rt.addValue("Differential Uniformity", DU);
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
        ImageStatistics is = imp.getStatistics();
        double factor = (is.min+ 1)/is.max;

        rt.incrementCounter();
        rt.addValue("ROI", "UFOV");
        FOV = Constants.getThreshold(imp, factor, 0.95);
        list.add(FOV);
        getUniformity(imp, FOV, shrinkfactor, rt);

        rt.incrementCounter();
        rt.addValue("ROI", "CFOV");
        
        FOV = Constants.getThreshold(imp, factor, 0.75);
        list.add(FOV);
        getUniformity(imp, FOV, shrinkfactor, rt);

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
