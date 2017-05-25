package simulation;


import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class HideEvaderPolicy extends MovePolicy {

    public HideEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        ArrayList<Point2D.Double> obstacleMidpoints = getObstacleMidpoints(map);

        return new Move(0, 0, 0);
    }

    private ArrayList<Point2D.Double> getObstacleMidpoints(MapRepresentation map) {
        ArrayList<Polygon> obstacles = map.getObstaclePolygons();
        ArrayList<Point2D.Double> midpoints = new ArrayList<>();

        for (Polygon p : obstacles) {
            ObservableList<Double> singlePoints = p.getPoints();
            ArrayList<Point2D.Double> points = new ArrayList<>();

            for (int i = 0; i < singlePoints.size(); i += 2) {
                points.add(new Point2D.Double(singlePoints.get(i), singlePoints.get(i + 1)));
            }

            if (singlePoints.size() > 2) {
                points.add(new Point2D.Double(singlePoints.get(0), singlePoints.get(1)));
            }

            for (int i = 0; i < points.size() - 1; i++) {
                Point2D.Double pointOne = points.get(i);
                Point2D.Double pointTwo = points.get(i + 1);

                double x = (pointOne.getX() + pointTwo.getX()) / 2;
                double y = (pointOne.getY() + pointTwo.getY()) / 2;

                midpoints.add(new Point2D.Double(x, y));
            }
        }

        return midpoints;
    }

}
