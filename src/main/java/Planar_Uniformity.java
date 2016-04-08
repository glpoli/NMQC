package NMQC;

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.gui.GenericDialog;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;

/**
 *
 * @author alex
 */
public class Planar_Uniformity implements PlugInFilter {

    private ImagePlus imp;
    //private final String[] form = {"UFOV", "CFOV"};
    //private final AutoThresholder.Method[] mMethod = AutoThresholder.Method.values();
    private final String[] mMethodStr = AutoThresholder.getMethods();

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

    private void getUniformity(ImagePlus imp, String choice, String sFOV) {
        Binner bin = new Binner();
        ImageProcessor ip2 = bin.shrink(imp.getProcessor(), 4, 4, Binner.SUM);
        ImagePlus imp2 = new ImagePlus("Convolved " + sFOV, ip2);
        imp2.deleteRoi();
        //GaussianBlur.blurGaussian(ip2, 2, 2, 1e-5);
        double cutoff;
        if (sFOV.equals("CFOV")) {
            cutoff = 0.75;
        } else {
            cutoff = 0.95;
        }
        Roi lFOV = getThreshold(imp2, choice, cutoff);
        imp2.setRoi(lFOV);
        //ip2.resetMinAndMax();
        float[] kernel = {1, 2, 1, 2, 4, 2, 1, 2, 1};
        Convolver cv = new Convolver();
        cv.setNormalize(true);
        cv.convolve(ip2, kernel, 3, 3);
        imp2.updateAndDraw();
        //imp2.show();
        //IJ.run(imp2, "Enhance Contrast", "");
        ImageStatistics is0 = ip2.getStatistics();
        //IJ.showMessage("min: "+is0.min+" max:"+is0.max+" IU:"+IU);

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
                    }
                    if (pixels[i][j] > globalmax) {
                        globalmax = pixels[i][j];
                    }
                    // Por filas
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
                    // Por columnas
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

        //imp2.show();
        ResultsTable rt = ResultsTable.getResultsTable();
        if (rt == null) {rt = new ResultsTable();}
        rt.incrementCounter();
        rt.addValue("ROI", sFOV);
        rt.addValue("Integral Uniformity", IU);
        rt.addValue("Differential Uniformity", DU);
        rt.showRowNumbers(true);
        rt.show("Planar uniformity");
        //return lFOV;
    }

    /**
     *
     * @param ip The image processor
     */
    @Override
    public void run(ImageProcessor ip) {
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
        
        //imp = WindowManager.getCurrentImage();
        RoiManager RM = RoiManager.getInstance();
        if (RM == null) {
            RM = new RoiManager();
        }
        RM.reset();
        Roi FOV;
        String sFOV = "UFOV";
        FOV = getThreshold(imp, choice, 0.95);
        RM.addRoi(FOV);
        getUniformity(imp, choice, sFOV);
        sFOV = "CFOV";
        FOV = getThreshold(imp, choice, 0.75);
        RM.addRoi(FOV);
        getUniformity(imp, choice, sFOV);
        RM.runCommand(imp, "Show All");
    }
    
    void showAbout() {
        IJ.showMessage(" Acerca de Uniformidad Planar...",
                "Este plugin es para hallar la uniformidad planar.");
    }

}
