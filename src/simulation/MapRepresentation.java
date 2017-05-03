package simulation;

import additionalOperations.GeometryOperations;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import ui.MapPolygon;

import java.util.ArrayList;

public class MapRepresentation {

    private Polygon borderPolygon;
    private ArrayList<Polygon> obstaclePolygons;
    private ArrayList<Polygon> allPolygons;
    private ArrayList<Line> polygonEdges;

    public MapRepresentation(ArrayList<MapPolygon> map) {
        allPolygons = new ArrayList<>();
        obstaclePolygons = new ArrayList<>();
        for (MapPolygon p : map) {
            if (p.getPoints().size() > 0) {
                allPolygons.add(p.getPolygon());
                obstaclePolygons.add(allPolygons.get(allPolygons.size() - 1));
            }
        }
        borderPolygon = allPolygons.get(0);
        obstaclePolygons.remove(0);

        polygonEdges = new ArrayList<>();
        for (Polygon p : allPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                polygonEdges.add(new Line(p.getPoints().get(i), p.getPoints().get(i + 1), (p.getPoints().get((i + 2) % p.getPoints().size())), (p.getPoints().get((i + 3) % p.getPoints().size()))));
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

    public ArrayList<Line> getPolygonEdges() {
        return polygonEdges;
    }

    public boolean isVisible(double x1, double y1, double x2, double y2) {
        // check whether the second controlledAgents is visible from the position of the first controlledAgents
        // (given its field of view and the structure of the map)
        for (Line l : polygonEdges) {
            if (GeometryOperations.lineIntersect(l, x1, y1, x2, y2)) {
                return false;
            }
        }
        return true;
    }

    // methods for the controlledAgents to extract the knowledge it has access to

}
