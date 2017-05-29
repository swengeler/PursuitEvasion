package pathfinding;


import javafx.geometry.Point2D;

import java.util.ArrayList;

public class PathVertex {

    private double x, y;

    private ArrayList<Edge> edges;

    public PathVertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public PathVertex(Point2D point) {
        this.x = point.getX();
        this.y = point.getY();

    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

}
