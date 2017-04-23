package simulation;

import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public abstract class MapRepresentation {

    protected Polygon borderPolygon;
    protected ArrayList<Polygon> obstaclePolygons;

    protected ArrayList<Polygon> allPolygons;

    public MapRepresentation(Polygon borderPolygon, ArrayList<Polygon> obstaclePolygons) {
        this.borderPolygon = borderPolygon;
        this.obstaclePolygons = obstaclePolygons;
        allPolygons = new ArrayList<>();
        allPolygons.add(borderPolygon);
        allPolygons.addAll(obstaclePolygons);
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

    public boolean isVisible(Agent lookingAgent, Agent otherAgent) {
        // check whether the second agent is visible from the position of the first agent
        // (given its field of view and the structure of the map)
        return false;
    }

    // methods for the agent to extract the knowledge it has access to

}
