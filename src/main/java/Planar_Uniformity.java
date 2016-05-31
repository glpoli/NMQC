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
//import ij.gui.GenericDialog; // Enable this line if you want to enable the dialog
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import NMQC.utils.FPoint2D;
import java.awt.Color;

/**
 *
 * @author alex
 */
public class Planar_Uniformity implements PlugInFilter {

    private ImagePlus imp;
    private FPoint2D minvalue;
    private FPoint2D maxvalue;
    private final int shrinkfactor = 4;

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

    /**
     *
     * @param imp The active image
     * @param Method The method to calculate boundary, one of
     * AutoThresholder.Method.values()
     * @param cutoff The cuttof to shrink boundary poligon
     */
    private Roi getThreshold(ImagePlus imp, String Method, double cutoff) {
        ImageProcessor ip2 = imp.getProcessor().duplicate();
        ImagePlus imp2 = new ImagePlus("Thresholded " + Method, ip2);
        ip2.setAutoThreshold(Method);
        boolean darkBackground = Method.contains("dark");
        if (!darkBackground) {
            ip2.invert();
        }
        ThresholdToSelection ts = new ThresholdToSelection();
        Roi roi = ts.convert(ip2);
        imp2.setRoi(roi);

        //Altura
        /*double height0 = roi.getFloatHeight();
	  RoiEnlarger re = new RoiEnlarger();
	  double height1 = height0;
	  float pixelshrink = -1;
	  Roi UFOV = re.enlarge(roi, pixelshrink);
	  while (height1>cutoff*height0) {
	    pixelshrink -= 1;
	    UFOV = re.enlarge(roi, pixelshrink);
		imp2.setRoi(UFOV);
		height1 = UFOV.getFloatHeight();
	  }
	  return UFOV;*/
        //Ancho
        /*double width0 = roi.getFloatWidth();
	  RoiEnlarger re = new RoiEnlarger();
	  double width1 = width0;
	  float pixelshrink = -1;
	  Roi UFOV = re.enlarge(roi, pixelshrink);
	  while (width1>cutoff*width0) {
	    pixelshrink -= 1;
	    UFOV = re.enlarge(roi, pixelshrink);
		imp2.setRoi(UFOV);
		width1 = UFOV.getFloatWidth();
	  }
	  return UFOV;*/
        //Area
        ImageStatistics is0 = ip2.getStatistics();
        double area0 = is0.area;
        double area1 = area0;
        float pixelshrink = -1;
        Roi UFOV = RoiEnlarger.enlarge(roi, pixelshrink);
        ImageStatistics is1;
        while (area1 > cutoff * cutoff * area0) {
            pixelshrink -= 1;
            UFOV = RoiEnlarger.enlarge(roi, pixelshrink);
            imp2.setRoi(UFOV);
            is1 = ip2.getStatistics();
            area1 = is1.area;
        }
        return UFOV;
    }

