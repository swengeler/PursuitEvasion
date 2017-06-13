package simulation;

import javafx.geometry.Point2D;

public class PointData {

    private Point2D midpoint;
    private double distance;
    private int numOfVertices;

    public PointData(Point2D midpoint, double distance, int numOfVertices) {
        this.midpoint = midpoint;
        this.distance = distance;
        this.numOfVertices = numOfVertices;
    }

    public Point2D getMidpoint() {
        return midpoint;
    }

    public double getDistance() {
        return distance;
    }

    public int getNumOfVertices() {
        return numOfVertices;
    }

}
