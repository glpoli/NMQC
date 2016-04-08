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
public class Pixel_Size_X implements PlugInFilter {

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

        float[][] pixels = ip.getFloatArray();
        double[] suma = new double[(int) roi.getFloatWidth()];

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
        double c1 = Plotter.peakpos(x1, arr1, false);
        double c2 = Plotter.peakpos(x2, arr2, false);

        double c = c2 - c1;

        GenericDialog gd = new GenericDialog("Pixel Size");
        gd.addNumericField("Enter distance between sources (cm):", 10, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        double d = gd.getNextNumber();
        double size = d / c;

        ResultsTable rt = ResultsTable.getResultsTable();
        rt.incrementCounter();
        rt.addValue("Pixel size (cm)", size);
        rt.showRowNumbers(true);
        rt.show("Pixel size in X");
    }

    void showAbout() {
        IJ.showMessage(" Acerca de tamano del pixel...",
                "Este plugin es para hallar el tamano del pixel en X.");
    }
}
