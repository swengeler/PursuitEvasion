package simulation;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import pathfinding.ShortestPathRoadMap;

import java.util.*;

public class HideEvaderPolicy extends MovePolicy {

    //only recompute every xx timestep

    private TraversalHandler traversalHandler;
    private PlannedPath currentPath;
    private Point2D ctarget;
    private ShortestPathRoadMap shortestPathMap;

    private final static int separationDistance = 100;
    ArrayList<Line> pathLines;
    int i = 0;

    public HideEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {

        if (traversalHandler == null) {
            initTree(map);
        }

        shortestPathMap = new ShortestPathRoadMap(map);
        ArrayList<ArrayList<PointData>> allPursuerData = new ArrayList<>();

        ArrayList<Point2D> polygonMidpoints = getPossiblePolygonPoints(map);

        Agent evader = getSingleAgent();
        Point2D target = null;

        int numberOfSeparationPursuers = 0;
        double separationDeltaX = 0;
        double separationDeltaY = 0;

        for (Agent pursuer : agents) {
            if (pursuer.isPursuer()) {

                ArrayList<PointData> pursuerPointData = new ArrayList<>();
                for (Point2D midpoint : polygonMidpoints) {

                    PlannedPath shortestPathFromPursuer = shortestPathMap.getShortestPath(new Point2D(pursuer.getXPos(), pursuer.getYPos()), midpoint);
                    double midpointDistance = shortestPathFromPursuer.getTotalLength();
                    int numberOfVertices = shortestPathFromPursuer.pathLength();

                    PointData pd = new PointData(midpoint, midpointDistance, numberOfVertices);
                    pursuerPointData.add(pd);
                    //System.out.println("dist: " + midpointDistance);

                }

                allPursuerData.add(pursuerPointData);

                double dist = Math.sqrt(Math.pow(pursuer.getXPos() - evader.getXPos(), 2) + Math.pow(pursuer.getYPos() - evader.getYPos(), 2));
                if (dist <= separationDistance) {
                    separationDeltaX += (pursuer.getXPos() - evader.getXPos());
                    separationDeltaY += (pursuer.getYPos() - evader.getYPos());
                    numberOfSeparationPursuers++;
                }

            }

        }

        if (numberOfSeparationPursuers != 0) {
            //if there are seperation pursuers, do further calculations (normalizing, reversing (180 degrees))
            System.out.println("should separate");

            separationDeltaX = -separationDeltaX / numberOfSeparationPursuers;
            separationDeltaY = -separationDeltaY / numberOfSeparationPursuers;

            double dlength = Math.sqrt(Math.pow(separationDeltaX, 2) + Math.pow(separationDeltaY, 2));
            separationDeltaX /= dlength;
            separationDeltaY /= dlength;
        }

        target = getMin(allPursuerData, evader, 4);

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

        pathLines = currentPath.getPathLines();

        if (separationDeltaX != 0 || separationDeltaY != 0) {
            if (map.legalPosition(getSingleAgent().getXPos() + separationDeltaX * evader.getSpeed() * 1 / 50, getSingleAgent().getYPos() + separationDeltaY * evader.getSpeed() * 1 / 50)) {
                ctarget = null;
                return new Move(separationDeltaX * evader.getSpeed() * 1 / 50, separationDeltaY * evader.getSpeed() * 1 / 50, 0);
            } else {
                //perhaps stand still here?
                System.out.println("illegal separation");
            }
        }

        if ((i > (pathLines.size() - 1))) {
            return new Move(0, 0, 0);
        }

        Move result;
        double length = Math.sqrt(Math.pow(pathLines.get(i).getEndX() - pathLines.get(i).getStartX(), 2) + Math.pow(pathLines.get(i).getEndY() - pathLines.get(i).getStartY(), 2));
        double deltaX = (pathLines.get(i).getEndX() - pathLines.get(i).getStartX()) / length * getSingleAgent().getSpeed() / 50;
        double deltaY = (pathLines.get(i).getEndY() - pathLines.get(i).getStartY()) / length * getSingleAgent().getSpeed() / 50;

        if (pathLines.get(i).contains(evader.getXPos() + deltaX, evader.getYPos() + deltaY)) {
            result = new Move(deltaX, deltaY, 0);
        } else {
            result = new Move(pathLines.get(i).getEndX() - getSingleAgent().getXPos(), pathLines.get(i).getEndY() - getSingleAgent().getYPos(), 0);
            i++;
        }

        return result;
    }

