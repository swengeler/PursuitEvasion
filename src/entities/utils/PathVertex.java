package entities.utils;


import javafx.geometry.Point2D;

public class PathVertex extends Point2D {

    private double estX = -1, estY = -1;

    public PathVertex(double x, double y) {
        super(x, y);
        estX = x;
        estY = y;
    }

    public PathVertex(Point2D point) {
        this(point.getX(), point.getY());
    }

    public PathVertex(double estX, double estY, double realX, double realY) {
        super(realX, realY);
        this.estX = estX;
        this.estY = estY;
    }

    public PathVertex(Point2D estPoint, Point2D realPoint) {
        this(estPoint.getX(), estPoint.getY(), realPoint.getX(), realPoint.getY());
    }

    public PathVertex(double estX, double estY, Point2D realPoint) {
        this(estX, estY, realPoint.getX(), realPoint.getY());
    }

    public PathVertex(Point2D estPoint, double realX, double realY) {
        this(estPoint.getX(), estPoint.getY(), realX, realY);
    }

    public double getEstX() {
        return estX;
    }

    public double getEstY() {
        return estY;
    }

    public void setEstX(double estX) {
        this.estX = estX;
    }

    public void setEstY(double estY) {
        this.estY = estY;
    }

    public double getRealX() {
        return super.getX();
    }

    public double getRealY() {
        return super.getY();
    }

}
