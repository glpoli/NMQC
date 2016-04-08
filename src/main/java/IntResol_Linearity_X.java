package NMQC;

import ij.*;
import ij.gui.*;
import ij.util.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import NMQC.utils.*;

/**
 *
 * @author alex
 */
public class IntResol_Linearity_X implements PlugInFilter {

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

        Calibration cal = imp.getCalibration();				// Declara la variable cal de donde se saca el ancho, alto y profundidad del pixel.
        double vw = cal.pixelWidth;							// Se declara el ancho del pixel.
        double vh = cal.pixelHeight;						// Se declara la altura del pixel.
        float[][] pixels = ip.getFloatArray();				// Se guarda en el arreglo pixels[][] los pixels de la imagen.

        int nbins = (int) (roi.getFloatWidth() * vw / 30);
        IJ.write("Number of bins: " + nbins);

        double[][] counts = new double[nbins][(int) roi.getFloatHeight()];
        double dpos = roi.getFloatWidth() / (nbins - 1);

        for (int j = 0; j < roi.getFloatHeight(); j++) {			// Se recorre la imagen 
            for (int i = 0; i < nbins; i++) {
                for (int k = 0; k < dpos; k++) {
                    counts[i][j] += pixels[i * k + (int) roi.getXBase()][j + (int) roi.getYBase()];
                }
                counts[i][j] /= (int) dpos;
            }
        }

        double resol = 0;
        double meanresol = 0;
        int countpeaks = 0;
        double[][] peakpositions = new double[nbins][];
        double[][] x = new double[nbins][];
        int npeaks = 0;
        for (int i = 0; i < nbins; i++) {
            int[] peakpos = Plotter.findPeaks(counts[i]);
            if (peakpos.length > npeaks) {
                npeaks = peakpos.length;
            }
            peakpositions[i] = new double[peakpos.length];
            x[i] = new double[peakpos.length];
            //String lin = "pos:";
            int med = 0;
            for (int j = 0; j < peakpos.length - 1; j++) {
                int med1 = (int) (0.5 * (peakpos[j] + peakpos[j + 1]));
                double[] arr1 = new double[med1 - med];
                double[] x1 = new double[med1 - med];
                for (int k = 0; k < med1 - med; k++) {
                    arr1[k] = counts[i][k + med];
                    x1[k] = k + med;
                }
                peakpositions[i][j] = Plotter.peakpos(x1, arr1, false) * vh;
                //lin+= " " + peakpositions[i][j];
                med = med1;
                x[i][j] = j;
                double tresol = Plotter.resolution(x1, arr1, vh, false);
                resol = Math.max(resol, tresol);
                meanresol += tresol;
                countpeaks += 1;
            }
            double[] arr = new double[counts[i].length - med];
            double[] xf = new double[counts[i].length - med];
            for (int k = 0; k < counts[i].length - med; k++) {
                arr[k] = counts[i][k + med];
                xf[k] = k + med;
            }
            peakpositions[i][peakpos.length - 1] = Plotter.peakpos(xf, arr, false) * vh;
            x[i][peakpos.length - 1] = peakpos.length - 1;
            double tresol = Plotter.resolution(xf, arr, vh, false);
            resol = Math.max(resol, tresol);
            meanresol += tresol;
            countpeaks += 1;
            //IJ.write(lin);
        }

        //for (int i=0; i<nbins; i++) Plotter.LinearFit(x[i],peakpositions[i]);
        double[] residuals = new double[npeaks * nbins];
        for (int j = 0; j < npeaks; j++) {
            double[] newx = new double[nbins];
            double[] newpos = new double[nbins];
            for (int i = 0; i < nbins; i++) {
                newx[i] = i;
                newpos[i] = peakpositions[i][j];
            }
            //Plotter.LinearFit(newx,newpos);
            double[] tresiduals = Plotter.getResidualsinLinearFit(newx, newpos);
            System.arraycopy(tresiduals, 0, residuals, j * nbins, nbins);
        }
        double[] a = Tools.getMinMax(residuals);
        ResultsTable rt = new ResultsTable();
        //rt.reset();
        for (int i = 0; i < 1; i++) {
            rt.incrementCounter();
            rt.addValue("Worst Intrinsic Resolution in X(mm): ", IJ.d2s(resol, 4, 9));
            rt.addValue("Mean Intrinsic Resolution in X(mm): ", IJ.d2s(meanresol / countpeaks, 4, 9));
            rt.addValue("Absolute Linearity in X(mm): ", IJ.d2s(a[1], 4, 9));
            rt.addValue("Differential Linearity in X(mm): ", IJ.d2s(Plotter.StdDev(residuals), 4, 9));
        }
        rt.showRowNumbers(false);
        rt.show("Intrinsic Resolution and Linearity in X");
    }

    void showAbout() {
        IJ.showMessage(" About Intrinsic Resolution and Linearity...",
                "Este plugin determina el peor valor y el promedio de los valores de resolucion intrinseca en el eje X en mm\n"
                 + " y determina la linealidad absoluta y diferencial en el eje X en mm.\n"
                 + "This plugin determinate the worst value and the mean of the values of intrinsic resolution\n"
                 + " and determinate the absolute linearity and differential linearity in X in mm.\n");
    }
}
