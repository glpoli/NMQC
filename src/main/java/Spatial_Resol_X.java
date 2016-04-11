package NMQC;

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import NMQC.utils.*;

/**
 *
 * @author alex
 */
public class Spatial_Resol_X implements PlugInFilter {

    private ImagePlus imp;
    private Roi roi;

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
        roi = imp.getRoi();
        if (roi == null) {
            IJ.error("Selection required");
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

        //int w = ip.getWidth();
        //int h = ip.getHeight();
        Calibration cal = imp.getCalibration();
        double vw = cal.pixelWidth;
        //double vh = cal.pixelHeight;
        float[][] pixels = ip.getFloatArray();
        double[] suma = new double[(int) (roi.getFloatWidth())];

        for (int j = 0; j < roi.getFloatHeight(); j++) {
            for (int i = 0; i < roi.getFloatWidth(); i++) {
                suma[i] += pixels[i + (int) roi.getXBase()][j + (int) roi.getYBase()];
            }
        }

        for (int i = 0; i < roi.getFloatWidth(); i++) {
            suma[i] = suma[i] / roi.getFloatHeight();
        }

        FPoint2D maximo = new FPoint2D(1, suma[0]);
        FPoint2D maximo2 = new FPoint2D(0, 0);
        boolean foundmax = false;
        for (int i = 2; i < roi.getFloatWidth(); i++) {
            if (suma[i] > maximo.Y) {
                maximo.assign(i, suma[i]);
            }
            if ((suma[i] <= (0.1) * maximo.Y) && !(foundmax)) {
                maximo2.assign(maximo);
                maximo.assign(i, suma[i]);
                foundmax = true;
            }
        }

        int med = (int) (0.5 * (maximo.X + maximo2.X));
        double[] arr1 = new double[med];
        double[] x1 = new double[med];
        double[] arr2 = new double[(int) roi.getFloatWidth() - med + 1];
        double[] x2 = new double[(int) roi.getFloatWidth() - med + 1];
        for (int i = 0; i < med; i++) {
            arr1[i] = suma[i];
            x1[i] = i;
        }
        for (int i = med; i < roi.getFloatWidth(); i++) {
            arr2[i - med] = suma[i];
            x2[i - med] = i;
        }

        double res1 = Fitter.resolution(x1, arr1, vw);
        double res2 = Fitter.resolution(x2, arr2, vw);

        ResultsTable rt =  new ResultsTable();
        rt.incrementCounter();
        rt.addValue("Res1(mm)", res1);
        rt.addValue("Res2(mm)", res2);
        rt.showRowNumbers(true);
        rt.show("Spatial resolution in X");
    }

    void showAbout() {
        IJ.showMessage("About Spatial Resolution...",
                "Este plugin es para hallar la resolucion espacial.");
    }
}
