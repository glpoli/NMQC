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

public class FPoint2D {

    public double X, Y;

    public FPoint2D(double xi, double yi) {
        X = xi;
        Y = yi;
    }

    public FPoint2D(FPoint2D p1) {
        X = p1.X;
        Y = p1.Y;
    }

    public void assign(double xi, double yi) {
        X = xi;
        Y = yi;
    }

    public void assign(FPoint2D p1) {
        X = p1.X;
        Y = p1.Y;
    }

    public String Print() {
        return "X: " + IJ.d2s(X, 4, 9) + "; Y: " + IJ.d2s(Y, 4, 9);
    }
}
