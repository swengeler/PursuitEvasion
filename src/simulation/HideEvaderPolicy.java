package simulation;


import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import pathfinding.ShortestPathRoadMap;

import java.util.ArrayList;
import java.util.List;

public class HideEvaderPolicy extends MovePolicy {

    public static boolean stopDont = false;

    private TraversalHandler traversalHandler;
    private PlannedPath currentPath;
    private Point2D ctarget;

    ArrayList<Line> pathLines;
    int i = 0;

    public HideEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        //boolean pathChanged = false;

        if (traversalHandler == null) {
            initTree(map);
        }

        ArrayList<Point2D> polygonMidpoints = getPossiblePolygonPoints(map);
        ShortestPathRoadMap shortestPathMap = new ShortestPathRoadMap(map);

        Agent evader = getSingleAgent();
        ArrayList<Triplet<Point2D, Double, Integer>> midpointData = new ArrayList<>();
        Point2D target = null;

        for (Point2D midpoint : polygonMidpoints) {

            double midpointDistance = 0;
            int numberOfVertices = 0;

            for (Agent pursuer : agents) {

                if (pursuer.isPursuer() && !stopDont) {
                    PlannedPath shortestPath = shortestPathMap.getShortestPath(new Point2D(pursuer.getXPos(), pursuer.getYPos()), midpoint);
                    midpointDistance += shortestPath.getTotalLength();
                    numberOfVertices += shortestPath.pathLength();

                    System.out.println("dist: " + midpointDistance);

                }

            }

            midpointData.add(new Triplet<Point2D, Double, Integer>(midpoint, midpointDistance, numberOfVertices));

        }

        target = getMin(midpointData, 2);

        if (target != null) {
            if (ctarget == null) {
                ctarget = target;
                currentPath = shortestPathMap.getShortestPath(new Point2D(getSingleAgent().getXPos(), getSingleAgent().getYPos()), target);
                i = 0;
            } else if (!ctarget.equals(target)) {
                ctarget = target;
                currentPath = shortestPathMap.getShortestPath(new Point2D(getSingleAgent().getXPos(), getSingleAgent().getYPos()), target);
                i = 0;
            }
        }

        // || traversalHandler.getNodeIndex(evader.getXPos(), evader.getYPos()) == currentPath.getEndIndex()

        pathLines = currentPath.getPathLines();

        if ((i > (pathLines.size() - 1))) {
            return new Move(0, 0, 0);
        }

        Move result;
        double length = Math.sqrt(Math.pow(pathLines.get(i).getEndX() - pathLines.get(i).getStartX(), 2) + Math.pow(pathLines.get(i).getEndY() - pathLines.get(i).getStartY(), 2));
        double deltaX = (pathLines.get(i).getEndX() - pathLines.get(i).getStartX()) / length * getSingleAgent().getSpeed() / 50;
        double deltaY = (pathLines.get(i).getEndY() - pathLines.get(i).getStartY()) / length * getSingleAgent().getSpeed() / 50;

        if (pathLines.get(i).contains(evader.getXPos() + deltaX, evader.getYPos() + deltaY)) {
            // move along line
            result = new Move(deltaX, deltaY, 0);
        } else {
            result = new Move(pathLines.get(i).getEndX() - getSingleAgent().getXPos(), pathLines.get(i).getEndY() - getSingleAgent().getYPos(), 0);
            i++;
        }

        return result;
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
            } else if (mode == 2) {

                //idea:
                //num of verts is more important, but if equal eucl distance determines

                if (triplet.getValue2() > numberOfVertices) {
                    numberOfVertices = triplet.getValue2();
                    target = triplet.getValue0();
                    s = "NUM OF VERTS + DIST BKUP";
                } else if (triplet.getValue2() == numberOfVertices) {
                    if (triplet.getValue1() > euclideanDistance) {
                        euclideanDistance = triplet.getValue1();
                        target = triplet.getValue0();
                    }
                }
            }

        }

        System.out.println("BEST POINT IS " + target.getX() + ":" + target.getY() + " | " + s);
        return target;
    }

    private ArrayList<Point2D> getPossiblePolygonPoints(MapRepresentation map) {
        ArrayList<Point2D> polygonPoints = new ArrayList<>();

        ObservableList<Double> singlePoints = map.getBorderPolygon().getPoints();
        ArrayList<Point2D> points = new ArrayList<>();

        for (int i = 0; i < singlePoints.size(); i += 2) {
            polygonPoints.add(new Point2D(singlePoints.get(i), singlePoints.get(i + 1)));
        }

        for (Polygon p : map.getObstaclePolygons()) {
            singlePoints = p.getPoints();
            points.clear();

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

                polygonPoints.add(new Point2D(x, y));
            }
        }

        return polygonPoints;
    }

    private void initTree(MapRepresentation map) {
        try {
            ArrayList<DEdge> constraintEdges = new ArrayList<>();
            ArrayList<Line> polygonEdges = map.getPolygonEdges();
            ArrayList<Polygon> polygons = map.getAllPolygons();
            for (Line l : polygonEdges) {
                constraintEdges.add(new DEdge(new DPoint(l.getStartX(), l.getStartY(), 0), new DPoint(l.getEndX(), l.getEndY(), 0)));
            }

            ConstrainedMesh mesh = new ConstrainedMesh();
            mesh.setConstraintEdges(constraintEdges);
            mesh.processDelaunay();
            List<DTriangle> triangles = mesh.getTriangleList();
            List<DTriangle> includedTriangles = new ArrayList<>();

            for (DTriangle dt : triangles) {
                // check if triangle in polygon
                double centerX = dt.getBarycenter().getX();
                double centerY = dt.getBarycenter().getY();
                boolean inPolygon = true;
                if (!polygons.get(0).contains(centerX, centerY)) {
                    inPolygon = false;
                }
                for (int i = 1; inPolygon && i < polygons.size() - 1; i++) {
                    if (polygons.get(i).contains(centerX, centerY)) {
                        inPolygon = false;
                    }
                }
                if (inPolygon) {
                    includedTriangles.add(dt);
                }
            }

            traversalHandler = new TraversalHandler((ArrayList<DTriangle>) includedTriangles);
            traversalHandler.shortestPathRoadMap = new ShortestPathRoadMap(map);
            traversalHandler.map = map;
        } catch (DelaunayError e) {
            e.printStackTrace();
        }
    }

}
