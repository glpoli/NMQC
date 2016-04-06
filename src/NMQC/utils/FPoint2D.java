package NMQC.utils;

public class FPoint2D {

    public double X, Y;

    public FPoint2D(double xi, double yi) {
        X = xi;
        Y = yi;
    }

    public void assign(double xi, double yi) {
        X = xi;
        Y = yi;
    }

    public void assign(FPoint2D p1) {
        X = p1.X;
        Y = p1.Y;
    }
}
