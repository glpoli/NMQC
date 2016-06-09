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
import ij.util.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import utils.*;

/**
 *
 * @author alex
 */
public class C_O_R implements PlugInFilter {

    private ImagePlus imp;
    private String Method;

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
        this.Method = arg;
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

        int ns = imp.getStackSize();
        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        //double vd = cal.pixelDepth;
        double[] cmx = new double[ns];
        double[] cmy = new double[ns];
        double[] it = new double[ns];

        //Reading Dicom header for angle data
        String Info = imp.getInfoProperty();
        String ScanArckey = "0018,1143";
        String StartAnglekey = "0054,0200";
        String RotationDirectiokey = "0018,1140";

        double ScanArc = Constants.getNumericValueFromInfo(Info, ScanArckey);
        double StartAngle = Constants.getNumericValueFromInfo(Info, StartAnglekey);
        String RotationDirection = Constants.getStringValueFromInfo(Info, RotationDirectiokey);
        double anglestep = 0;
        if (RotationDirection.contains("CCW")) {
            anglestep = ScanArc / ns;
        } else if (RotationDirection.contains("CW")) {
            anglestep = -ScanArc / ns;
        } else {
            ScanArc = ns;
            StartAngle = 0;
            anglestep = 1;
        }

        for (int z = 1; z <= ns; z++) {
            imp.setSlice(z);
            it[z - 1] = (StartAngle + z * anglestep) * 2 * Math.PI / 360;
            ImageStatistics is = ip.getStatistics();
            cmx[z - 1] = is.xCenterOfMass;
            cmy[z - 1] = is.yCenterOfMass;
        }

        //To determinate the offset in X
        CurveFitter cf = new CurveFitter(it, cmx);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        java.lang.String equation = "y = a + b * sin(c * x + d)";
        double[] initialParams = new double[4];
        boolean showSettings = false;

        // Using default initialization
        initialParams[0] = cmx[0];
        initialParams[1] = 0.0;
        initialParams[2] = 2 * Math.PI / ns;
        initialParams[3] = 0.0;

        int params_e = cf.doCustomFit(equation, initialParams, showSettings);
        if (params_e == 0) {
            IJ.beep();
            IJ.log("Bad formula; should be:\n   y = function(x, a, ...)");
            return;
        }
        if (cf.getStatus() == Minimizer.INITIALIZATION_FAILURE) {
            IJ.beep();
            IJ.showStatus(cf.getStatusString());
            IJ.log("Curve Fitting Error:\n" + cf.getStatusString());
            return;
        }
        if (Double.isNaN(cf.getSumResidualsSqr())) {
            IJ.beep();
            IJ.showStatus("Error: fit yields Not-a-Number");
            return;
        }

        //To determinate the offset in Y
        double avgy = MathUtils.averag(cmy);
        double[] diferencia = new double[ns];
        for (int i = 0; i < cmy.length - 1; i++) {
            diferencia[i] = Math.abs(cmy[i] - avgy);
        }

        double[] rest = cf.getResiduals();
        double[] b = Tools.getMinMax(rest);
        double[] c = Tools.getMinMax(diferencia);

        ResultsTable rt = new ResultsTable();
        rt.showRowNumbers(false);

        if (Method.contains("Sine")) {//Sine fit
            Plotter.plot(cf, false);
            rt.incrementCounter();
            rt.addValue("Test", "COR X");
            rt.addValue("px", IJ.d2s(b[1], 5, 9));
            rt.addValue("mm", IJ.d2s(b[1] * vw, 5, 9));
            rt.incrementCounter();
            rt.addValue("Test", "COR Y");
            rt.addValue("px", IJ.d2s(c[1], 5, 9));
            rt.addValue("mm", IJ.d2s(c[1] * vh, 5, 9));

            rt.show("Center of Rotation: Sine Fit " + imp.getTitle());
        }
        if (Method.contains("Conjugate")) {//Conjugate views
            if (ScanArc != 360.0) {
                IJ.error("Error in Connjugate View Method", "Scan Arc must be 360 for this method\n"
                        + "Current Scan Arc is: " + IJ.d2s(ScanArc));
                return;
            }

            int rsize = (int) ns / 2;
            double[] Rx = new double[rsize];
            for (int i = 0; i < rsize; i++) {
                Rx[i] = (imp.getWidth() + 1 - cmx[i] - cmx[i + rsize]) / 2;
            }

            b = Tools.getMinMax(Rx);

            rt.incrementCounter();
            rt.addValue("Test", "COR X");
            rt.addValue("px", IJ.d2s(b[1], 5, 9));
            rt.addValue("mm", IJ.d2s(b[1] * vw, 5, 9));
            rt.incrementCounter();
            rt.addValue("Test", "COR Y");
            rt.addValue("px", IJ.d2s(c[1], 5, 9));
            rt.addValue("mm", IJ.d2s(c[1] * vh, 5, 9));

            rt.show("Center of Rotation: Conjugate Views " + imp.getTitle());
        }

    }

    void showAbout() {
        IJ.showMessage("About COR...",
                "Este plugin determina el corrimiento del centro de rotación de una cámara gamma.\n\n"
                + "This plugin determines the offset of the center of rotation of gamma camera");
    }
}
