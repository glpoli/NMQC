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
package NMQC.Tools;

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;

/**
 *
 * @author alex
 */
public class Geometric_Mean implements PlugInFilter {

    private ImagePlus imp1;
    private ImagePlus imp2;

    /**
     *
     * @param arg
     * @param imp
     * @return
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        return DOES_ALL;
    }

    /**
     *
     * @param ip
     */
    @Override
    public void run(ImageProcessor ip) {
        String[] title = WindowManager.getImageTitles();
        if (title.length < 2) {
            IJ.error("You need at least two images open");
            return;
        }
        GenericDialog gd = new GenericDialog("Settings.");
        gd.addChoice("Select anterior image:", title, title[0]);
        gd.addChoice("Select posterior image:", title, title[1]);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String choice1 = gd.getNextChoice();
        String choice2 = gd.getNextChoice();
        imp1 = WindowManager.getImage(choice1);
        imp2 = WindowManager.getImage(choice2);
        ImageProcessor ip1 = imp1.getProcessor();
        ImageProcessor ip2 = imp2.getProcessor();
        int w = ip1.getWidth();
        int h = ip1.getHeight();
        int ns = imp1.getStackSize();
        int nst = imp2.getStackSize();
        if (ns != nst) {
            IJ.error("Both images must have the same dimensions");
            return;
        }
        ImageStack ip0 = new ImageStack(w, h);

        for (int z = 1; z <= ns; z++) {
            imp1.setSlice(z);
            imp2.setSlice(z);
            float[][] pixels1 = ip1.getFloatArray();
            float[][] pixels2 = ip2.getFloatArray();
            if (pixels1.length != pixels2.length) {
                IJ.error("The size of both images must be the same!");
                return;
            }

            float[][] pixels0 = new float[ip1.getWidth()][ip1.getHeight()];
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    pixels0[i][j] = (float) Math.sqrt(pixels1[w - i - 1][j] * pixels2[i][j]);
                }
            }

            FloatProcessor ipt = new FloatProcessor(pixels0);
            ip0.addSlice(ipt);
        }

        ImagePlus imp0 = new ImagePlus("Geometric mean.", ip0);
        imp0.updateAndDraw();
        imp0.show();

    }

    void showAbout() {
        IJ.showMessage(" About Geometric Mean...",
                "This plugin calculates the Geometric Mean of two images in an AP view");
    }
}