    private Point2D getMin(ArrayList<ArrayList<PointData>> midpointData, Agent evader, int mode) {
        Point2D target = null;
        double euclideanDistance = Double.MIN_VALUE;
        int numberOfVertices = Integer.MIN_VALUE;
        double euclideanDistanceEvader = Double.MAX_VALUE;
        int numberOfVerticesEvader = Integer.MAX_VALUE;

        int numOfPursuers = midpointData.size();
        int numOfMidpoints = midpointData.get(0).size();

        final int THRESHOLD = 1 * numOfPursuers;
        ArrayList<Point2D> possibleTargets = new ArrayList<>();
        Random r = new Random();

        for (int i = 0; i < numOfMidpoints; i++) {
            int tmpNumberOfVertices = 0;
            double tmpEuclideanDistance = 0;

            for (int j = 0; j < numOfPursuers; j++) {
                int numOfVerts = midpointData.get(j).get(i).getNumOfVertices();
                double distance = midpointData.get(j).get(i).getDistance();
                tmpNumberOfVertices += numOfVerts;
                tmpEuclideanDistance += distance;
            }

            if (mode != 4) {

                PlannedPath shortestPathFromEvader = shortestPathMap.getShortestPath(new Point2D(evader.getXPos(), evader.getYPos()), midpointData.get(0).get(i).getMidpoint());

                if (tmpNumberOfVertices == numberOfVertices) {
                    if (mode == 1) {
                        if (tmpEuclideanDistance > euclideanDistance) {
                            euclideanDistance = tmpEuclideanDistance;
                            target = midpointData.get(0).get(i).getMidpoint();
                        }
                    } else if (mode == 2) {
                        if (shortestPathFromEvader.getTotalLength() < euclideanDistanceEvader) {
                            euclideanDistanceEvader = shortestPathFromEvader.getTotalLength();
                            target = midpointData.get(0).get(i).getMidpoint();
                            euclideanDistance = tmpEuclideanDistance;
                        } else if (shortestPathFromEvader.getTotalLength() == euclideanDistanceEvader) {
                            if (tmpEuclideanDistance > euclideanDistance) {
                                euclideanDistance = tmpEuclideanDistance;
                                target = midpointData.get(0).get(i).getMidpoint();
                            }
                        }
                    } else if (mode == 3) {
                        if (shortestPathFromEvader.pathLength() < numberOfVerticesEvader) {
                            numberOfVerticesEvader = shortestPathFromEvader.pathLength();
                            target = midpointData.get(0).get(i).getMidpoint();
                            euclideanDistance = tmpEuclideanDistance;
                        } else if (shortestPathFromEvader.pathLength() == numberOfVerticesEvader) {
                            if (tmpEuclideanDistance > euclideanDistance) {
                                euclideanDistance = tmpEuclideanDistance;
                                target = midpointData.get(0).get(i).getMidpoint();
                            }
                        }
                    }

                } else if (tmpNumberOfVertices > numberOfVertices) {
                    numberOfVertices = tmpNumberOfVertices;
                    euclideanDistanceEvader = shortestPathFromEvader.getTotalLength();
                    euclideanDistance = tmpEuclideanDistance;
                    target = midpointData.get(0).get(i).getMidpoint();
                }

            } else {
                if (tmpNumberOfVertices >= THRESHOLD) {
                    possibleTargets.add(midpointData.get(0).get(i).getMidpoint());
                }
            }

        }

        if (mode == 4) {
            if (possibleTargets.isEmpty()) {
                System.out.println("Threshold not satisfied! Returning default");
                return getMin(midpointData, evader, 1);
            }
            int rand = r.nextInt(possibleTargets.size());
            target = possibleTargets.get(rand);
        }

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
