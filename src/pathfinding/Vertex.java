package pathfinding;

import java.util.ArrayList;

public class Vertex {

    private double x, y;

    private ArrayList<Edge> edges;

    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

}
