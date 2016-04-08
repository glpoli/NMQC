package NMQC;

import ij.*;
import ij.util.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import NMQC.utils.*;

/**
 *
 * @author alex
 */
public class C_O_R implements PlugInFilter {

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

        int ns = imp.getStackSize();
        //IJ.showMessage("Number of images:" + ns);
        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        double vh = cal.pixelHeight;
        //double vd = cal.pixelDepth;
        double[] cmx = new double[ns];
        double[] cmy = new double[ns];
        double[] it = new double[ns];

        for (int z = 1; z <= ns; z++) {
            imp.setSlice(z);
            it[z - 1] = z;
            ImageStatistics is = ip.getStatistics();
            cmx[z - 1] = is.xCenterOfMass * vw;
            cmy[z - 1] = is.yCenterOfMass * vh;
        }

        //ResultsTable rt = new ResultsTable();

        /*for (int i = 0; i < it.length; i++) {
            rt.incrementCounter();
            rt.addValue(" ", it[i]);
            rt.addValue("XM", cmx[i]);
            rt.addValue("YM", cmy[i]);
        }
        rt.showRowNumbers(false);
        rt.show("Results");*/

        CurveFitter cf = new CurveFitter(it, cmx);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        java.lang.String equation = "y = a + b * sin(c * x + d)";
        double[] initialParams = new double[4];
        boolean showSettings = false;
        /* Using user input
        GenericDialog gd = new GenericDialog("CurveFitting settings");
        gd.addNumericField("a:", cmx[0], 3);
        gd.addNumericField("b:", 0.0, 3);
        gd.addNumericField("c:", 2 * Math.PI / ns, 3);
        gd.addNumericField("d:", 0.0, 3);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        
        for (int n = 0; n < 4; n++) {
            initialParams[n] = (float) gd.getNextNumber();
        }*/        
        
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
        Plotter.plot(cf, false);

        /* Show the fitting parameters
        ResultsTable rt1 = new ResultsTable();
        double[] params = cf.getParams();
        char pChar1 = 'a';
        for (int n = 0; n < 4; n++) {
            rt1.incrementCounter();
            rt1.addValue(" ", Character.toString(pChar1));
            rt1.addValue("Parameters", params[n]);
            pChar1++;
        }
        rt1.showRowNumbers(false);
        rt1.show("Parameters");*/

        //Para hallar CMY
        double avgy = Plotter.averag(cmy);
        double[] diferencia = new double[ns];
        for (int i = 0; i < cmy.length - 1; i++) {
            diferencia[i] = Math.abs(cmy[i] - avgy);
        }

        double[] rest = cf.getResiduals();
        double[] b = Tools.getMinMax(rest);
        double[] c = Tools.getMinMax(diferencia);

        /*IJ.showMessage("Center of Rotation X: " + IJ.d2s(b[1], 3, 9) + " cm.\n"
                + "Center of Rotation Y: " + IJ.d2s(c[1], 3, 9) + " cm.");*/

        ResultsTable rt2 = new ResultsTable();
        //rt2.reset();
        for (int i = 0; i < 1; i++) {
            rt2.incrementCounter();
            rt2.addValue("COR X (cm)", IJ.d2s(b[1], 5, 9));
            rt2.addValue("COR Y (cm)", IJ.d2s(c[1], 5, 9));
        }
        rt2.showRowNumbers(false);
        rt2.show("Center of Rotations");
    }

    void showAbout() {
        IJ.showMessage(" About COR...",
                "Para determinar el centro de rotación de una cámara gamma.\n"+
                "To determinate the center of rotation of gamma camera");
    }
}
