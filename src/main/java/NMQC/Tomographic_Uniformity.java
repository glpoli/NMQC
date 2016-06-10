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
        
        double DU = 0;
        for (int i = 0; i < 360; i++) {
            int rmax = 0;
            double angle = 2 * Math.PI * i / 360;
            int lX = (int) (center.X);
            int lY = (int) (center.Y);
            while (lFOV.contains(lX, lY)) {
                rmax += 1;
                lX = (int) (center.X + rmax * Math.cos(angle));
                lY = (int) (center.Y + rmax * Math.sin(angle));
            }
            if (rmax < rmin) {
                rmin = rmax;
            }
            double[] vector = new double[rmax];
            for (int j = 0; j < rmax; j++) {
                lX = (int) (center.X + j * Math.cos(angle));
                lY = (int) (center.Y + j * Math.sin(angle));
                vector[j] = Pixels[lX][lY];
                gvector[j] += vector[j];
            }
            double[] temp = Tools.getMinMax(vector);
            double lmin = temp[0];
            double lmax = temp[1];
            DU = Math.max(DU, ((lmax - lmin) / (lmax + lmin)) * 100);
        }

        double[] ngvector = new double[rmin];
        System.arraycopy(gvector, 0, ngvector, 0, rmin);
        double[] temp = Tools.getMinMax(ngvector);
        double gmax = temp[1];
        double gmin = temp[0];
        double IU = ((gmax - gmin) / (gmax + gmin)) * 100;

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
        rt.incrementCounter();
        rt.addValue("ROI", "UFOV");
        Overlay list = new Overlay();

        int ns = imp.getStackSize();
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
            imp.setSlice(z);
            float[][] pixels = imp.getProcessor().getFloatArray();
            for (int i = 0; i < imp.getWidth(); i++) {
                for (int j = 0; j < imp.getHeight(); j++) {
                    ip2mat[i][j] += pixels[i][j];
                }
            }
        }
        for (int i = 0; i < imp.getWidth(); i++) {
            for (int j = 0; j < imp.getHeight(); j++) {
                ip2mat[i][j] /= send - sinit + 1;
            }
        }
        FloatProcessor ip2 = new FloatProcessor(ip2mat);
        ImagePlus imp2 = new ImagePlus("Mean Image", ip2);
        Roi FOV = Constants.getThreshold(imp2, 0.1, 0.9); // 10% of max value for threshold
        list.add(FOV);
        getUniformity(imp2, FOV, rt);
        imp2.show();
        rt.showRowNumbers(true);
        rt.show("Tomographic Uniformity");
        imp2.setOverlay(list);
    }

    void showAbout() {
        IJ.showMessage("About Tomographic Uniformity...",
                "Este plugin es para hallar la uniformidad tomográfica en reconstrucciones 3D.\n"
                + "This plugin finds the tomographic uniformity in 3D reconstructions");
    }

}
