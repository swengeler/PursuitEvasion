package simulation;

import AdditionalOperations.GeometryOperations;
import javafx.collections.ObservableList;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public abstract class MapRepresentation {

    protected Polygon borderPolygon;
    protected ArrayList<Polygon> obstaclePolygons;

    protected ArrayList<Polygon> allPolygons;

    protected ArrayList<Line> polygonBorders;

    public MapRepresentation(Polygon borderPolygon, ArrayList<Polygon> obstaclePolygons) {
        this.borderPolygon = borderPolygon;
        this.obstaclePolygons = obstaclePolygons;
        allPolygons = new ArrayList<>();
        allPolygons.add(borderPolygon);
        allPolygons.addAll(obstaclePolygons);
        polygonBorders = new ArrayList<>();
        Line l = null;
        for (Polygon p : allPolygons) {

            for (int i = 0; i < p.getPoints().size(); i += 2) {
                l = new Line(p.getPoints().get(i), p.getPoints().get(i + 1), (p.getPoints().get((i + 2) % p.getPoints().size())), (p.getPoints().get((i + 3) % p.getPoints().size())));
                polygonBorders.add(l);
            }
        }


    }

    public boolean legalPosition(double xPos, double yPos) {
        if (!borderPolygon.contains(xPos, yPos)) {
            return false;
        }
        for (Polygon p : obstaclePolygons) {
            if (p.contains(xPos, yPos)) {
                return false;
            }
        }
        return true;
    }

    public Polygon getBorderPolygon() {
        return borderPolygon;
    }

    public ArrayList<Polygon> getObstaclePolygons() {
        return obstaclePolygons;
    }

    public ArrayList<Polygon> getAllPolygons() {
        return allPolygons;
    }

    public boolean isVisible(double x1, double y1, double x2, double y2) {
        // check whether the second agent is visible from the position of the first agent
        // (given its field of view and the structure of the map)
        for (Line l : polygonBorders) {
            if (GeometryOperations.lineIntersect(l, x1, y1, x2, y2)) {
                return false;
            }
        }
        return true;
    }

    // methods for the agent to extract the knowledge it has access to

}