    private void getUniformity(ImagePlus imp, String choice, String sFOV, ResultsTable rt) {
        Binner bin = new Binner();
        ImageProcessor ip2 = bin.shrink(imp.getProcessor(), shrinkfactor, shrinkfactor, Binner.SUM);
        ImagePlus imp2 = new ImagePlus("Convolved " + sFOV, ip2);
        imp2.deleteRoi();
        double cutoff = sFOV.equals("CFOV") ? 0.75 : 0.95;
        Roi lFOV = getThreshold(imp2, choice, cutoff);
        imp2.setRoi(lFOV);
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

        for (int j = (int) lFOV.getYBase(); j < lFOV.getFloatHeight() + lFOV.getYBase(); j++) {
            for (int i = (int) lFOV.getXBase(); i < lFOV.getFloatWidth() + lFOV.getXBase(); i++) {
                if (lFOV.contains(i, j)) {
                    float localmin = pixels[i][j];
                    float localmax = pixels[i][j];
                    if (pixels[i][j] < globalmin) {
                        globalmin = pixels[i][j];
                        minvalue.assign(i, j);
                    }
                    if (pixels[i][j] > globalmax) {
                        globalmax = pixels[i][j];
                        maxvalue.assign(i, j);
                    }
                    // By rows
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
        ip2.setMinAndMax(globalmin, globalmax);
        imp2.show();

        rt.incrementCounter();
        rt.addValue("ROI", sFOV);
        rt.addValue("Integral Uniformity", IU);
        rt.addValue("Differential Uniformity", DU);
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
        /*  Dialog to handle the method and the background
        GenericDialog gd = new GenericDialog("Planar Uniformity.");
        //gd.addChoice("Select FOV:", form, "UFOV");
        gd.addChoice("Select threshold method:", mMethodStr, "Default");
        gd.addCheckbox("Dark Background", true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        //String sFOV = gd.getNextChoice();
        String choice = gd.getNextChoice();
        boolean darkb = gd.getNextBoolean();
        if (darkb) {
            choice += " dark";
        }
         */ // end Dialog
        String choice = "Triangle dark"; // No dialog used
        /*ResultsTable rt = ResultsTable.getResultsTable();
        if (rt == null) {rt = new ResultsTable();}*/
        ResultsTable rt = new ResultsTable();
        RoiManager RM = RoiManager.getInstance();
        if (RM == null) {
            RM = new RoiManager();
        }
        RM.reset();

        int ns = imp.getStackSize();
        if (ns == 1) { // Planar Uniformity
            Roi FOV;
            String sFOV = "UFOV";
            FOV = getThreshold(imp, choice, 0.95);
            RM.addRoi(FOV);
            getUniformity(imp, choice, sFOV, rt);
            sFOV = "CFOV";
            FOV = getThreshold(imp, choice, 0.75);
            RM.addRoi(FOV);
            getUniformity(imp, choice, sFOV, rt);
            PointRoi minPointRoi = new PointRoi(minvalue.X * shrinkfactor, minvalue.Y * shrinkfactor);
            minPointRoi.setStrokeColor(Color.blue);
            PointRoi maxPointRoi = new PointRoi(maxvalue.X * shrinkfactor, maxvalue.Y * shrinkfactor);
            maxPointRoi.setStrokeColor(Color.red);
            RM.addRoi(minPointRoi);
            RM.addRoi(maxPointRoi);
            rt.showRowNumbers(true);
            rt.show("Planar Uniformity");
        } else if (ns > 1) { // Tomographic Uniformity
            GenericDialog gd = new GenericDialog("Tomographic Uniformity.");
            gd.addNumericField("Entre el corte inicial", 1, 0);
            gd.addNumericField("Entre el corte final", ns, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            int sinit = (int) Math.round(gd.getNextNumber());
            int send = (int) Math.round(gd.getNextNumber());
            float[][] ip2mat = new float[ip.getWidth()][ip.getHeight()];
            for (int z = sinit; z <= send; z++) {
                imp.setSlice(z);
                float[][] pixels = ip.getFloatArray();
                for (int i = 0; i < ip.getWidth(); i++) {
                    for (int j = 0; j < ip.getHeight(); j++) {
                        ip2mat[i][j] += pixels[i][j];
                    }
                }
            }
            for (int i = 0; i < ip.getWidth(); i++) {
                for (int j = 0; j < ip.getHeight(); j++) {
                    ip2mat[i][j] /= send - sinit + 1;
                }
            }
            FloatProcessor ip2 = new FloatProcessor(ip2mat);
            ImagePlus imp2 = new ImagePlus("Mean Image",ip2);
            Roi FOV;
            String sFOV = "UFOV";
            FOV = getThreshold(imp2, choice, 0.95);
            RM.addRoi(FOV);
            getUniformity(imp2, choice, sFOV, rt);
            PointRoi minPointRoi = new PointRoi(minvalue.X * shrinkfactor, minvalue.Y * shrinkfactor);
            minPointRoi.setStrokeColor(Color.blue);
            PointRoi maxPointRoi = new PointRoi(maxvalue.X * shrinkfactor, maxvalue.Y * shrinkfactor);
            maxPointRoi.setStrokeColor(Color.red);
            RM.addRoi(minPointRoi);
            RM.addRoi(maxPointRoi);
            rt.showRowNumbers(true);
            rt.show("Tomographic Uniformity");
        }
        RM.runCommand(imp, "Show All");
        RM.setVisible(false);
    }

    void showAbout() {
        IJ.showMessage("About Planar Uniformity...",
                "Este plugin es para hallar la uniformidad planar de imágenes planas.\n"
                +"También halla la uniformidad tomográfica en reconstrucciones 3D.\n"
                +"El tipo de imagen es detectado automáticamente");
    }

}
