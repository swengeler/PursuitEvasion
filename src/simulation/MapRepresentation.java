package simulation;

import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public abstract class MapRepresentation {

    protected Polygon borderPolygon;
    protected ArrayList<Polygon> obstaclePolygons;

    public MapRepresentation(Polygon borderPolygon, ArrayList<Polygon> obstaclePolygons) {
        this.borderPolygon = borderPolygon;
        this.obstaclePolygons = obstaclePolygons;
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

    // methods for the agent to extract the knowledge it has access to

}
