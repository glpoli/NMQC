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
import ij.plugin.filter.PlugInFilter;

/**
 *
 * @author alex
 */
public class Cardiac_Reslicer implements PlugInFilter {

    private ImagePlus imp;

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
        if (imp == null) {
            IJ.noImage();
            return DONE;
        }
        this.imp = imp;
        return DOES_ALL;
    }

    /**
     *
     * @param ip
     */
    @Override
    public void run(ImageProcessor ip) {
        GenericDialog gd = new GenericDialog("Settings.");
        gd.addNumericField("Enter number of views", 60, 2);
        gd.addNumericField("Enter number of segments", 16, 1);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        int ns = imp.getStackSize();
        int views = (int) gd.getNextNumber();
        int segments = (int) gd.getNextNumber();
        int sctwin = (int) ((ns / (views * segments))) - 1;

        ImageStack emision= new ImageStack(ip.getWidth(), ip.getHeight());
        ImageStack[] scatter = new ImageStack[sctwin];
        for (int i = 0; i < sctwin; i++) {
            scatter[i] = new ImageStack(ip.getWidth(), ip.getHeight());
        }

        for (int z = 1; z <= ns;) {
            imp.setSlice(z);
            for (int i = 0; i < sctwin; i++) {
                for (int j = 0; j < views; j++) {
                    scatter[i].addSlice(ip.convertToShortProcessor());
                    imp.setSlice(++z);
                }
            }
            for (int j = 0; j < views; j++) {
                emision.addSlice(ip.convertToShortProcessor());
                imp.setSlice(++z);
            }
        }

        ImagePlus[] theScatter = new ImagePlus[sctwin];
        for (int i = 0; i < sctwin; i++) {
            theScatter[i] = new ImagePlus("Scatter " + i, scatter[i]);
            theScatter[i].show();
        }
        ImagePlus theEmission = new ImagePlus("Emission", emision);
        theEmission.show();

    }

    void showAbout() {
        IJ.showMessage(" About Cardiac Reslicer...",
                "This plugin reslice the stack in a gated cardiac study with scatter windows");
    }
}
