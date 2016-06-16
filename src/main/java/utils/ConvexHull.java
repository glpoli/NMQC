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

import ij.gui.*;
import java.util.Arrays;

/*
 * Computing the convex hull of a set of points using
 * the Graham scan algorithm.
 * Application: find the two points with biggest euclidean distance between
 * them given a set of points.
 * http://en.wikipedia.org/wiki/Graham_scan
 */
public class ConvexHull {

    private final int n;
    private FPoint2D[] points;

    /**
     * Creator
     * @param points the points around which we create the convex hull
     */
    public ConvexHull(FPoint2D[] points) {
        this.n = points.length;
        this.points = points;
    }

    private FPoint2D getPointWithLowestYCoord() {
        FPoint2D lowest_point = points[0];
        for (int i = 1; i < n; i++) {
            if (points[i].getY() < lowest_point.getY()) {
                lowest_point = points[i];
            } else if (points[i].getY() < lowest_point.getY()) {
                if (points[i].getX() > lowest_point.getX()) {
                    lowest_point = points[i];
                }
            }
        }
        return lowest_point;
    }

    private int computeConvexHull() {
        if (n <= 2) {
            return n;
        }
        FPoint2D point_with_lowest_y_coord = getPointWithLowestYCoord();
        for (int i = 0; i < n; i++) {
            points[i].setComparatorPoint(point_with_lowest_y_coord);
        }
        Arrays.sort(points);
        FPoint2D[] mod_points = new FPoint2D[n + 1];
        System.arraycopy(points, 0, mod_points, 0, n);
        mod_points[n] = points[0];
        points = mod_points;
        int convex_hull_index = 1;
        int i = 2;
        while (i <= n) {
            // decide if an angle is counterclockwise or not
            // if it is not counterclockwise, do not include it in the
            // convex hull
            while (!FPoint2D.isCounterclockwise(points[convex_hull_index - 1],
                    points[convex_hull_index], points[i])) {
                if (convex_hull_index > 1) {
                    convex_hull_index--;
                } else if (i == n) // all points are collinear
                {
                    break;
                } else {
                    i++;
                }
            }
            convex_hull_index++;
            swap(points, convex_hull_index, i);
            i++;
        }
        return convex_hull_index;
    }

    /**
     *
     * @return the convex hull as an array of points
     */
    public FPoint2D[] getConvexHull() {
        int convex_hull_index = computeConvexHull();
        FPoint2D[] convex_hull_points = new FPoint2D[convex_hull_index];
        System.arraycopy(points, 0, convex_hull_points, 0, convex_hull_index);
        return convex_hull_points;
    }
    
    /**
     *
     * @return the convex hull as a Polygon Roi
     */
    public PolygonRoi getConvexHullP() {
        FPoint2D[] convex_hull_points = getConvexHull();
        return new PolygonRoi(FPoint2D.getXPoints(convex_hull_points), FPoint2D.getYPoints(convex_hull_points), Roi.POLYGON);
    }

    private void swap(FPoint2D[] points, int index1, int index2) {
        assert (index1 < points.length);
        assert (index2 < points.length);

        FPoint2D aux = points[index1];
        points[index1] = points[index2];
        points[index2] = aux;
    }

}
