package pathfinding;

import javafx.scene.shape.Polygon;
import simulation.MapRepresentation;

import java.util.ArrayList;

public class ShortestPathRoadMap {

    private ArrayList<Vertex> graph;

    public ShortestPathRoadMap(MapRepresentation map) {
        init(map);
    }

    private void init(MapRepresentation map) {
        graph = new ArrayList<>();

        // determine reflex vertices
        double currentAngle, a1, a2, b1, b2;
        boolean inPolygon;
        Polygon currentPolygon = map.getBorderPolygon();
        for (int i = 0; i < currentPolygon.getPoints().size(); i += 2) {
            a1 = currentPolygon.getPoints().get(i) - currentPolygon.getPoints().get((i == 0 ? currentPolygon.getPoints().size() : i) - 2); // x
            a2 = currentPolygon.getPoints().get(i + 1) - currentPolygon.getPoints().get((i == 0 ? currentPolygon.getPoints().size() : i) - 1); // y
            b1 = currentPolygon.getPoints().get(i) - currentPolygon.getPoints().get((i + 2) % currentPolygon.getPoints().size()); // x
            b2 = currentPolygon.getPoints().get(i + 1) - currentPolygon.getPoints().get((i + 3) % currentPolygon.getPoints().size()); // y
            currentAngle = Math.acos((a1 * b1 + a2 * b2) / (Math.sqrt(Math.pow(a1, 2) + Math.pow(a2, 2)) * Math.sqrt(Math.pow(b1, 2) + Math.pow(b2, 2))));
            inPolygon = currentPolygon.contains(currentPolygon.getPoints().get(i) + 0.0001 * a1,  currentPolygon.getPoints().get(i + 1) + 0.0001 * a2);
            if (inPolygon) {
                graph.add(new Vertex(currentPolygon.getPoints().get(i) + 0.0001 * a1, currentPolygon.getPoints().get(i + 1) + + 0.0001 * a2));
            }
        }
        System.out.println("---------");
        for (Polygon p : map.getObstaclePolygons()) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                a1 = p.getPoints().get(i) - p.getPoints().get((i == 0 ? p.getPoints().size() : i) - 2); // x
                a2 = p.getPoints().get(i + 1) - p.getPoints().get((i == 0 ? p.getPoints().size() : i) - 1); // y
                b1 = p.getPoints().get(i) - p.getPoints().get((i + 2) % p.getPoints().size()); // x
                b2 = p.getPoints().get(i + 1) - p.getPoints().get((i + 3) % p.getPoints().size()); // y
                currentAngle = Math.acos((a1 * b1 + a2 * b2) / (Math.sqrt(Math.pow(a1, 2) + Math.pow(a2, 2)) * Math.sqrt(Math.pow(b1, 2) + Math.pow(b2, 2))));
                inPolygon = p.contains(p.getPoints().get(i) + 0.0001 * a1,  p.getPoints().get(i + 1) + + 0.0001 * a2);
                if (!inPolygon) {
                    graph.add(new Vertex(p.getPoints().get(i) + 0.0001 * a1, p.getPoints().get(i + 1) + + 0.0001 * a2));
                    for (int j = 0; j < p.getPoints().size(); j += 2) {
                        if (i != j) {

                        }
                    }
                }
            }
            System.out.println("Polygon");
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                System.out.println(p.getPoints().get(i) + " - " + p.getPoints().get(i + 1));
            }
        }

        for (Vertex v1 : graph) {
            for (Vertex v2 : graph) {

            }
            System.out.println("Vertex at (" + v1.getX() + "|" + v1.getY() + ")");
        }
    }

}
