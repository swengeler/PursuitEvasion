package simulation;


import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import pathfinding.ShortestPathRoadMap;

import java.util.ArrayList;

public class HideEvaderPolicy extends MovePolicy {

    public HideEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        ArrayList<Point2D> polygonMidpoints = getPolygonMidpoints(map);
        ShortestPathRoadMap shortestPathMap = new ShortestPathRoadMap(map);

        Agent evader = getSingleAgent();
        //double maxDistance = Double.MIN_VALUE;
        //int maxVertices = Integer.MIN_VALUE;
        Point2D target = null;

        for (Point2D midpoint: polygonMidpoints) {

            double midpointDistance = 0;
            int numberOfVertices = 0;

            for (Agent pursuer: agents) {

                if (pursuer.isPursuer()) {

                    double dist = Math.sqrt(Math.pow(pursuer.getXPos() - midpoint.getX(), 2) + Math.pow(pursuer.getYPos() - midpoint.getY(), 2));
                    midpointDistance += dist;

                    PlannedPath shortestPath = shortestPathMap.getShortestPath(new Point2D(pursuer.getXPos(), pursuer.getYPos()), midpoint);
                    int shortestPathSize = 0;
                    numberOfVertices += shortestPathSize;
                    //size?

                }

            }

        }

        if (target != null) {
            return new Move(target.getX() * evader.getSpeed() * 1/250, target.getY() * evader.getSpeed() * 1/250, 0);
        } else {
            return new Move(0, 0, 0);
        }

    }

    private ArrayList<Point2D> getPolygonMidpoints(MapRepresentation map) {
        ArrayList<Polygon> polygons = map.getAllPolygons();
        ArrayList<Point2D> midpoints = new ArrayList<>();

        for (Polygon p : polygons) {
            ObservableList<Double> singlePoints = p.getPoints();
            ArrayList<Point2D> points = new ArrayList<>();

            for (int i = 0; i < singlePoints.size(); i += 2) {
                points.add(new Point2D(singlePoints.get(i), singlePoints.get(i + 1)));
            }

            if (singlePoints.size() > 2) {
                points.add(new Point2D(singlePoints.get(0), singlePoints.get(1)));
            }

            for (int i = 0; i < points.size() - 1; i++) {
                Point2D pointOne = points.get(i);
                Point2D pointTwo = points.get(i + 1);

                double x = (pointOne.getX() + pointTwo.getX()) / 2;
                double y = (pointOne.getY() + pointTwo.getY()) / 2;

                midpoints.add(new Point2D(x, y));
            }
        }

        return midpoints;
    }

}
