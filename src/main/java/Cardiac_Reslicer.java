/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        int sctwin = (int) (ns / (views * segments));

        ImageStack emision = ImageStack.create(ip.getWidth(), ip.getHeight(), views, 16);
        ImageStack[] scatter = new ImageStack[sctwin];
        for (int i = 0; i < sctwin; i++) {
            scatter[i] = ImageStack.create(ip.getWidth(), ip.getHeight(), views, 16);
        }

        for (int z = 1; z <= ns; ) {
            imp.setSlice(z);
            for (int i = 0; i < sctwin; i++) {
                for (int j = 0; j < views; j++) {
                    scatter[i].addSlice(ip);
                    imp.setSlice(++z);
                }
            }
            for (int j = 0; j < views; j++) {
                emision.addSlice(ip);
                imp.setSlice(++z);
            }
        }
        
        ImagePlus[] theScatter = new ImagePlus[sctwin];
        for (int i = 0; i < sctwin; i++) {
            theScatter[i] = new ImagePlus("Scatter "+i, scatter[i]);
            theScatter[i].show();
        } 
        ImagePlus theEmission = new ImagePlus("Emission",emision);
        theEmission.show();

    }

    void showAbout() {
        IJ.showMessage(" About Cardiac Reslicer...",
                "This plugin reslice the stack in a gated cardiac study with scatter windows");
    }
}
