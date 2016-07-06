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
import ij.plugin.filter.*;
import utils.FPoint2D;
import ij.util.*;
import utils.*;

/**
 *
 * @author alex
 */
public class Tomographic_Uniformity implements PlugInFilter {

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

    private void getUniformity(ImagePlus simp, Roi sFOV, ResultsTable rt) {
        Roi lFOV = RoiEnlarger.enlarge(sFOV, -1);//To avoid boundaries
        float[][] Pixels = simp.getProcessor().getFloatArray();
        double[] gvector = new double[simp.getWidth()];
        int rmin = simp.getWidth();
        FPoint2D center = new FPoint2D(simp.getHeight() / 2, simp.getWidth() / 2);
        simp.setRoi(lFOV);
        ImageStatistics is = simp.getStatistics();

        double DU = 0;
        for (int i = 0; i < 360; i++) {
            int rmax = 0;
            double angle = 2 * Math.PI * i / 360;
            int lX = (int) (center.getX());
            int lY = (int) (center.getY());
            while (lFOV.contains(lX, lY)) {
                rmax += 1;
                lX = (int) (center.getX() + rmax * Math.cos(angle));
                lY = (int) (center.getY() + rmax * Math.sin(angle));
            }
            if (rmax < rmin) {
                rmin = rmax;
            }
            double[] vector = new double[rmax];
            for (int j = 0; j < rmax; j++) {
                lX = (int) (center.getX() + j * Math.cos(angle));
                lY = (int) (center.getY() + j * Math.sin(angle));
                vector[j] = Pixels[lX][lY];
                gvector[j] += vector[j];
            }
            double[] temp = Tools.getMinMax(vector);
            double lmin = temp[0];
            double lmax = temp[1];
            DU = Math.max(DU, Math.max(MathUtils.Contrast(is.mean, lmax), MathUtils.Contrast(is.mean, lmin)));
        }

        double[] ngvector = new double[rmin];
        System.arraycopy(gvector, 0, ngvector, 0, rmin);
        double centre = 0;
        double border = 0;
        for (int i = 0; i < Math.min(5, rmin / 2); i++) {
            centre += ngvector[i];
            border += ngvector[rmin - i - 1];
        }
        double IU = MathUtils.Contrast(centre, border);

        rt.addValue("Maximum Ring Contrast", DU);
        rt.addValue("Centre - Border Contrast", IU);
        
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("ROI", "UFOV");

        int ns = imp.getStackSize();
        ImageStack stack = imp.getImageStack();
        int sinit;
        int send;
        if (ns > 1) {
            GenericDialog gd = new GenericDialog("Tomographic Uniformity.");
            gd.addNumericField("Entre el corte inicial", 1, 0);
            gd.addNumericField("Entre el corte final", ns, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            sinit = (int) Math.round(gd.getNextNumber());
            send = (int) Math.round(gd.getNextNumber());
        } else {
            sinit = 1;
            send = 1;
        }
        float[][] ip2mat = new float[imp.getWidth()][imp.getHeight()];
        for (int z = sinit; z <= send; z++) {
            ImageProcessor ip2 = stack.getProcessor(z);
            float[][] pixels = ip2.getFloatArray();
            for (int i = 0; i < ip2.getWidth(); i++) {
                for (int j = 0; j < ip2.getHeight(); j++) {
                    ip2mat[i][j] += pixels[i][j];
                }
            }
        }
        for (int i = 0; i < stack.getWidth(); i++) {
            for (int j = 0; j < stack.getHeight(); j++) {
                ip2mat[i][j] /= send - sinit + 1;
            }
        }
        FloatProcessor ip2 = new FloatProcessor(ip2mat);
        ImagePlus imp2 = new ImagePlus("Mean Image " + imp.getTitle() + ": Frames " + sinit + " to " + send, ip2);
        ImageStatistics is2 = imp2.getStatistics();
        Roi FOV = Commons.getThreshold(imp2, 0.1*is2.max, 0.9); // 10% of max value for threshold
        getUniformity(imp2, FOV, rt);
        imp2.show();
        rt.showRowNumbers(true);
        rt.show("Tomographic Uniformity " + imp.getTitle() + ": Frames " + sinit + " to " + send);
        
        FileInfo fi = imp.getOriginalFileInfo();
        Commons.saveRT(rt, fi.directory, fi.fileName);

    }

    void showAbout() {
        IJ.showMessage("About Tomographic Uniformity...",
                "Este plugin es para hallar la uniformidad tomográfica en reconstrucciones 3D.\n"
                + "This plugin finds the tomographic uniformity in 3D reconstructions");
    }

}
