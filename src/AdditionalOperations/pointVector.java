package AdditionalOperations;

import javafx.geometry.Point2D;

/**
 * Created by robin on 20.04.2017.
 */
public class pointVector {


    private Point2D origin, destination;
    private double x, y;

    public pointVector(Point2D origin, Point2D destination) {
        this.origin = origin;
        this.destination = destination;
        this.x = destination.getX() - origin.getX();
        this.y = destination.getY() - origin.getY();
    }

    public pointVector(Point2D p1) {
        new pointVector(p1.getX(), p1.getY());
    }

    public pointVector(double x, double y) {
        this.origin = new Point2D(0,0);
        this.destination = new Point2D(x, y);
        this.x = x;
        this.y = y;
    }

    public pointVector() {
        this.origin = null;
        this.destination = null;
        this.x = 0;
        this.y = 0;
    }


    public void setOrigin(Point2D origin) {
        this.origin = origin;
        update();
    }

    public void setDestination(Point2D destination) {
        this.destination = destination;
        update();
    }

    public void update()    {
        this.x = destination.getX() - origin.getX();
        this.y = destination.getY() - origin.getY();
    }

    public Point2D getOrigin() {
        return origin;
    }

    public Point2D getDestination() {
        return destination;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
