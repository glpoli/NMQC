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
package Tools;

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import utils.Commons;

/**
 *
 * @author alex.vergara
 */
public class Select_Object implements PlugInFilter {

    private ImagePlus imp;
    private PointRoi proi;

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
        this.proi = (PointRoi) imp.getRoi();
        if (proi == null) {
            IJ.error("Point Roi required");
            return ROI_REQUIRED;
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
        if (proi.getLength() > 1) {
            Overlay list = Commons.getObjects(imp, proi, 0.1);
            imp.setOverlay(list);
        } else {
            Roi roi = Commons.getObject(imp, proi.getContainedPoints()[0], 0.1);
            imp.setRoi(roi);
        }
    }

    void showAbout() {
        IJ.showMessage("About Select Object...",
                "Este plugin Selecciona un objeto que contiene el punto seleccionado.\n"
                + "This plugin selects an object containing the selected Point");
    }

}
