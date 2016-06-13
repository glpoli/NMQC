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

public class FPoint2D implements Comparable<FPoint2D> {

    private double X, Y;
    private FPoint2D comparatorPoint = null;

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

    public static boolean isCounterclockwise(FPoint2D a, FPoint2D b, FPoint2D c) {
        // determines if the angle formed by a -> b -> c is counterclockwise
        double signed_area_doubled = (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY())
                * (c.getX() - a.getX());
        return (signed_area_doubled > 0);
    }

    public static void printPoints(FPoint2D[] points) {
        for (FPoint2D p : points) {
            IJ.log(p.Print());
        }
    }

    protected void setComparatorPoint(FPoint2D p) {
        this.comparatorPoint = p;
    }

    public double getPolarRadius() {
        return Math.sqrt(X * X + Y * Y);
    }

    // gets the polar angle between this point and origin of the plane
    public double getPolarAngle() {
        // java library requires swapped arguments
        double arctan = Math.atan2(getY(), getX());
        return (arctan >= 0) ? arctan : (Math.PI * 2 - arctan);
    }

    // gets the polar angle between this point and the point given as
    // argument with the argument as origin
    public double getPolarAngle(FPoint2D p) {
        double x_n = p.getX() - X;
        double y_n = p.getY() - Y;
        return new FPoint2D(x_n, y_n).getPolarAngle();
    }

    public double getEuclideanDistance(FPoint2D p) {
        return Math.sqrt(Math.pow(p.getX() - X, 2) + Math.pow(p.getY() - Y, 2));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FPoint2D) {
            FPoint2D p = (FPoint2D) o;
            return p.getX() == X && p.getY() == Y;
        } else {
            return false;
        }
    }

    public int compareTo(FPoint2D p) {
        // compares two points in the plane according to the polar angle they
        // form with the comparator point of this object
        // if a comparator point is not provided, the default is the origin
        // of the plane
        if (comparatorPoint == null) {
            comparatorPoint = new FPoint2D(0, 0);
        }
        Double angle1 = comparatorPoint.getPolarAngle(this);
        Double angle2 = comparatorPoint.getPolarAngle(p);
        return angle1.compareTo(angle2);
    }

    @Override
    public String toString() {
        return Print();
    }

    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

    public static float[] getXPoints(FPoint2D[] points) {
        float[] xs = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            xs[i] = (float)points[i].getX();
        }
        return xs;
    }

    public static float[] getYPoints(FPoint2D[] points) {
        float[] ys = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            ys[i] = (float)points[i].getY();
        }
        return ys;
    }
}
