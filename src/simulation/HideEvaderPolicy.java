package simulation;


import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Pair;
import org.javatuples.Triplet;
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

        System.out.println(polygonMidpoints.size());

        Agent evader = getSingleAgent();
        ArrayList<Triplet<Point2D, Double, Integer>> midpointData = new ArrayList<>();
        Point2D target = null;

        for (Point2D midpoint: polygonMidpoints) {

            double midpointDistance = 0;
            int numberOfVertices = 0;

            for (Agent pursuer: agents) {

                if (pursuer.isPursuer()) {

                    PlannedPath shortestPath = shortestPathMap.getShortestPath(new Point2D(pursuer.getXPos(), pursuer.getYPos()), midpoint);
                    midpointDistance += shortestPath.getTotalLength();
                    numberOfVertices += shortestPath.pathLength();

                    System.out.println("dist: " + midpointDistance);

                }

            }

            midpointData.add(new Triplet<Point2D, Double, Integer>(midpoint, midpointDistance, numberOfVertices));

        }

        target = getMin(midpointData, 1);
        double dx = 0;
        double dy = 0;

        if (target != null) {
            PlannedPath pathToTarget = shortestPathMap.getShortestPath(new Point2D(getSingleAgent().getXPos(), getSingleAgent().getYPos()), target);
            dx = pathToTarget.getPathLines().get(0).getEndX();
            dy = pathToTarget.getPathLines().get(0).getEndY();

            System.out.println("SHOULD MOVE TO " + dx + ":" + dy);
        }

        if (map.legalPosition(dx * evader.getSpeed() * 1 / 250, dy * evader.getSpeed() * 1 / 250)) {
            return new Move(dx * evader.getSpeed() * 1 / 250, dy * evader.getSpeed() * 1 / 250, 0);
        }  else {
            System.out.println("MOVE OUT OF BOUNDS: STAND STILL");
            return new Move(0, 0, 0);
        }

    }

    private Point2D getMin(ArrayList<Triplet<Point2D, Double, Integer>> midpointData, int mode) {
        Point2D target = null;
        double euclideanDistance = Double.MIN_VALUE;
        int numberOfVertices = Integer.MIN_VALUE;
        String s = "";

        for (Triplet<Point2D, Double, Integer> triplet: midpointData) {

            if (mode == 0) {
                if (triplet.getValue1() > euclideanDistance) {
                    euclideanDistance = triplet.getValue1();
                    target = triplet.getValue0();
                    s = "EUCLIDEAN DIST";
                }
            } else if (mode == 1) {
                if (triplet.getValue2() > numberOfVertices)  {
                    numberOfVertices = triplet.getValue2();
                    target = triplet.getValue0();
                    s = "NUM OF VERTS";
                }
            }

        }

        System.out.println("BEST POINT IS " + target.getX() + ":" + target.getY() + " | " + s);
        return target;
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
