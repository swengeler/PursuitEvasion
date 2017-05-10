package pathfinding;



import java.util.ArrayList;
import javafx.geometry.Point2D;

public class PathVertex {

    private double x, y;

    private ArrayList<Edge> edges;

    public PathVertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public PathVertex(Point2D point)  {
        this.x = point.getX();
        this.y = point.getY();

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
