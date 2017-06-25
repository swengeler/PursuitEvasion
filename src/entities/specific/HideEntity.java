package entities.specific;

import additionalOperations.GeometryOperations;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import entities.base.DistributedEntity;
import entities.base.Entity;
import entities.utils.PathLine;
import entities.utils.PlannedPath;
import entities.utils.ShortestPathRoadMap;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import simulation.*;
import ui.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HideEntity extends DistributedEntity {

    private TraversalHandler traversalHandler;
    private PlannedPath currentPath;
    private Point2D ctarget;
    private ShortestPathRoadMap shortestPathMap;
    double cnt = 0;

    private final static int STEP = 50;
    private final static int separationDistance = 100;
    private final static int pursuerDistance = 500;

    ArrayList<PathLine> pathLines;
    int i = 0;

    public HideEntity(MapRepresentation map) {
        super(map);
    }

    @Override
    public void move() {
        boolean debug = true;

        ArrayList<ArrayList<PointData>> allPursuerData = new ArrayList<>();
        Agent evader = controlledAgent;
        Point2D target = null;
        boolean halt = false;

        int numberOfSeparationPursuers = 0;
        double separationDeltaX = 0;
        double separationDeltaY = 0;

        if (cnt % STEP == 0) {
            if (traversalHandler == null) {
                initTree(map);
            }

            if (shortestPathMap == null) {
                shortestPathMap = new ShortestPathRoadMap(map);
            }

            ArrayList<Point2D> polygonMidpoints = getPossiblePolygonPoints(map);
            for (Entity entity : map.getPursuingEntities()) {
                ArrayList<Agent> pursuers = entity.getControlledAgents();
                for (Agent pursuer : pursuers) {
                    double dist = Math.sqrt(Math.pow(pursuer.getXPos() - evader.getXPos(), 2) + Math.pow(pursuer.getYPos() - evader.getYPos(), 2));
                    if (dist <= pursuerDistance) {
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

                    }
                }

            }

            target = getMin(allPursuerData, evader, 1);
        }

        if (allPursuerData.isEmpty() && cnt % STEP == 0) {
            System.out.println("NO PURSUERS NEAR");
            controlledAgent.moveBy(0, 0);
            return;
            //Alternatively, do a random move?
        }

        cnt++;

        for (Entity entity : map.getPursuingEntities()) {
            ArrayList<Agent> pursuers = entity.getControlledAgents();
            for (Agent pursuer : pursuers) {
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


        if (target != null) {
            if (ctarget == null) {
                ctarget = target;
                currentPath = shortestPathMap.getShortestPath(new Point2D(controlledAgent.getXPos(), controlledAgent.getYPos()), target);
                i = 0;
            } else if (!ctarget.equals(target)) {
                ctarget = target;
                currentPath = shortestPathMap.getShortestPath(controlledAgent.getXPos(), controlledAgent.getYPos(), target);
                if (currentPath == null) {
                    ShortestPathRoadMap.DRAW_VISION_LINES = true;
                    currentPath = shortestPathMap.getShortestPath(controlledAgent.getXPos(), controlledAgent.getYPos(), target);
                    ShortestPathRoadMap.DRAW_VISION_LINES = false;
                    System.out.printf("Agent: (%.3f|%.3f)\n", controlledAgent.getXPos(), controlledAgent.getYPos());
                    System.out.println("Legal position: " + shortestPathMap.getMap().legalPosition(controlledAgent.getXPos(), controlledAgent.getYPos()));
                    System.out.printf("Target: (%.3f|%.3f)\n", target.getX(), target.getY());
                    System.out.println("Target legal position: " + shortestPathMap.getMap().legalPosition(target.getX(), target.getY()));
                    AdaptedSimulation.masterPause("What");
                    Simulation.masterPause();
                    //System.exit(-43);
                    Main.pane.getChildren().add(new Circle(controlledAgent.getXPos(), controlledAgent.getYPos(), 7, Color.INDIANRED));
                    Main.pane.getChildren().add(new Circle(target.getX(), target.getY(), 7, Color.BLUE));
                    controlledAgent.moveBy(0, 0);
                    return;
                }
                i = 0;
            }
        }

        pathLines = currentPath.getPathLines();

        if (separationDeltaX != 0 || separationDeltaY != 0) {
            if (map.legalPosition(controlledAgent.getXPos() + separationDeltaX * evader.getSpeed() * 1 / 50, controlledAgent.getYPos() + separationDeltaY * evader.getSpeed() * 1 / 50)) {
                ctarget = null;
                if (!map.legalPosition(evader.getXPos() + separationDeltaX * evader.getSpeed() * 1 / 50, evader.getYPos() + separationDeltaY * evader.getSpeed() * 1 / 50)) {
                    System.out.println("ILLEGAL 1: " + map.getPolygon().distance(new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(evader.getXPos() + separationDeltaX * evader.getSpeed() * 1 / 50, evader.getYPos() + separationDeltaY * evader.getSpeed() * 1 / 50)}), GeometryOperations.factory)));
                }
                cnt = 0;
                controlledAgent.moveBy(separationDeltaX * evader.getSpeed() * 1 / 50, separationDeltaY * evader.getSpeed() * 1 / 50);
                return;
            } else {
                //perhaps stand still here?
                System.out.println("illegal separation");
            }
        }

        if ((i > (pathLines.size() - 1))) {
            controlledAgent.moveBy(0, 0);
            return;
        }

        Move result;
        double length = Math.sqrt(Math.pow(pathLines.get(i).getEndX() - pathLines.get(i).getStartX(), 2) + Math.pow(pathLines.get(i).getEndY() - pathLines.get(i).getStartY(), 2));
        double deltaX = ((pathLines.get(i).getEndX() - pathLines.get(i).getStartX()) / length) * (controlledAgent.getSpeed() / 50);
        double deltaY = ((pathLines.get(i).getEndY() - pathLines.get(i).getStartY()) / length) * (controlledAgent.getSpeed() / 50);

        if (pathLines.get(i).contains(evader.getXPos() + deltaX, evader.getYPos() + deltaY)) {
            result = new Move(deltaX, deltaY, 0);
            if (!map.legalPosition(evader.getXPos() + result.getXDelta(), evader.getYPos() + result.getYDelta())) {
                System.out.println("ILLEGAL 2: " + map.getPolygon().distance(new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(evader.getXPos() + result.getXDelta(), evader.getYPos() + result.getYDelta())}), GeometryOperations.factory)));
                System.out.println("WHAT: " + map.legalPosition(pathLines.get(i).getEndX(), pathLines.get(i).getEndY()));
                System.out.println("WHAT: " + pathLines.get(i).contains(evader.getXPos() + deltaX + 0.49, evader.getYPos() + deltaY + 0.49));
                Main.pane.getChildren().add(pathLines.get(i));
            }
        } else {
            result = new Move(pathLines.get(i).getEndX() - controlledAgent.getXPos(), pathLines.get(i).getEndY() - controlledAgent.getYPos(), 0);
            if (!map.legalPosition(evader.getXPos() + result.getXDelta(), evader.getYPos() + result.getYDelta())) {
                System.out.println("ILLEGAL 3: " + map.getPolygon().distance(new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(evader.getXPos() + result.getXDelta(), evader.getYPos() + result.getYDelta())}), GeometryOperations.factory)));
            }
            i++;
        }

        controlledAgent.moveBy(result.getXDelta(), result.getYDelta());
        return;
    }

    private Point2D getMin(ArrayList<ArrayList<PointData>> midpointData, Agent evader, int mode) {
        if (midpointData.isEmpty()) {
            return null;
        }

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

                PlannedPath shortestPathFromEvader = null;
                shortestPathFromEvader = shortestPathMap.getShortestPath(evader.getXPos(), evader.getYPos(), midpointData.get(0).get(i).getMidpoint());
                if (shortestPathFromEvader == null) {
                    System.out.printf("Midpoint: (%.3f|%.3f)\n", midpointData.get(0).get(i).getMidpoint().getX(), midpointData.get(0).get(i).getMidpoint().getY());
                    System.out.println("Legal position: " + shortestPathMap.getMap().legalPosition(midpointData.get(0).get(i).getMidpoint().getX(), midpointData.get(0).get(i).getMidpoint().getY()));
                    System.out.printf("Evader: (%.3f|%.3f)\n", evader.getXPos(), evader.getYPos());
                    System.out.println("Evader legal position: " + shortestPathMap.getMap().legalPosition(evader.getXPos(), evader.getYPos()));
                    AdaptedSimulation.masterPause("What");
                    Simulation.masterPause();
                    Main.pane.getChildren().add(new Circle(midpointData.get(0).get(i).getMidpoint().getX(), midpointData.get(0).get(i).getMidpoint().getY(), 7, Color.INDIANRED));
                    //System.exit(-43);
                    return new Point2D(0, 0);
                }

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
            //traversalHandler.map = map;
        } catch (DelaunayError e) {
            e.printStackTrace();
        }
    }

}
