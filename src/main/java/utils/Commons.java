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
package utils;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.util.ArrayUtil;
import ij.util.Tools;
import static ij.util.Tools.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author alex.vergara
 */
public class Commons {

    public static int NEMAWIDTH = 8;

    public static double[] toPrimitive(Double[] array) {
        return Stream.of(array).mapToDouble(Double::doubleValue).toArray();
    }

    public static String getStringValueFromInfo(String Info, String key) {
        int i = Info.indexOf(key);
        if (i < 0) {
            IJ.error("Error while reading header", "No info for key " + key + " in dicom header");
        }
        while (i > 0 && Character.isLetterOrDigit(Info.charAt(i - 1))) {
            i = Info.indexOf(key, i + key.length());
        }
        int index1 = i + key.length();
        int index2 = Info.indexOf("\n", index1);
        if (index2 == -1) {
            index2 = Info.length();
        }
        String Value = Info.substring(index1, index2);
        String sep = ": ";
        i = Value.indexOf(sep);// standard 'key: value' pair?
        if (i < 0) {
            sep = " = ";
            i = Value.indexOf(sep);// Bio-Formats metadata?
            if (i < 0) {
                IJ.error("Error while reading header", "Bad header or not a dicom header");
            }
        }
        return Value.substring(i + sep.length());
    }

    public static double getNumericValueFromInfo(String Info, String key) {
        return parseDouble(getStringValueFromInfo(Info, key));
    }

    /**
     * Returns the coordinates of the pixels inside this ROI as an array of
     * Points.
     *
     * @param roi
     * @return an array with all the points included in the roi
     * @see #getContainedFloatPoints()
     * @see #Iterator()
     * @Deprecated it is already included in imagej 1.51a
     */
    @Deprecated
    public static FPoint2D[] getContainedPoints(Roi roi) {
        if (roi.isLine()) {
            FloatPolygon p = roi.getInterpolatedPolygon();
            FPoint2D[] points = new FPoint2D[p.npoints];
            for (int i = 0; i < p.npoints; i++) {
                points[i] = new FPoint2D((int) Math.round(p.xpoints[i]), (int) Math.round(p.ypoints[i]));
            }
            return points;
        }
        ImageProcessor mask = roi.getMask();
        Rectangle bounds = roi.getBounds();
        ArrayList points = new ArrayList();
        for (int y = 0; y < bounds.height; y++) {
            for (int x = 0; x < bounds.width; x++) {
                if (mask == null || mask.getPixel(x, y) != 0) {
                    points.add(new FPoint2D((int) roi.getXBase() + x, (int) roi.getYBase() + y));
                }
            }
        }
        return (FPoint2D[]) points.toArray(new FPoint2D[points.size()]);
    }

    /**
     *
     * @param imp The active image
     * @param min The percentage of the max to be considered for the boundary
     * polygon
     * @param max The cuttof to shrink boundary polygon
     * @return The FOV for the specified cutoff
     */
    public static Roi getThreshold(ImagePlus imp, double min, double max) {
        ImageProcessor ip2 = imp.getProcessor().duplicate();
        //ImagePlus imp2 = new ImagePlus("Thresholded " + imp.getTitle(), ip2);
        ip2.setThreshold(min, ip2.getStatistics().max, ImageProcessor.BLACK_AND_WHITE_LUT);
        ThresholdToSelection ts = new ThresholdToSelection();
        Roi roi = ts.convert(ip2);
        //ConvexHull ch = new ConvexHull(points);
        //points = ch.getConvexHull();
        PolygonRoi CHroi;
        if (IJ.getVersion().contains("1.51")) {
            CHroi = new PolygonRoi(roi.getContainedFloatPoints(), Roi.POLYGON);
        } else {
            FPoint2D[] points = getContainedPoints(roi);
            CHroi = new PolygonRoi(FPoint2D.getXPoints(points), FPoint2D.getYPoints(points), Roi.POLYGON);
        }

        CHroi = new PolygonRoi(CHroi.getConvexHull(), Roi.POLYGON);

        //the final roi shall be a fraction of current roi
        double theight = CHroi.getBounds().height;
        double twidth = CHroi.getBounds().width;
        double pixelshrink = (max - 1) * Math.max(theight, twidth) / 2;
        Roi UFOV = RoiEnlarger.enlarge(CHroi, pixelshrink);
        return UFOV;
    }

    public static Roi getObject(ImagePlus imp, Point p, double level) {
        ImageProcessor ip2 = imp.getProcessor().duplicate();
        ImageStatistics is2 = ip2.getStatistics();
        ip2.setThreshold(Math.min(is2.max * level, ip2.getPixel((int) p.getX(), (int) p.getY())), is2.max, ImageProcessor.BLACK_AND_WHITE_LUT);
        ThresholdToSelection ts = new ThresholdToSelection();
        Roi roi = ts.convert(ip2);
        ShapeRoi Sroi = new ShapeRoi(roi);
        Roi[] listroi = Sroi.getRois();
        for (Roi r : listroi) {
            if (r.contains((int) p.getX(), (int) p.getY())) {
                roi = r;
            }
        }
        return roi;
    }

    public static Overlay getObjects(ImagePlus imp, PointRoi p, double level) {
        ImageProcessor ip2 = imp.getProcessor().duplicate();
        ImageStatistics is2 = ip2.getStatistics();
        int xmin = (int) p.getContainedPoints()[0].getX();
        int ymin = (int) p.getContainedPoints()[0].getY();
        double value = ip2.getPixelValue(xmin, ymin);
        for (Point tp : p.getContainedPoints()) {
            double nvalue = ip2.getPixelValue((int) tp.getX(), (int) tp.getY());
            if (nvalue < value) {
                value = nvalue;
                xmin = (int) tp.getX();
                ymin = (int) tp.getY();
            }
        }
        ip2.setThreshold(Math.min(is2.max * level, ip2.getPixelValue(xmin, ymin)), is2.max, ImageProcessor.BLACK_AND_WHITE_LUT);
        ThresholdToSelection ts = new ThresholdToSelection();
        Roi roi = ts.convert(ip2);
        ShapeRoi Sroi = new ShapeRoi(roi);
        Roi[] listroi = Sroi.getRois();
        Overlay result = new Overlay();
        for (Roi r : listroi) {
            for (Point tp : p.getContainedPoints()) {
                if (r.contains((int) tp.getX(), (int) tp.getY())) {
                    result.add(r);
                }
            }
        }
        return result;
    }

}
