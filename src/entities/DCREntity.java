package entities;

import additionalOperations.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import pathfinding.ShortestPathRoadMap;
import simulation.*;
import ui.Main;

import java.util.*;

/**
 * DCR = Divide and Conquer, Randomized
 */
public class DCREntity extends CentralisedEntity {

    private static final boolean GUARD_TEST = false;
    private static final boolean CONSTANT_TARGET_TEST = true;

    private enum Stage {
        CATCHER_TO_SEARCHER, FIND_TARGET, INIT_FIND_TARGET, FOLLOW_TARGET, WAIT_LINE_CROSSING
    }

    private TraversalHandler traversalHandler;
    private ArrayList<ArrayList<Line>> componentBoundaryLines;
    private ArrayList<ArrayList<DEdge>> componentBoundaryEdges;
    private ArrayList<Shape> componentBoundaryShapes;
    private ArrayList<DEdge> separatingEdges;
    private ArrayList<Line> separatingLines;

    private Agent target;
    private Point2D origin, pseudoBlockingVertex, lastPointVisible;
    private boolean pocketCounterClockwise;
    private Stage currentStage;

    private Agent searcher, catcher;
    private PlannedPath currentSearcherPath, currentCatcherPath;
    private ArrayList<Line> pathLines;
    private int searcherPathLineCounter, catcherPathLineCounter;

    private ArrayList<SquareGuardManager> squareGuardManagers;
    private ArrayList<ArrayList<DTriangle>> gSqrIntersectingTriangles;
    private ArrayList<Agent> guards;
    private ArrayList<PlannedPath> initGuardPaths;
    private ArrayList<Integer> guardPathLineCounters;
    private ArrayList<Line> guardPathLines;
    private boolean guardsPositioned;

    private ShortestPathRoadMap specialShortestPathRoadMap;
    private SquareGuardManager currentGuardedSquare;
    private boolean inGuardedSquareOverNonSeparating, inGuardedSquareOverSeparating, spottedOnce, spottedOutsideGuardingSquare;

    private Group catchGraphics;
    private Group guardGraphics;

    public DCREntity(MapRepresentation map) {
        super(map);
        computeRequirements();
        catchGraphics = new Group();
        guardGraphics = new Group();
        Main.pane.getChildren().addAll(catchGraphics, guardGraphics);
    }

    @Override
    public void move() {
        // initialising some local variables
        double length, deltaX, deltaY;

        // check if any agent is caught
        if (GUARD_TEST) {
            if (target == null) {
                for (Entity e : map.getEvadingEntities()) {
                    if (e.isActive()) {
                        for (Agent a1 : e.getControlledAgents()) {
                            if (a1.isActive()) {
                                for (Agent a2 : availableAgents) {
                                    if (map.isVisible(a1, a2)) {
                                        //a1.setActive(false);
                                        target = a1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // it is assumed that the required number of agents is provided by the GUI
        // could change this to throw an exception if it is not the case, giving the user a warning
        if (searcher == null) {
            assignTasks();
        }

        if (target == null) {
            outer:
            for (Entity e : map.getEvadingEntities()) {
                if (e.isActive()) {
                    for (Agent a : e.getControlledAgents()) {
                        if (a.isActive()) {
                            target = a;
                            for (SquareGuardManager gm : squareGuardManagers) {
                                gm.initTargetPosition(target);
                            }
                            break outer;
                        }
                    }
                }
            }
        }

        // ******************************************************************************************************************************** //
        // Guard movement
        // ******************************************************************************************************************************** //

        if (!guardsPositioned()) {
            // let the guards move along their respective paths
            for (int i = 0; i < guards.size(); i++) {
                if (guards.get(i).getXPos() != initGuardPaths.get(i).getEndX() || guards.get(i).getYPos() != initGuardPaths.get(i).getEndY()) {
                    // this guard is not at its final destination and will be moved along the path
                    guardPathLines = initGuardPaths.get(i).getPathLines();

                    length = Math.sqrt(Math.pow(guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guardPathLines.get(guardPathLineCounters.get(i)).getStartX(), 2) + Math.pow(guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guardPathLines.get(guardPathLineCounters.get(i)).getStartY(), 2));
                    deltaX = (guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guardPathLines.get(guardPathLineCounters.get(i)).getStartX()) / length * guards.get(i).getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                    deltaY = (guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guardPathLines.get(guardPathLineCounters.get(i)).getStartY()) / length * guards.get(i).getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;

                    if (guardPathLines.get(guardPathLineCounters.get(i)).contains(guards.get(i).getXPos() + deltaX, guards.get(i).getYPos() + deltaY)) {
                        // move along line
                        guards.get(i).moveBy(deltaX, deltaY);
                    } else {
                        // move to end of line
                        guards.get(i).moveBy(guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guards.get(i).getXPos(), guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guards.get(i).getYPos());
                        guardPathLineCounters.set(i, guardPathLineCounters.get(i) + 1);
                    }
                }
            }
        } else if (target != null) {
            //for (Agent a : availableAgents) {
            //    if (map.isVisible(target, a)) {
            for (SquareGuardManager gm : squareGuardManagers) {
                // maybe cheat and update this regardless
                gm.updateTargetPosition(target);
            }
            //    }
            //}
            if (spottedOnce) {
                if ((inGuardedSquareOverNonSeparating || inGuardedSquareOverSeparating) && !currentGuardedSquare.inGuardedSquare(target.getXPos(), target.getYPos())) {
                    System.out.println("Exited square");
                    specialShortestPathRoadMap = null;
                    currentGuardedSquare = null;
                    inGuardedSquareOverNonSeparating = false;
                    inGuardedSquareOverSeparating = false;
                } else /*if (map.isVisible(catcher, target))*/ {
                    for (SquareGuardManager gm : squareGuardManagers) {
                        if (gm.crossedNonSeparatingLine()) {
                            currentGuardedSquare = gm;
                            inGuardedSquareOverNonSeparating = true;
                            inGuardedSquareOverSeparating = false;
                            System.out.println("Entered square over non-separating line");
                            break;
                        } else if (gm.crossedSeparatingLine()) {
                            currentGuardedSquare = gm;
                            inGuardedSquareOverNonSeparating = false;
                            inGuardedSquareOverSeparating = true;
                            System.out.println("Entered square over separating line");
                            break;
                        }
                    }
                }
            }
        }


        // ******************************************************************************************************************************** //


        // ******************************************************************************************************************************** //
        // Searcher and catcher movement
        // ******************************************************************************************************************************** //

        if (!GUARD_TEST) {
            //System.out.println(currentStage);
            if (currentStage == Stage.CATCHER_TO_SEARCHER) {
                // only move catcher
                pathLines = currentCatcherPath.getPathLines();
                length = Math.sqrt(Math.pow(pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY(), 2));
                deltaX = (pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                deltaY = (pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;

                if (pathLines.get(catcherPathLineCounter).contains(catcher.getXPos() + deltaX, catcher.getYPos() + deltaY)) {
                    // move along line
                    catcher.moveBy(deltaX, deltaY);
                } else {
                    // move to end of line
                    // TODO: instead take a "shortcut" to the next line
                    catcher.moveBy(pathLines.get(catcherPathLineCounter).getEndX() - catcher.getXPos(), pathLines.get(catcherPathLineCounter).getEndY() - catcher.getYPos());
                    catcherPathLineCounter++;
                }

                // check if searcher position reached and the next stage can begin
                if (catcher.getXPos() == searcher.getXPos() && catcher.getYPos() == searcher.getYPos()) {
                    currentStage = Stage.INIT_FIND_TARGET;
                }
            } else if (currentStage == Stage.INIT_FIND_TARGET) {
                // move searcher and catcher together (for its assumed they have the same speed
                if (currentSearcherPath == null) {
                    try {
                        currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                        currentCatcherPath = currentSearcherPath;
                        searcherPathLineCounter = 0;
                        catcherPathLineCounter = 0;
                    } catch (DelaunayError e) {
                        e.printStackTrace();
                    }
                }

                if (traversalHandler.getNodeIndex(searcher.getXPos(), searcher.getYPos()) == currentSearcherPath.getEndIndex()) {
                    // end of path reached, compute new path
                    try {
                        currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                        currentCatcherPath = currentSearcherPath;
                    } catch (DelaunayError e) {
                        e.printStackTrace();
                    }
                    searcherPathLineCounter = 0;
                    catcherPathLineCounter = 0;
                }

                // move searcher and catcher using same paths
                pathLines = currentSearcherPath.getPathLines();
                length = Math.sqrt(Math.pow(pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY(), 2));
                deltaX = (pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                deltaY = (pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                if (pathLines.get(searcherPathLineCounter).contains(searcher.getXPos() + deltaX, searcher.getYPos() + deltaY)) {
                    // move along line
                    searcher.moveBy(deltaX, deltaY);
                } else {
                    // move to end of line
                    searcher.moveBy(pathLines.get(searcherPathLineCounter).getEndX() - searcher.getXPos(), pathLines.get(searcherPathLineCounter).getEndY() - searcher.getYPos());
                    searcherPathLineCounter++;
                }

                // move catcher using same path
                pathLines = currentCatcherPath.getPathLines();
                length = Math.sqrt(Math.pow(pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY(), 2));
                deltaX = (pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX()) / length * catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                deltaY = (pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY()) / length * catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                if (pathLines.get(catcherPathLineCounter).contains(catcher.getXPos() + deltaX, catcher.getYPos() + deltaY)) {
                    // move along line
                    catcher.moveBy(deltaX, deltaY);
                } else {
                    // move to end of line
                    catcher.moveBy(pathLines.get(catcherPathLineCounter).getEndX() - catcher.getXPos(), pathLines.get(catcherPathLineCounter).getEndY() - catcher.getYPos());
                    catcherPathLineCounter++;
                }

                if (CONSTANT_TARGET_TEST) {
                    // check whether target is visible
                    if (target != null) {
                        if (target.isActive() && map.isVisible(target, searcher) && !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos(), separatingLines)) {
                            System.out.println("Target found");
                            spottedOnce = true;
                            boolean temp = false;
                            for (SquareGuardManager gm : squareGuardManagers) {
                                if (gm.inGuardedSquare(target.getXPos(), target.getYPos())) {
                                    temp = true;
                                    break;
                                }
                            }
                            if (!temp) {
                                spottedOutsideGuardingSquare = true;
                            }
                            origin = new Point2D(catcher.getXPos(), catcher.getYPos());
                            Label l = new Label("Origin");
                            l.setTranslateX(origin.getX() + 5);
                            l.setTranslateY(origin.getY() + 5);
                            Main.pane.getChildren().addAll(new Circle(origin.getX(), origin.getY(), 7, Color.GRAY), l);
                            currentStage = Stage.FOLLOW_TARGET;
                        }
                    }
                } else {
                    // check whether an evader is visible
                    for (int i = 0; currentStage == Stage.INIT_FIND_TARGET && i < map.getEvadingEntities().size(); i++) {
                        if (map.getEvadingEntities().get(i).isActive()) {
                            for (int j = 0; currentStage == Stage.INIT_FIND_TARGET && j < map.getEvadingEntities().get(i).getControlledAgents().size(); j++) {
                                if (map.getEvadingEntities().get(i).getControlledAgents().get(j).isActive() && map.isVisible(map.getEvadingEntities().get(i).getControlledAgents().get(j), searcher) && !GeometryOperations.lineIntersectSeparatingLines(map.getEvadingEntities().get(i).getControlledAgents().get(j).getXPos(), map.getEvadingEntities().get(i).getControlledAgents().get(j).getYPos(), searcher.getXPos(), searcher.getYPos(), separatingLines)) {
                                    target = map.getEvadingEntities().get(i).getControlledAgents().get(j);
                                    System.out.println("Target found");
                                    for (SquareGuardManager gm : squareGuardManagers) {
                                        gm.initTargetPosition(target);
                                    }
                                    origin = new Point2D(catcher.getXPos(), catcher.getYPos());
                                    Label l = new Label("Origin");
                                    l.setTranslateX(origin.getX() + 5);
                                    l.setTranslateY(origin.getY() + 5);
                                    Main.pane.getChildren().addAll(new Circle(origin.getX(), origin.getY(), 7, Color.GRAY), l);
                                    currentStage = Stage.FOLLOW_TARGET;
                                }
                            }
                        }
                    }
                }

            } else if (currentStage == Stage.FOLLOW_TARGET) {
                // behaviour changes based on visibility
                // if the target is still visible, first check whether it can simply be caught
                length = Math.sqrt(Math.pow(target.getXPos() - catcher.getXPos(), 2) + Math.pow(target.getYPos() - catcher.getYPos(), 2));
                // TODO: (also elsewhere) need check whether evader is behind a separating line
                // if it is, then the catcher should simply move towards the separating line to resume capture after the evader has crossed the line again
                // this should actually already happen once the searcher (or catcher) sees that the evader has crossed the line
                if (map.isVisible(target, catcher) && length <= catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER) {
                    pseudoBlockingVertex = null;
                    //System.out.println("pseudoBlockingVertex null because target visible in FOLLOW_TARGET (can capture)");
                    lastPointVisible = null;
                    catcher.moveBy(target.getXPos() - catcher.getXPos(), target.getYPos() - catcher.getYPos());
                    target.setActive(false);
                    target = null;
                    origin = null;
                    spottedOnce = false;
                    spottedOutsideGuardingSquare = false;
                    currentStage = Stage.CATCHER_TO_SEARCHER;
                } else if (map.isVisible(target, catcher) && (!GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines) || inGuardedSquareOverSeparating)) {
                    /*if (GeometryOperations.lineIntersectSeparatingLines(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos(), separatingEdges)) {
                        System.out.println("Evader behind separating line");
                        return;
                    }*/
                    pseudoBlockingVertex = null;
                    //System.out.println("pseudoBlockingVertex null because target visible in FOLLOW_TARGET");
                    lastPointVisible = null;
                    // first case: target is visible
                    // perform simple lion's move


                    if (inGuardedSquareOverNonSeparating && specialShortestPathRoadMap == null) {
                        ArrayList<Line> temp = new ArrayList<>();
                        temp.add(currentGuardedSquare.getOriginalSeparatingLine());
                        specialShortestPathRoadMap = new ShortestPathRoadMap(temp, map);
                    } else if (inGuardedSquareOverSeparating && specialShortestPathRoadMap == null) {
                        ArrayList<Line> temp = new ArrayList<>();
                        for (int i = 1; i < currentGuardedSquare.getSquareSideLines().size(); i++) {
                            temp.add(currentGuardedSquare.getSquareSideLines().get(i));
                        }
                        specialShortestPathRoadMap = new ShortestPathRoadMap(temp, map);
                    }
                    PlannedPath temp;
                    if (inGuardedSquareOverNonSeparating && specialShortestPathRoadMap != null) {
                        temp = specialShortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
                    } else if (!spottedOutsideGuardingSquare) {
                        //temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), origin);
                        temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
                    } else if (inGuardedSquareOverSeparating && specialShortestPathRoadMap != null) {
                        temp = specialShortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
                    } else {
                        temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), origin);
                    }

                    //PlannedPath temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
                    //temp.draw();

                    guardGraphics.getChildren().clear();
                    Line lionsMoveLine = temp.getPathLine(0);
                    lionsMoveLine.setStroke(Color.INDIANRED);
                    guardGraphics.getChildren().add(lionsMoveLine);

                    //System.out.printf("lionsMoveLine: (%.3f|%.3f) to (%.3f|%.3f)\n", lionsMoveLine.getStartX(), lionsMoveLine.getStartY(), lionsMoveLine.getEndX(), lionsMoveLine.getEndY());

                    // calculate the perpendicular distance of the catcher's position to the line
                    // based on this find the parallel distance in either direction that will give the legal distance of movement
                    // find the two points and take the one closer to the target as the point to move to
                    PointVector closestPoint = GeometryOperations.closestPoint(catcher.getXPos(), catcher.getYPos(), lionsMoveLine);
                    //Main.pane.getChildren().add(new Circle(closestPoint.getX(), closestPoint.getY(), 1, Color.RED));
                    PointVector normal = new PointVector(closestPoint.getX() - catcher.getXPos(), closestPoint.getY() - catcher.getYPos());
                    double parallelLength = Math.sqrt(Math.pow(catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER, 2) - Math.pow(normal.length(), 2));
                    PointVector gradient = new PointVector(lionsMoveLine.getEndX() - lionsMoveLine.getStartX(), lionsMoveLine.getEndY() - lionsMoveLine.getStartY());
                    gradient = VectorOperations.multiply(gradient, 1 / gradient.length());
                    Point2D candidate1 = VectorOperations.add(closestPoint, VectorOperations.multiply(gradient, parallelLength)).toPoint();
                    Point2D candidate2 = VectorOperations.add(closestPoint, VectorOperations.multiply(gradient, -parallelLength)).toPoint();

                    //System.out.printf("candidate1: (%.3f|%.3f)\n", candidate1.getX(), candidate1.getY());
                    //System.out.printf("candidate2: (%.3f|%.3f)\n", candidate2.getX(), candidate2.getY());
                    //System.out.printf("normal: (%.3f|%.3f)\n", normal.getEstX(), normal.getEstY());

                    if (catcher.shareLocation(searcher)) {
                        if (Math.sqrt(Math.pow(candidate1.getX() - target.getXPos(), 2) + Math.pow(candidate1.getY() - target.getYPos(), 2)) < Math.sqrt(Math.pow(candidate2.getX() - target.getXPos(), 2) + Math.pow(candidate2.getY() - target.getYPos(), 2))) {
                            // move to first candidate point
                            catcher.moveBy(candidate1.getX() - catcher.getXPos(), candidate1.getY() - catcher.getYPos());
                            searcher.moveBy(candidate1.getX() - searcher.getXPos(), candidate1.getY() - searcher.getYPos());
                            Main.pane.getChildren().add(new Circle(candidate1.getX(), candidate1.getY(), 1, Color.BLACK));
                            //System.out.println("candidate1 chosen");
                        } else {
                            // move to second candidate point
                            catcher.moveBy(candidate2.getX() - catcher.getXPos(), candidate2.getY() - catcher.getYPos());
                            searcher.moveBy(candidate2.getX() - searcher.getXPos(), candidate2.getY() - searcher.getYPos());
                            Main.pane.getChildren().add(new Circle(candidate2.getX(), candidate2.getY(), 1, Color.BLACK));
                            //System.out.println("candidate2 chosen");
                        }
                    } else {
                        if (Math.sqrt(Math.pow(candidate1.getX() - target.getXPos(), 2) + Math.pow(candidate1.getY() - target.getYPos(), 2)) < Math.sqrt(Math.pow(candidate2.getX() - target.getXPos(), 2) + Math.pow(candidate2.getY() - target.getYPos(), 2))) {
                            // move to first candidate point
                            catcher.moveBy(candidate1.getX() - catcher.getXPos(), candidate1.getY() - catcher.getYPos());
                            Main.pane.getChildren().add(new Circle(candidate1.getX(), candidate1.getY(), 1, Color.BLACK));
                            //System.out.println("candidate1 chosen");
                        } else {
                            // move to second candidate point
                            catcher.moveBy(candidate2.getX() - catcher.getXPos(), candidate2.getY() - catcher.getYPos());
                            Main.pane.getChildren().add(new Circle(candidate2.getX(), candidate2.getY(), 1, Color.BLACK));
                            //System.out.println("candidate2 chosen");
                        }
                    }
                } else if (map.isVisible(target, catcher) && GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines)) {
                    System.out.println("WANT TO GET CATCHER BACK TO SEARCHER");
                    /*MapRepresentation.showVisible = true;
                    System.out.println("map.isVisible(catcher, searcher): " + map.isVisible(catcher, searcher));
                    System.out.println("map.isVisible(searcher, catcher): " + map.isVisible(searcher, catcher));
                    MapRepresentation.showVisible = false;
                    currentCatcherPath = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), searcher.getXPos(), searcher.getYPos());
                    System.out.println("currentCatcherPath: " + currentCatcherPath);*/
                    // except for when the searcher goes out searching for the evader, the catcher should always be within line of sight
                    // of the searcher (I think), so the following should be sufficient:
                    Line l = new Line(catcher.getXPos(), catcher.getYPos(), searcher.getXPos(), searcher.getYPos());
                    currentCatcherPath = new PlannedPath();
                    currentCatcherPath.addLine(l);
                    currentSearcherPath = null;
                    catcherPathLineCounter = 0;
                    searcherPathLineCounter = 0;
                    origin = null;
                    currentStage = Stage.CATCHER_TO_SEARCHER;
                } else {
                    // second case: target is not visible anymore (disappeared around corner)
                    // the method used here is cheating somewhat but assuming minimum feature size it just makes the computation easier
                    if (pseudoBlockingVertex == null) {
                        System.out.println("target around corner, calculate path to first vertex");
                        ShortestPathRoadMap.drawLines = true;
                        PlannedPath temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                        //PlannedPath temp = shortestPathRoadMap.getShortestPath(target.getXPos() - 1, target.getYPos(), origin.getEstX() + 1, origin.getEstY());
                        ShortestPathRoadMap.drawLines = false;
                        pseudoBlockingVertex = new Point2D(temp.getPathVertex(1).getEstX(), temp.getPathVertex(1).getEstY());
                        lastPointVisible = new Point2D(catcher.getXPos(), catcher.getYPos());
                        pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), target.getXPos(), -target.getYPos());

                        catchGraphics.getChildren().add(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 4, Color.BLUEVIOLET));

                        currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                        currentCatcherPath = catcher.shareLocation(searcher) ? currentSearcherPath : traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
                        searcherPathLineCounter = 0;
                        catcherPathLineCounter = 0;
                    }

                    // move searcher
                    if (currentSearcherPath != null) {
                        pathLines = currentSearcherPath.getPathLines();
                        if (!(searcher.getXPos() == currentSearcherPath.getEndX() && searcher.getYPos() == currentSearcherPath.getEndY())) {
                            length = Math.sqrt(Math.pow(pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY(), 2));
                            deltaX = (pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                            deltaY = (pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                            if (pathLines.get(searcherPathLineCounter).contains(searcher.getXPos() + deltaX, searcher.getYPos() + deltaY)) {
                                // move along line
                                searcher.moveBy(deltaX, deltaY);
                            } else {
                                // move to end of line
                                searcher.moveBy(pathLines.get(searcherPathLineCounter).getEndX() - searcher.getXPos(), pathLines.get(searcherPathLineCounter).getEndY() - searcher.getYPos());
                                searcherPathLineCounter++;
                            }
                        } else {
                            // after the last (searcher move) the evader was still visible and the pseudo-blocking vertex was reached
                            System.out.println("Searcher reached end of line");
                            System.out.println("Evader still visible: " + map.isVisible(searcher, target));
                        }
                    }

                    Main.pane.getChildren().add(new Circle(catcher.getXPos(), catcher.getYPos(), 1, Color.FUCHSIA));

                    // move catcher
                    pathLines = currentCatcherPath.getPathLines();
                    if (!(catcher.getXPos() == currentCatcherPath.getEndX() && catcher.getYPos() == currentCatcherPath.getEndY())) {
                        length = Math.sqrt(Math.pow(pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY(), 2));
                        deltaX = (pathLines.get(catcherPathLineCounter).getEndX() - pathLines.get(catcherPathLineCounter).getStartX()) / length * catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                        deltaY = (pathLines.get(catcherPathLineCounter).getEndY() - pathLines.get(catcherPathLineCounter).getStartY()) / length * catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                        if (pathLines.get(catcherPathLineCounter).contains(catcher.getXPos() + deltaX, catcher.getYPos() + deltaY)) {
                            // move along line
                            catcher.moveBy(deltaX, deltaY);
                        } else {
                            // move to end of line
                            catcher.moveBy(pathLines.get(catcherPathLineCounter).getEndX() - catcher.getXPos(), pathLines.get(catcherPathLineCounter).getEndY() - catcher.getYPos());
                            catcherPathLineCounter++;
                        }
                    } else {
                        System.out.println("Catcher reached end of line");
                    }

                    // if pseudo-blocking vertex has been reached without seeing the evader again
                    // if evader does become visible again, the old strategy is continued (should maybe already do that here)
                    if (catcher.getXPos() == pseudoBlockingVertex.getX() && catcher.getYPos() == pseudoBlockingVertex.getY() && (!map.isVisible(catcher, target) || (map.isVisible(catcher, target) && GeometryOperations.lineIntersectSeparatingLines(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos(), separatingLines)))) {
                        // do randomised search in pocket
                        // pocket to be calculated from blocking vertex and position that the evader was last seen from (?)
                        currentStage = Stage.FIND_TARGET;

                        // pocket from lastPointVisible over pseudoBlockingVertex to polygon boundary
                        // needs to be the current component though
                        int componentIndex = 0;
                        if (componentBoundaryLines.size() != 1) {
                            for (int i = 0; i < traversalHandler.getComponents().size(); i++) {
                                if (componentBoundaryShapes.get(i).contains(catcher.getXPos(), catcher.getYPos())) {
                                    componentIndex = i;
                                    break;
                                }
                            }
                        }
                        double rayStartX = lastPointVisible.getX();
                        double rayStartY = lastPointVisible.getY();
                        double rayDeltaX = pseudoBlockingVertex.getX() - rayStartX;
                        double rayDeltaY = pseudoBlockingVertex.getY() - rayStartY;
                        // determine pocket boundary line
                        Point2D currentPoint, pocketBoundaryEndPoint = null;
                        double minLengthSquared = Double.MAX_VALUE, currentLengthSquared;
                        Line intersectingLine = null;
                        for (Line line : componentBoundaryLines.get(componentIndex)) {
                            currentPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, line);
                            if (currentPoint != null && (currentLengthSquared = Math.pow(catcher.getXPos() - currentPoint.getX(), 2) + Math.pow(catcher.getYPos() - currentPoint.getY(), 2)) < minLengthSquared/*&& map.isVisible(catcher.getXPos(), catcher.getYPos(), pocketBoundaryEndPoint.getEstX(), pocketBoundaryEndPoint.getEstY())*/) {
                                minLengthSquared = currentLengthSquared;
                                pocketBoundaryEndPoint = currentPoint;
                                intersectingLine = line;
                                //Main.pane.getChildren().add(new Circle(currentPoint.getX(), currentPoint.getY(), 5, Color.DARKGRAY));
                                //found = true;
                                //break;
                            }/* else if (currentPoint != null) {
                                Main.pane.getChildren().add(new Circle(currentPoint.getEstX(), currentPoint.getEstY(), 2, Color.BLACK));
                            }*/
                        }
                        System.out.println("separatingLines.contains(intersectingLine): " + separatingLines.contains(intersectingLine));
                        // if yes, then look for the intersection point on the other side of the square
                        if (separatingLines.contains(intersectingLine)) {
                            // check for second intersection point:
                            LineString ls;
                            for (int i = 1; i < 4; i++) {
                                ls = squareGuardManagers.get(separatingLines.indexOf(intersectingLine)).getSquareSides().get(i);


                            }
                        }

                        // TODO: possibly extend the pocket to include the intersected parts of the guarding square
                        if (/*!found || */pocketBoundaryEndPoint == null) {
                            System.out.println("No pocket boundary end point found.");
                        } else {
                            Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), catcher.getXPos(), catcher.getYPos());
                            catchGraphics.getChildren().add(boundaryLine);
                            catchGraphics.getChildren().add(new Circle(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), 5, Color.BLACK));

                            for (SquareGuardManager gm : squareGuardManagers) {
                                if (intersectingLine.equals(gm.getOriginalSeparatingLine())) {
                                    System.out.println("Intersecting line is separating line");
                                    break;
                                }
                            }

                            // find the new "pocket component"
                            System.out.printf("Catcher at (%f|%f)\nReal at (%f|%f)\nFake at (%f|%f)\n", catcher.getXPos(), catcher.getYPos(), currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY(), currentCatcherPath.getLastPathVertex().getEstX(), currentCatcherPath.getLastPathVertex().getEstY());
                            Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY());
                            traversalHandler.restrictToPocket(pocketInfo.getFirst(), pocketInfo.getSecond());

                            // if the pocket boundary crosses through a separating line, extend it to whichever other line of that
                            // guarded square it hits (or the boundary of the polygon) and also take all of the triangles intersected in the square
                            // then add all other triangles in the square which lie on the correct side of the boundary

                            try {
                                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                                searcherPathLineCounter = 0;
                            } catch (DelaunayError e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (catcher.getXPos() == pseudoBlockingVertex.getX() && catcher.getYPos() == pseudoBlockingVertex.getY()) {
                        currentStage = Stage.FOLLOW_TARGET;
                        System.out.println("Still visible, following");
                    }
                }
            } else if (currentStage == Stage.FIND_TARGET) {
                // searcher and catcher are "locked onto" a target, but the searcher has to rediscover it
                // could do this by
                // a) restricting random traversals to the triangles that are in the pocket (at least in part)
                //    -> could check which triangles the line cuts through and don't allow movement beyond them
                // b) turning the searcher back when it tries to cross the line

                if (map.isVisible(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos()) && !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines)) {
                    pseudoBlockingVertex = null;
                    System.out.println("pseudoBlockingVertex null because target visible in FIND_TARGET");
                    currentStage = Stage.FOLLOW_TARGET;
                } else {
                    if (traversalHandler.getNodeIndex(searcher.getXPos(), searcher.getYPos()) == currentSearcherPath.getEndIndex()) {
                        try {
                            currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                        } catch (DelaunayError e) {
                            e.printStackTrace();
                        }
                        searcherPathLineCounter = 0;
                    }

                    pathLines = currentSearcherPath.getPathLines();
                    if (!(searcher.getXPos() == currentSearcherPath.getEndX() && searcher.getYPos() == currentSearcherPath.getEndY())) {
                        length = Math.sqrt(Math.pow(pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY(), 2));
                        deltaX = (pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                        deltaY = (pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                        if (pathLines.get(searcherPathLineCounter).contains(searcher.getXPos() + deltaX, searcher.getYPos() + deltaY)) {
                            // move along line
                            searcher.moveBy(deltaX, deltaY);
                        } else {
                            // move to end of line
                            searcher.moveBy(pathLines.get(searcherPathLineCounter).getEndX() - searcher.getXPos(), pathLines.get(searcherPathLineCounter).getEndY() - searcher.getYPos());
                            searcherPathLineCounter++;
                        }
                    } else {
                        // after the last (searcher move) the evader was still visible and the pseudo-blocking vertex was reached
                        System.out.println("Searcher reached end of line");
                        System.out.println("Evader still visible: " + map.isVisible(searcher, target));
                    }

                    if (map.isVisible(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos()) && !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos(), separatingLines)) {
                        System.out.println("target found again by searcher");
                        catchGraphics.getChildren().clear();

                        PlannedPath temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                        lastPointVisible = new Point2D(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                        pseudoBlockingVertex = new Point2D(temp.getPathVertex(1).getEstX(), temp.getPathVertex(1).getEstY());
                        pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), target.getXPos(), -target.getYPos());

                        int componentIndex = 0;
                        if (componentBoundaryLines.size() != 1) {
                            for (int i = 0; i < traversalHandler.getComponents().size(); i++) {
                                if (componentBoundaryShapes.get(i).contains(catcher.getXPos(), catcher.getYPos())) {
                                    componentIndex = i;
                                    break;
                                }
                            }
                        }
                        double rayStartX = lastPointVisible.getX();
                        double rayStartY = lastPointVisible.getY();
                        double rayDeltaX = pseudoBlockingVertex.getX() - rayStartX;
                        double rayDeltaY = pseudoBlockingVertex.getY() - rayStartY;
                        // determine pocket boundary line
                        Point2D currentPoint, pocketBoundaryEndPoint = null;
                        double minLengthSquared = Double.MAX_VALUE, currentLengthSquared;
                        for (Line line : componentBoundaryLines.get(componentIndex)) {
                            currentPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, line);
                            if (currentPoint != null && (currentLengthSquared = Math.pow(catcher.getXPos() - currentPoint.getX(), 2) + Math.pow(catcher.getYPos() - currentPoint.getY(), 2)) < minLengthSquared/*&& map.isVisible(catcher.getXPos(), catcher.getYPos(), pocketBoundaryEndPoint.getEstX(), pocketBoundaryEndPoint.getEstY())*/) {
                                minLengthSquared = currentLengthSquared;
                                pocketBoundaryEndPoint = currentPoint;
                            }
                        }
                        Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                        catchGraphics.getChildren().add(boundaryLine);
                        catchGraphics.getChildren().add(new Circle(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), 6, Color.BLACK));
                        Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                        traversalHandler.restrictToPocket(pocketInfo.getFirst(), pocketInfo.getSecond());

                        catchGraphics.getChildren().add(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 4, Color.BLUEVIOLET));

                        currentSearcherPath = null;
                        //currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                        currentCatcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
                        searcherPathLineCounter = 0;
                        catcherPathLineCounter = 0;

                        System.out.println("pseudoBlockingVertex null because target visible in FIND_TARGET (2)");
                        currentStage = Stage.FOLLOW_TARGET;
                    }
                }
            }
        }

        //System.out.printf("Catcher at (%.3f|%.3f)   |   Searcher at (%.3f|%.3f)\n", catcher.getXPos(), catcher.getYPos(), searcher.getXPos(), searcher.getYPos());
    }

    @Override
    public int totalRequiredAgents() {
        return requiredAgents;
    }

    @Override
    public int remainingRequiredAgents() {
        return requiredAgents - availableAgents.size();
    }

    @Override
    public void addAgent(Agent a) {
        availableAgents.add(a);
    }

    private void assignTasks() {
        if (availableAgents.size() < requiredAgents) {
            AdaptedSimulation.masterPause("Not enough agents for DCREntity");
        }

        // assign a certain number of agents to be guards for separating triangles
        initGuardPaths = new ArrayList<>();
        guards = new ArrayList<>();
        guardPathLineCounters = new ArrayList<>();
        double bestDistance, currentDistance;
        PlannedPath bestShortestPath, currentShortestPath;
        Agent tempClosestAgent;
        ArrayList<Agent> currentGuards;
        for (SquareGuardManager gm : squareGuardManagers) {
            currentGuards = new ArrayList<>();
            for (Coordinate c : gm.getOriginalPositions()) {
                bestDistance = Double.MAX_VALUE;
                bestShortestPath = null;
                tempClosestAgent = null;
                for (Agent a : availableAgents) {
                    if (!guards.contains(a)) {
                        // compute distance to current triangle
                        currentShortestPath = shortestPathRoadMap.getShortestPath(a.getXPos(), a.getYPos(), c.x, c.y);
                        currentDistance = currentShortestPath.getTotalLength();
                        if (currentDistance < bestDistance) {
                            bestDistance = currentDistance;
                            bestShortestPath = currentShortestPath;
                            tempClosestAgent = a;
                        }
                        /*guards.add(a);
                        currentGuards.add(a);
                        initGuardPaths.add(shortestPathRoadMap.getShortestPath(a.getXPos(), a.getYPos(), c.x, c.y));
                        break;*/
                    }
                }
                guards.add(tempClosestAgent);
                currentGuards.add(tempClosestAgent);
                initGuardPaths.add(bestShortestPath);
            }
            gm.assignGuards(currentGuards);
        }

        System.out.println("guards: " + guards.size());
        for (Agent g : guards) {
            guardPathLineCounters.add(0);
        }

        // the computed PlannedPath objects will initially be used to position all the guards in their correct locations
        // the (at least 2) remaining agents will be assigned to be searcher (and catcher)
        for (Agent a : availableAgents) {
            if (!guards.contains(a)) {
                searcher = a;
                break;
            }
        }
        for (Agent a : availableAgents) {
            if (!guards.contains(a) && !a.equals(searcher)) {
                catcher = a;
                break;
            }
        }
        currentStage = Stage.CATCHER_TO_SEARCHER;
        ShortestPathRoadMap.SHOW_ON_CANVAS = true;
        currentCatcherPath = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), searcher.getXPos(), searcher.getYPos());
        ShortestPathRoadMap.SHOW_ON_CANVAS = false;
    }

    private boolean guardsPositioned() {
        if (!guardsPositioned) {
            for (int i = 0; i < guards.size(); i++) {
                if (guards.get(i).getXPos() != initGuardPaths.get(i).getEndX() || guards.get(i).getYPos() != initGuardPaths.get(i).getEndY()) {
                    return false;
                }
            }
            guardsPositioned = true;
            return true;
        }
        return true;
    }

    private void computeRequirements() {
        // build needed data structures and analyse map to see how many agents are required
        try {
            // computing the triangulation of the given map
            // what is returned are the triangles of the map itself and those of the holes
            Tuple<ArrayList<DTriangle>, ArrayList<DTriangle>> triangles = triangulate(map);
            ArrayList<DTriangle> nodes = triangles.getFirst();
            ArrayList<DTriangle> holeTriangles = triangles.getSecond();

            // computing adjacency between the triangles in the map -> modelling it as a graph
            Tuple<int[][], int[]> matrices = computeAdjacency(nodes);
            int[][] originalAdjacencyMatrix = matrices.getFirst();
            int[] degreeMatrix = matrices.getSecond();

            // grouping hole triangles by holes (through adjacency)
            ArrayList<ArrayList<DTriangle>> holes = computeHoles(holeTriangles);

            // compute separating triangles and the updated adjacency matrix
            Triplet<ArrayList<DTriangle>, ArrayList<DEdge>, int[][]> separation = computeSeparatingTriangles(nodes, holes, originalAdjacencyMatrix, degreeMatrix);
            ArrayList<DTriangle> separatingTriangles = separation.getValue0();
            ArrayList<DEdge> nonSeparatingLines = separation.getValue1();
            int[][] spanningTreeAdjacencyMatrix = separation.getValue2();

            // show the computed spanning tree on the main pane
            ArrayList<Line> tree = showSpanningTree(nodes, spanningTreeAdjacencyMatrix);

            // compute the simply connected components in the graph
            ArrayList<DTriangle> componentNodes = new ArrayList<>();
            for (DTriangle dt : nodes) {
                if (!separatingTriangles.contains(dt)) {
                    componentNodes.add(dt);
                }
            }
            Tuple<ArrayList<ArrayList<DTriangle>>, int[]> componentInfo = computeConnectedComponents(nodes, componentNodes, spanningTreeAdjacencyMatrix);
            ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = componentInfo.getFirst();
            int[] parentNodes = componentInfo.getSecond();
            System.out.println("holes.size(): " + holes.size() + "\nseparatingTriangles.size(): " + separatingTriangles.size() + "\nsimplyConnectedComponents.size(): " + simplyConnectedComponents.size());

            // if there are more than 2 components compute change triangles around so that there is only one
            // looks like it may not be needed at all
            //computeSingleConnectedComponent(simplyConnectedComponents, holes, nodes, separatingTriangles, spanningTreeAdjacencyMatrix, originalAdjacencyMatrix, parentNodes, tree);

            Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> lineSeparation = computeGuardingLines(separatingTriangles, nonSeparatingLines);
            ArrayList<Line> separatingLines = lineSeparation.getValue0();
            ArrayList<DEdge> reconnectingEdges = lineSeparation.getValue1();
            ArrayList<DEdge> separatingEdges = lineSeparation.getValue2();

            Tuple<int[][], ArrayList<ArrayList<DTriangle>>> reconnectedAdjacency = computeReconnectedAdjacency(nodes, simplyConnectedComponents, reconnectingEdges, spanningTreeAdjacencyMatrix, separatingTriangles);
            int[][] reconnectedAdjacencyMatrix = reconnectedAdjacency.getFirst();
            ArrayList<ArrayList<DTriangle>> reconnectedComponents = reconnectedAdjacency.getSecond();

            Tuple<ArrayList<ArrayList<Line>>, ArrayList<Shape>> componentBoundaries = computeComponentBoundaries(reconnectedComponents, separatingEdges, separatingLines);
            componentBoundaryLines = componentBoundaries.getFirst();
            componentBoundaryShapes = componentBoundaries.getSecond();

            squareGuardManagers = computeGuardManagers(separatingLines);

            gSqrIntersectingTriangles = computeGuardingSquareIntersection(squareGuardManagers, nodes);

            // given the spanning tree adjacency matrix and all the triangles, the tree structure that will be used
            // for deciding on randomised paths can be constructed

            // if separating triangles are used
            //traversalHandler = new TraversalHandler(shortestPathRoadMap, nodes, simplyConnectedComponents, spanningTreeAdjacencyMatrix);
            //traversalHandler.separatingTriangleBased(separatingTriangles);

            // if separating lines are used
            //simplyConnectedComponents.set(0, nodes);
            traversalHandler = new TraversalHandler(shortestPathRoadMap, nodes, simplyConnectedComponents, spanningTreeAdjacencyMatrix);
            ShortestPathRoadMap.SHOW_ON_CANVAS = false;
            //traversalHandler.separatingLineBased(separatingLines);
            traversalHandler.separatingLineBased(separatingLines, reconnectedComponents, reconnectedAdjacencyMatrix);
            ShortestPathRoadMap.SHOW_ON_CANVAS = false;

            this.separatingLines = separatingLines;

            for (SquareGuardManager gm : squareGuardManagers) {
                requiredAgents += gm.totalRequiredGuards();
            }
            requiredAgents += 2;
            System.out.println("\nrequiredAgents: " + requiredAgents);


            /*ArrayList<Integer> nextLayer, currentLayer = new ArrayList<>();
            currentLayer.add(15);
            ArrayList<Line> lineTree = new ArrayList<>();
            Line temp;
            boolean unexploredLeft = true;
            boolean[] visitedNodes = new boolean[reconnectedAdjacencyMatrix.length];
            int[] parentNodesThing = new int[reconnectedAdjacencyMatrix.length];
            parentNodesThing[15] = -1;
            while (unexploredLeft) {
                nextLayer = new ArrayList<>();
                for (int i : currentLayer) {
                    visitedNodes[i] = true;
                    for (int j = 0; j < reconnectedAdjacencyMatrix.length; j++) {
                        if (reconnectedAdjacencyMatrix[i][j] == 1 && j != parentNodesThing[i] && !visitedNodes[j]) {
                        System.out.println("thing: " + j);
                            nextLayer.add(j);
                            parentNodesThing[j] = i;
                            visitedNodes[j] = true;

                            temp = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                            temp.setStroke(Color.RED);
                            temp.setStrokeWidth(4);
                            lineTree.add(temp);
                        }
                    }
                }
                currentLayer = nextLayer;
                if (nextLayer.size() == 0) {
                    unexploredLeft = false;
                }
            }
            Main.pane.getChildren().addAll(lineTree);*/
        } catch (DelaunayError error) {
            error.printStackTrace();
        }
        //requiredAgents = 2;
    }

    private ArrayList<SquareGuardManager> computeGuardManagers(ArrayList<Line> separatingLines) {
        ArrayList<SquareGuardManager> squareGuardManagers = new ArrayList<>(separatingLines.size());
        SquareGuardManager temp1, temp2;


        for (Line l : separatingLines) {
            temp1 = computeSingleGuardManager(l, false);
            temp2 = computeSingleGuardManager(l, true);
            if (temp1.getOriginalPositions().size() < temp2.getOriginalPositions().size()) {
                squareGuardManagers.add(temp1);
                for (int i = 0; i < temp1.getGuardedSquare().getCoordinates().length - 1; i++) {
                    Line line = new Line(temp1.getGuardedSquare().getCoordinates()[i].x, temp1.getGuardedSquare().getCoordinates()[i].y, temp1.getGuardedSquare().getCoordinates()[i + 1].x, temp1.getGuardedSquare().getCoordinates()[i + 1].y);
                    line.setStrokeWidth(4);
                    line.setStroke(Color.LIGHTBLUE);
                    Main.pane.getChildren().add(line);
                }
            } else {
                squareGuardManagers.add(temp2);
                for (int i = 0; i < temp2.getGuardedSquare().getCoordinates().length - 1; i++) {
                    Line line = new Line(temp2.getGuardedSquare().getCoordinates()[i].x, temp2.getGuardedSquare().getCoordinates()[i].y, temp2.getGuardedSquare().getCoordinates()[i + 1].x, temp2.getGuardedSquare().getCoordinates()[i + 1].y);
                    line.setStrokeWidth(4);
                    line.setStroke(Color.LIGHTBLUE);
                    Main.pane.getChildren().add(line);
                }
            }
        }

        /*
        for each line segment, two guards should be assigned
        this is not done if there is already a guard in that square that has been assigned to the same points
         */

        return squareGuardManagers;
    }

    private SquareGuardManager computeSingleGuardManager(Line l, boolean reverseSign) {
        // **************************************** //
        // variables to be used for the computation //
        // **************************************** //
        SquareGuardManager squareGuardManager;

        LinearRing guardedSquare;
        com.vividsolutions.jts.geom.Polygon guardedPolygon;
        ArrayList<LineString> squareSides;

        HashMap<LineString, ArrayList<LineString>> entranceToGuarded; // ordered: parallel, perpendicular1, perpendicular2
        HashMap<LineString, ArrayList<LineSegment>> guardedToSegments;

        ArrayList<LineSegment> tempLineSegments;
        ArrayList<LineString> tempLineStrings;
        LineSegment tempLineSegment;

        Coordinate[][] intersections = new Coordinate[3][];

        double length1, length2, deltaX, deltaY;
        LineSegment[] lines = new LineSegment[4];

        // ****************** //
        // actual computation //
        // ****************** //
        lines[0] = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
        length1 = Math.sqrt(Math.pow(lines[0].getCoordinate(0).x - lines[0].getCoordinate(1).x, 2) + Math.pow(lines[0].getCoordinate(0).y - lines[0].getCoordinate(1).y, 2));
        deltaX = -(lines[0].getCoordinate(0).y - lines[0].getCoordinate(1).y);
        deltaY = lines[0].getCoordinate(0).x - lines[0].getCoordinate(1).x;

        // construct the square
        length2 = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        deltaX /= length2 / length1;
        deltaY /= length2 / length1;
        lines[1] = new LineSegment(lines[0].getCoordinate(0).x, lines[0].getCoordinate(0).y, lines[0].getCoordinate(0).x + (reverseSign ? -deltaX : deltaX), lines[0].getCoordinate(0).y + (reverseSign ? -deltaY : deltaY));
        lines[2] = new LineSegment(lines[0].getCoordinate(1).x, lines[0].getCoordinate(1).y, lines[0].getCoordinate(1).x + (reverseSign ? -deltaX : deltaX), lines[0].getCoordinate(1).y + (reverseSign ? -deltaY : deltaY));
        lines[3] = new LineSegment(lines[0].getCoordinate(0).x + (reverseSign ? -deltaX : deltaX), lines[0].getCoordinate(0).y + (reverseSign ? -deltaY : deltaY), lines[0].getCoordinate(1).x + (reverseSign ? -deltaX : deltaX), lines[0].getCoordinate(1).y + (reverseSign ? -deltaY : deltaY));

        intersections[0] = map.getBoundary().intersection(lines[1].toGeometry(GeometryOperations.factory)).getCoordinates();
        intersections[1] = map.getBoundary().intersection(lines[2].toGeometry(GeometryOperations.factory)).getCoordinates();
        intersections[2] = map.getBoundary().intersection(lines[3].toGeometry(GeometryOperations.factory)).getCoordinates();

        // storing information about the square
        squareSides = new ArrayList<>();
        squareSides.add(lines[0].toGeometry(GeometryOperations.factory));
        squareSides.add(lines[1].toGeometry(GeometryOperations.factory));
        squareSides.add(lines[2].toGeometry(GeometryOperations.factory));
        squareSides.add(lines[3].toGeometry(GeometryOperations.factory));
        guardedSquare = new LinearRing(new CoordinateArraySequence(new Coordinate[]{lines[0].getCoordinate(0), lines[1].getCoordinate(1), lines[3].getCoordinate(1), lines[0].getCoordinate(1), lines[0].getCoordinate(0)}), GeometryOperations.factory);
        guardedPolygon = new com.vividsolutions.jts.geom.Polygon(guardedSquare, null, GeometryOperations.factory);

        // storing information about which entrance line maps to which guarded lines
        entranceToGuarded = new HashMap<>(4);
        tempLineStrings = new ArrayList<>();
        tempLineStrings.add(squareSides.get(3)); // parallel first
        tempLineStrings.add(squareSides.get(1)); // perpendicular 1
        tempLineStrings.add(squareSides.get(2)); // perpendicular 2
        entranceToGuarded.put(squareSides.get(0), tempLineStrings);
        tempLineStrings = new ArrayList<>();
        tempLineStrings.add(squareSides.get(2));
        tempLineStrings.add(squareSides.get(0));
        tempLineStrings.add(squareSides.get(3));
        entranceToGuarded.put(squareSides.get(1), tempLineStrings);
        tempLineStrings = new ArrayList<>();
        tempLineStrings.add(squareSides.get(1));
        tempLineStrings.add(squareSides.get(0));
        tempLineStrings.add(squareSides.get(3));
        entranceToGuarded.put(squareSides.get(2), tempLineStrings);
        tempLineStrings = new ArrayList<>();
        tempLineStrings.add(squareSides.get(0));
        tempLineStrings.add(squareSides.get(1));
        tempLineStrings.add(squareSides.get(2));
        entranceToGuarded.put(squareSides.get(3), tempLineStrings);

        guardedToSegments = new HashMap<>();

        tempLineSegments = new ArrayList<>();
        tempLineSegments.add(lines[0]);
        guardedToSegments.put(squareSides.get(0), tempLineSegments);
        /*Line aLine = new Line(lines[0].getCoordinate(0).x, lines[0].getCoordinate(0).y, lines[0].getCoordinate(1).x, lines[0].getCoordinate(1).y);
        aLine.setStroke(Color.LIGHTGREEN);
        aLine.setStrokeWidth(3);
        Main.pane.getChildren().add(aLine);*/

        for (int i = 1; i < lines.length; i++) {
            tempLineSegments = new ArrayList<>();
            if (map.isVisible(lines[i].getCoordinate(0).x, lines[i].getCoordinate(0).y, lines[i].getCoordinate(1).x, lines[i].getCoordinate(1).y)) {
                // add to guards assigned to these endpoints
                tempLineSegments.add(lines[i]);
                /*Line line = new Line(lines[i].getCoordinate(0).x, lines[i].getCoordinate(0).y, lines[i].getCoordinate(1).x, lines[i].getCoordinate(1).y);
                line.setStroke(Color.BLUE);
                line.setStrokeWidth(2);
                Main.pane.getChildren().add(line);*/
            } else {
                ArrayList<Coordinate> currentIntersectionPoints = new ArrayList<>();
                Coordinate[] intersectionArray = intersections[i - 1];
                for (Coordinate c : intersectionArray) {
                    if (!c.equals2D(lines[0].getCoordinate(0)) && !c.equals2D(lines[0].getCoordinate(1))) {
                        boolean changed = false;
                        //Main.pane.getChildren().add(new Circle(c.x, c.y, 4.5, Color.CYAN));
                        Point p = new Point(new CoordinateArraySequence(new Coordinate[]{c}), GeometryOperations.factory);

                        if (map.getPolygon().distance(p) != 0.0) {
                            //Main.pane.getChildren().add(new Circle(c.x, c.y, 4.5, Color.CYAN));

                            deltaX = lines[i].getCoordinate(1).x - lines[i].getCoordinate(0).x;
                            deltaY = lines[i].getCoordinate(1).y - lines[i].getCoordinate(0).y;
                            double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                            deltaX /= length;
                            deltaY /= length;
                            double dist = map.getPolygon().distance(p);
                            Point candidate1 = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(c.x + deltaX * (100 * dist), c.y + deltaY * (100 * dist))}), GeometryOperations.factory);
                            Point candidate2 = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(c.x - deltaX * (100 * dist), c.y - deltaY * (100 * dist))}), GeometryOperations.factory);

                            if (map.getPolygon().distance(candidate1) == 0.0) {
                                p = candidate1;
                                c.x = p.getX();
                                c.y = p.getY();
                            } else if (map.getPolygon().distance(candidate2) == 0.0) {
                                p = candidate2;
                                c.x = p.getX();
                                c.y = p.getY();
                            } else {
                                System.exit(45745);
                            }
                            changed = true;
                        } else {
                            deltaX = lines[i].getCoordinate(1).x - lines[i].getCoordinate(0).x;
                            deltaY = lines[i].getCoordinate(1).y - lines[i].getCoordinate(0).y;
                            double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                            deltaX /= length;
                            deltaY /= length;
                            Point candidate1 = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(c.x + deltaX * 1E-7, c.y + deltaY * 1E-7)}), GeometryOperations.factory);
                            Point candidate2 = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(c.x - deltaX * 1E-7, c.y - deltaY * 1E-7)}), GeometryOperations.factory);

                            if (map.getPolygon().distance(candidate1) == 0.0) {
                                p = candidate1;
                                c.x = p.getX();
                                c.y = p.getY();
                            } else if (map.getPolygon().distance(candidate2) == 0.0) {
                                p = candidate2;
                                c.x = p.getX();
                                c.y = p.getY();
                            } else {
                                System.exit(45745);
                            }
                        }

                        /*Label label = new Label(map.getPolygon().distance(p) + "");
                        label.setTranslateX(c.x + 5);
                        label.setTranslateY(c.y);
                        Main.pane.getChildren().add(label);*/

                        currentIntersectionPoints.add(c);
                        //System.out.printf("(%f|%f) vs (%f|%f)\n", p.getCoordinate().x, p.getCoordinate().y, p.getX(), p.getY());
                        if (map.legalPosition(c.x, c.y)) {
                            //Main.pane.getChildren().add(new Circle(p.getCoordinate().x, p.getCoordinate().y, 2.5, changed ? Color.FUCHSIA : Color.WHITE));
                        }
                    }
                }

                // check visibility with endpoints and everything else
                Coordinate c1, c2;
                for (int j = 0; j >= 0 && currentIntersectionPoints.size() > 0 && j < currentIntersectionPoints.size(); j++) {
                    c1 = currentIntersectionPoints.get(j);
                    if (map.isVisible(c1.x, c1.y, lines[i].getCoordinate(0).x, lines[i].getCoordinate(0).y)) {
                        tempLineSegment = new LineSegment(c1.x, c1.y, lines[i].getCoordinate(0).x, lines[i].getCoordinate(0).y);
                        tempLineSegments.add(tempLineSegment);
                        currentIntersectionPoints.remove(c1);
                        j--;

                        /*Line line = new Line(c1.x, c1.y, lines[i].getCoordinate(0).x, lines[i].getCoordinate(0).y);
                        line.setStroke(Color.BLUE);
                        line.setStrokeWidth(2);
                        Main.pane.getChildren().add(line);*/
                    } else if (map.isVisible(c1.x, c1.y, lines[i].getCoordinate(1).x, lines[i].getCoordinate(1).y)) {
                        tempLineSegment = new LineSegment(c1.x, c1.y, lines[i].getCoordinate(1).x, lines[i].getCoordinate(1).y);
                        tempLineSegments.add(tempLineSegment);
                        currentIntersectionPoints.remove(c1);
                        j--;

                        /*Line line = new Line(c1.x, c1.y, lines[i].getCoordinate(1).x, lines[i].getCoordinate(1).y);
                        line.setStroke(Color.BLUE);
                        line.setStrokeWidth(2);
                        Main.pane.getChildren().add(line);*/
                    } else {
                        for (int k = 0; k < currentIntersectionPoints.size(); k++) {
                            c2 = currentIntersectionPoints.get(k);
                            // check visibility with endpoints and everything else
                            if (c1 != c2 && map.isVisible(c1.x, c1.y, c2.x, c2.y)) {
                                tempLineSegment = new LineSegment(c1.x, c1.y, c2.x, c2.y);
                                tempLineSegments.add(tempLineSegment);
                                currentIntersectionPoints.remove(c1);
                                currentIntersectionPoints.remove(c2);
                                j -= 2;

                                /*Line line = new Line(c1.x, c1.y, c2.x, c2.y);
                                line.setStroke(Color.BLUE);
                                line.setStrokeWidth(2);
                                Main.pane.getChildren().add(line);*/
                                break;
                            }
                        }
                    }
                }
                currentIntersectionPoints.clear();
            }
            guardedToSegments.put(squareSides.get(i), tempLineSegments);
        }

        squareGuardManager = new SquareGuardManager(l, guardedPolygon, squareSides, entranceToGuarded, guardedToSegments);
        //squareGuardManagers.add(squareGuardManager);

        /*Main.pane.getChildren().add(l);
        Main.pane.getChildren().add(new Line(lines[1].getCoordinate(0).x, lines[1].getCoordinate(0).y, lines[1].getCoordinate(1).x, lines[1].getCoordinate(1).y));
        Main.pane.getChildren().add(new Line(lines[2].getCoordinate(0).x, lines[2].getCoordinate(0).y, lines[2].getCoordinate(1).x, lines[2].getCoordinate(1).y));
        Main.pane.getChildren().add(new Line(lines[3].getCoordinate(0).x, lines[3].getCoordinate(0).y, lines[3].getCoordinate(1).x, lines[3].getCoordinate(1).y));*/
        return squareGuardManager;
    }

    private Tuple<ArrayList<DTriangle>, int[][]> findPocketComponent(Line boundaryLine, int componentIndex, double pseudoBlockingVertX, double pseudoBlockingVertY) {
        // go through all triangles of the current component and find those intersecting the boundary line
        // start to find the rest of the triangles of the pocket component using adjacency matrices
        // if a triangle is adjacent to one of the intersected triangles, test whether the edge connecting them
        // lies to the right or left (clockwise or counter-clockwise) of the boundary line (by checking endpoints)

        double length = Math.sqrt(Math.pow(boundaryLine.getEndX() - boundaryLine.getStartX(), 2) + Math.pow(boundaryLine.getEndY() - boundaryLine.getStartY(), 2));
        double deltaX = (boundaryLine.getStartX() - boundaryLine.getEndX()) / length * 0.001;
        double deltaY = (boundaryLine.getStartY() - boundaryLine.getEndY()) / length * 0.001;
        DPoint approxPosition = null;
        try {
            approxPosition = new DPoint(pseudoBlockingVertX + deltaX, pseudoBlockingVertY + deltaY, 0);
        } catch (DelaunayError e) {
            e.printStackTrace();
        }

        ArrayList<DTriangle> pocketBoundaryTriangles = new ArrayList<>();
        DTriangle currentTriangle = null;
        DPoint currentPoint = null;
        // find the DPoint corresponding to the vertex where the catcher is positioned
        //outer:
        double minDistance = Double.MAX_VALUE, currentDistance;
        for (DTriangle dt : traversalHandler.getComponents().get(componentIndex)) {
            for (DPoint dp : dt.getPoints()) {
                currentDistance = Math.pow(dp.getX() - pseudoBlockingVertX, 2) + Math.pow(dp.getY() - pseudoBlockingVertY, 2);
                if (currentDistance < minDistance) {
                    currentPoint = dp;
                    minDistance = currentDistance;
                }
                /*if (dp.getX() == pseudoBlockingVertX && dp.getY() == pseudoBlockingVertY) {
                    currentPoint = dp;
                    break outer;
                }*/
            }
        }

        for (DTriangle dt : traversalHandler.getComponents().get(componentIndex)) {
            if (dt.contains(approxPosition)) {
                currentTriangle = dt;
                break;
            }
        }

        try {
            System.out.println("Catcher vertex: (" + currentPoint.getX() + "|" + currentPoint.getY() + ")");
        } catch (NullPointerException e) {
            e.printStackTrace();
            AdaptedSimulation.masterPause("in DCREntity");
        }

        Polygon plgn;
        plgn = new Polygon(currentTriangle.getPoint(0).getX(), currentTriangle.getPoint(0).getY(), currentTriangle.getPoint(1).getX(), currentTriangle.getPoint(1).getY(), currentTriangle.getPoint(2).getX(), currentTriangle.getPoint(2).getY());
        plgn.setFill(Color.BLUE.deriveColor(1, 1, 1, 0.1));
        catchGraphics.getChildren().add(plgn);
        pocketBoundaryTriangles.add(currentTriangle);
        // add all triangles that are intersected by the boundary line to the list
        LineString lineString = new LineString(new CoordinateArraySequence(new Coordinate[]{new Coordinate(boundaryLine.getStartX(), boundaryLine.getStartY()), new Coordinate(boundaryLine.getEndX(), boundaryLine.getEndY())}), GeometryOperations.factory);
        LinearRing linearRing;
        for (DTriangle dt : traversalHandler.getComponents().get(componentIndex)) {
            Coordinate first = new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY());
            linearRing = new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                    first,
                    new Coordinate(dt.getPoint(1).getX(), dt.getPoint(1).getY()),
                    new Coordinate(dt.getPoint(2).getX(), dt.getPoint(2).getY()),
                    first
            }), GeometryOperations.factory);
            /*if (dt != currentTriangle && !dt.getPoints().contains(currentPoint) && GeometryOperations.lineTriangleIntersectWithoutPoints(boundaryLine, dt)) {
                plgn = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                plgn.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.1));
                catchGraphics.getChildren().add(plgn);
                pocketBoundaryTriangles.add(dt);
            }*/
            /*Geometry intersection = linearRing.intersection(lineString);
            System.out.println("boundary line: " + lineString);
            System.out.println("intersection: " + intersection);
            System.out.println("point 0: " + dt.getPoint(0));
            System.out.println("point 1: " + dt.getPoint(1));
            System.out.println("point 2: " + dt.getPoint(2));*/
            double distance0 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(0).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(0).getY(), 2));
            double distance1 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(1).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(1).getY(), 2));
            double distance2 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(2).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(2).getY(), 2));
            if (dt != currentTriangle && (linearRing.intersects(lineString)) && distance0 > minDistance && distance1 > minDistance && distance2 > minDistance) {
                plgn = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                plgn.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.1));
                catchGraphics.getChildren().add(plgn);
                pocketBoundaryTriangles.add(dt);
            }
        }

        // also go through guarding square triangles
        // get all triangles in the guard square which are intersected by the boundary line

        ArrayList<DTriangle> connectingTriangles = new ArrayList<>();
        ArrayList<DEdge> connectingEdges = new ArrayList<>();
        for (DTriangle dt1 : pocketBoundaryTriangles) {
            for (DEdge de : dt1.getEdges()) {
                boolean found = false;
                for (DTriangle dt2 : pocketBoundaryTriangles) {
                    if (dt1 != dt2 && dt2.isEdgeOf(de)) {
                        found = true;
                        break;
                    }
                }
                if (!found && (!componentBoundaryEdges.get(componentIndex).contains(de) || separatingEdges.contains(de))) {
                    // TODO: check whether this component boundary edge is also a separating edge, if so, include the guarded square connected to it
                    Line l = new Line(de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY());
                    l.setStroke(Color.RED);
                    l.setStrokeWidth(2);
                    //Main.pane.getChildren().add(l);
                    Label label = new Label("1: " + GeometryOperations.leftTurnPredicate(boundaryLine.getEndX(), boundaryLine.getEndY(), boundaryLine.getStartX(), boundaryLine.getStartY(), de.getPointLeft().getX(), de.getPointLeft().getY()));
                    label.setTranslateX(GeometryOperations.getLineMiddle(l).getX() + 5);
                    label.setTranslateY(GeometryOperations.getLineMiddle(l).getY());
                    //Main.pane.getChildren().add(label);
                    label = new Label("2: " + GeometryOperations.leftTurnPredicate(boundaryLine.getEndX(), boundaryLine.getEndY(), boundaryLine.getStartX(), boundaryLine.getStartY(), de.getPointRight().getX(), de.getPointRight().getY()));
                    label.setTranslateX(GeometryOperations.getLineMiddle(l).getX() + 5);
                    label.setTranslateY(GeometryOperations.getLineMiddle(l).getY() + 15);
                    //Main.pane.getChildren().add(label);
                    if (GeometryOperations.leftTurnPredicate(boundaryLine.getStartX(), boundaryLine.getStartY(), boundaryLine.getEndX(), boundaryLine.getEndY(), de.getPointLeft().getX(), de.getPointLeft().getY()) == pocketCounterClockwise &&
                            GeometryOperations.leftTurnPredicate(boundaryLine.getStartX(), boundaryLine.getStartY(), boundaryLine.getEndX(), boundaryLine.getEndY(), de.getPointRight().getX(), de.getPointRight().getY()) == pocketCounterClockwise) {
                        // TODO: add extra check for guarded square triangles, which should also be added under this condition
                        connectingTriangles.add(dt1);
                        connectingEdges.add(de);
                        l = new Line(de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY());
                        l.setStroke(Color.BLUE);
                        l.setStrokeWidth(2);
                        //Main.pane.getChildren().add(l);
                    }
                }
            }
        }

        int[][] pocketAdjacencyMatrix = new int[traversalHandler.getAdjacencyMatrix().length][traversalHandler.getAdjacencyMatrix().length];
        ArrayList<DTriangle> pbtClone = new ArrayList<>();
        pbtClone.addAll(pocketBoundaryTriangles);
        ArrayList<DTriangle> currentLayer = new ArrayList<>(), nextLayer;
        boolean unexploredLeft = true;
        for (DTriangle dt : connectingTriangles) {
            currentLayer.add(dt);
            pocketBoundaryTriangles.remove(dt);
            /*p = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
            p.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.4));
            Main.pane.getChildren().add(p);*/
        }
        boolean first = true;
        DEdge separatingEdge;
        DTriangle otherTriangle;
        ArrayList<DTriangle> currentSquareIntersecting;
        while (unexploredLeft) {
            nextLayer = new ArrayList<>();
            for (DTriangle dt : currentLayer) {
                pocketBoundaryTriangles.add(dt);
                for (int i = 0; i < traversalHandler.getAdjacencyMatrix().length; i++) {
                    // either they are properly adjacent and connected anyway
                    // or they share a separating edge, i.e. a guarding square is connected to the triangle
                    if (pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt)][i] != 1) {
                        if (traversalHandler.getAdjacencyMatrix()[traversalHandler.getTriangles().indexOf(dt)][i] == 1 && !pocketBoundaryTriangles.contains(traversalHandler.getTriangles().get(i))) {
                            if (!first || (traversalHandler.getTriangles().get(i).equals(connectingEdges.get(connectingTriangles.indexOf(dt)).getOtherTriangle(dt)))) {
                                nextLayer.add(traversalHandler.getTriangles().get(i));
                                pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt)][i] = 1;
                                pocketAdjacencyMatrix[i][traversalHandler.getTriangles().indexOf(dt)] = 1;
                            }
                        }
                    }
                }
                // find out whether the current triangle shares a separating edge with a guarded square
                separatingEdge = separatingEdges.contains(dt.getEdge(0)) ? dt.getEdge(0) : (separatingEdges.contains(dt.getEdge(1)) ? dt.getEdge(1) : (separatingEdges.contains(dt.getEdge(2)) ? dt.getEdge(2) : null));
                for (ArrayList<DTriangle> arr : gSqrIntersectingTriangles) {
                    if (arr.contains(dt)) {
                        separatingEdge = null;
                        break;
                    }
                }
                // need the connecting edge, then the two triangles adjacent to the separating line should be reconnected
                if (separatingEdge != null) {
                    plgn = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                    plgn.setFill(Color.INDIANRED.deriveColor(1.0, 1.0, 1.0, 1.0));
                    catchGraphics.getChildren().add(plgn);

                    otherTriangle = separatingEdge.getOtherTriangle(dt);
                    //pocketBoundaryTriangles.add(otherTriangle);
                    pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt)][traversalHandler.getTriangles().indexOf(otherTriangle)] = 1;
                    pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(otherTriangle)][traversalHandler.getTriangles().indexOf(dt)] = 1;

                    // find the square intersecting triangles that the "other triangle" is part of, i.e. find the square to add to the pocket
                    currentSquareIntersecting = new ArrayList<>();
                    for (ArrayList<DTriangle> arr : gSqrIntersectingTriangles) {
                        if (arr.contains(otherTriangle)) {
                            currentSquareIntersecting = arr;
                            break;
                        }
                    }
                    pocketBoundaryTriangles.addAll(currentSquareIntersecting);

                    // also attach the other triangles from that set of square intersecting triangles
                    for (DTriangle dt1 : currentSquareIntersecting) {
                        for (DTriangle dt2 : currentSquareIntersecting) {
                            //System.out.println("dt1: " + dt1 + "\ndt2: " + dt2 + "\ndt1.getEdge(0).getOtherTriangle(dt1): " + dt1.getEdge(0).getOtherTriangle(dt1) + "\ndt1.getEdge(1).getOtherTriangle(dt1): " + dt1.getEdge(1).getOtherTriangle(dt1) + "\ndt1.getEdge(2).getOtherTriangle(dt1): " + dt1.getEdge(2).getOtherTriangle(dt1));
                            if (dt1 != dt2 && ((dt1.getEdge(0).getOtherTriangle(dt1) != null && dt1.getEdge(0).getOtherTriangle(dt1).equals(dt2)) ||
                                    (dt1.getEdge(1).getOtherTriangle(dt1) != null && dt1.getEdge(1).getOtherTriangle(dt1).equals(dt2)) || (dt1.getEdge(2).getOtherTriangle(dt1) != null && dt1.getEdge(2).getOtherTriangle(dt1).equals(dt2)))) {
                                pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt1)][traversalHandler.getTriangles().indexOf(dt2)] = 1;
                                pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt2)][traversalHandler.getTriangles().indexOf(dt1)] = 1;
                            }
                        }
                    }
                }
            }
            currentLayer = nextLayer;
            if (nextLayer.size() == 0) {
                unexploredLeft = false;
            }
            first = false;
        }

        for (DTriangle dt1 : pbtClone) {
            for (DTriangle dt2 : pbtClone) {
                //System.out.println("dt1: " + dt1 + "\ndt2: " + dt2 + "\ndt1.getEdge(0).getOtherTriangle(dt1): " + dt1.getEdge(0).getOtherTriangle(dt1) + "\ndt1.getEdge(1).getOtherTriangle(dt1): " + dt1.getEdge(1).getOtherTriangle(dt1) + "\ndt1.getEdge(2).getOtherTriangle(dt1): " + dt1.getEdge(2).getOtherTriangle(dt1));
                if (dt1 != dt2 && ((dt1.getEdge(0).getOtherTriangle(dt1) != null && dt1.getEdge(0).getOtherTriangle(dt1).equals(dt2)) ||
                        (dt1.getEdge(1).getOtherTriangle(dt1) != null && dt1.getEdge(1).getOtherTriangle(dt1).equals(dt2)) || (dt1.getEdge(2).getOtherTriangle(dt1) != null && dt1.getEdge(2).getOtherTriangle(dt1).equals(dt2)))) {
                    pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt1)][traversalHandler.getTriangles().indexOf(dt2)] = 1;
                    pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt2)][traversalHandler.getTriangles().indexOf(dt1)] = 1;
                }
            }
        }

        Polygon p;
        for (DTriangle dt : pocketBoundaryTriangles) {
            p = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
            p.setFill(Color.BLUE.deriveColor(1, 1, 1, 0.1));
            catchGraphics.getChildren().add(p);
        }
        return new Tuple<>(pocketBoundaryTriangles, pocketAdjacencyMatrix);
    }

    // ******************************************************************************************************************************** //
    // Methods for initial computations (before task assignment)
    // ******************************************************************************************************************************** //

    private ArrayList<ArrayList<DTriangle>> computeGuardingSquareIntersection(ArrayList<SquareGuardManager> squareGuardManagers, ArrayList<DTriangle> triangles) {
        LinearRing temp;
        Geometry intersection;
        Coordinate first;
        ArrayList<Color> colors = new ArrayList<>();
        ArrayList<ArrayList<DTriangle>> guardingSquareIntersectingTriangles = new ArrayList<>(squareGuardManagers.size());
        for (int i = 0; i < squareGuardManagers.size(); i++) {
            guardingSquareIntersectingTriangles.add(new ArrayList<>());
            colors.add(new Color(Math.random(), Math.random(), Math.random(), 0.5));
        }
        for (DTriangle dt : triangles) {
            first = new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY());
            temp = new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                    first,
                    new Coordinate(dt.getPoint(1).getX(), dt.getPoint(1).getY()),
                    new Coordinate(dt.getPoint(2).getX(), dt.getPoint(2).getY()),
                    first
            }), GeometryOperations.factory);
            for (int i = 0; i < squareGuardManagers.size(); i++) {
                //intersection = squareGuardManagers.get(i).getGuardedSquare().intersection(temp);
                //System.out.println(triangles.indexOf(dt) + " - intersection size: " + intersection.getCoordinates().length + ", array: " + Arrays.toString(intersection.getCoordinates()));
                if (squareGuardManagers.get(i).getGuardedSquare().crosses(temp) || squareGuardManagers.get(i).getGuardedSquare().covers(temp)) {
                    guardingSquareIntersectingTriangles.get(i).add(dt);
                    /*Polygon p = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                    //p.setFill(Color.YELLOW.deriveColor(Math.random(), 1.0, 1.0, 1.0));
                    p.setFill(colors.get(i));
                    Main.pane.getChildren().add(p);
                    for (Coordinate c : intersection.getCoordinates()) {
                        Main.pane.getChildren().add(new Circle(c.x, c.y, 3, Color.BLACK));
                    }*/
                }
            }
        }
        return guardingSquareIntersectingTriangles;
    }

    private Tuple<ArrayList<ArrayList<Line>>, ArrayList<Shape>> computeComponentBoundaries(ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<DEdge> separatingEdges, ArrayList<Line> separatingLines) {
        System.out.println("simplyConnectedComponents.size(): " + simplyConnectedComponents.size());
        ArrayList<ArrayList<Line>> boundaryLines = new ArrayList<>();
        componentBoundaryEdges = new ArrayList<>();
        ArrayList<Shape> componentShapes = new ArrayList<>();
        ArrayList<Line> temp;
        ArrayList<DEdge> tempEdges, tempComponentBoundaryEdges;
        Shape tempShape;
        for (ArrayList<DTriangle> arr : simplyConnectedComponents) {
            temp = new ArrayList<>();
            tempEdges = new ArrayList<>();
            tempComponentBoundaryEdges = new ArrayList<>();
            tempShape = new Polygon(0, 0);
            for (DTriangle dt : arr) {
                tempEdges.addAll(Arrays.asList(dt.getEdges()));
                tempShape = Shape.union(tempShape, new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY()));
            }
            componentShapes.add(tempShape);
            /*tempShape.setFill(Color.BLACK.brighter().brighter().brighter().brighter());
            Main.pane.getChildren().add(tempShape);*/
            for (DEdge de : tempEdges) {
                if (tempEdges.indexOf(de) == tempEdges.lastIndexOf(de) /*|| separatingEdges.contains(de)*/) {
                    temp.add(new Line(de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY()));
                    tempComponentBoundaryEdges.add(de);
                    /*Line l = new Line(de.getPointLeft().getEstX(), de.getPointLeft().getEstY(), de.getPointRight().getEstX(), de.getPointRight().getEstY());
                    l.setStroke(Color.BLUE);
                    l.setStrokeWidth(2);
                    Main.pane.getChildren().add(l);*/
                } else if (separatingEdges.contains(de)) {
                    temp.add(separatingLines.get(separatingEdges.indexOf(de)));
                    tempComponentBoundaryEdges.add(de);
                }
            }
            boundaryLines.add(temp);
            componentBoundaryEdges.add(tempComponentBoundaryEdges);
        }
        Polygon p;
        /*for (ArrayList<Line> arr : boundaryLines) {
            temp = (ArrayList<Line>) arr.clone();
            p = new Polygon();
            p.getPoints().addAll(
                    temp.get(0).getStartX(), temp.get(0).getStartY(),
                    temp.get(0).getEndX(), temp.get(0).getEndY()
            );
            temp.remove(0);
            while (!temp.isEmpty()) {
                boolean found = false;
                for (int j = 0; !found && j < temp.size(); j++) {
                    if (temp.get(j).getStartX() == p.getPoints().get(p.getPoints().size() - 2) && temp.get(j).getStartY() == p.getPoints().get(p.getPoints().size() - 1)) {
                        p.getPoints().addAll(temp.get(j).getEndX(), temp.get(j).getEndY());
                        temp.remove(j);
                        found = true;
                    } else if (temp.get(j).getEndX() == p.getPoints().get(p.getPoints().size() - 2) && temp.get(j).getEndY() == p.getPoints().get(p.getPoints().size() - 1)) {
                        p.getPoints().addAll(temp.get(j).getStartX(), temp.get(j).getStartY());
                        temp.remove(j);
                        found = true;
                    }
                }
                System.out.println("Doing line stuff");
            }
            componentShapes.add(p);
            p.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.2));
            p.setStroke(Color.RED);
            Main.pane.getChildren().add(p);
        }*/
        return new Tuple<>(boundaryLines, componentShapes);
    }

    private Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> computeGuardingLines(ArrayList<DTriangle> separatingTriangles, ArrayList<DEdge> nonSeparatingEdges) {
        // for now its enough to just cover one side of each separating triangle because they are computed to have one edge adjacent to a polygon (i.e. they have degree 2 in the dual triangulation graph)
        ArrayList<Line> separatingLines = new ArrayList<>();
        separatingEdges = new ArrayList<>();
        ArrayList<DEdge> reconnectingEdges = new ArrayList<>();
        double minLength, maxLength, currentLengthSquared;
        DEdge minLengthEdge, maxLengthEdge;
        for (DTriangle dt : separatingTriangles) {
            minLength = Double.MAX_VALUE;
            maxLength = -Double.MAX_VALUE;
            minLengthEdge = null;
            maxLengthEdge = null;
            for (DEdge de : dt.getEdges()) {
                if (!nonSeparatingEdges.contains(de)) {
                    currentLengthSquared = de.getSquared2DLength();
                    if (currentLengthSquared < minLength) {
                        minLength = currentLengthSquared;
                        minLengthEdge = de;
                    }
                    if (currentLengthSquared > maxLength) {
                        maxLength = currentLengthSquared;
                        maxLengthEdge = de;
                    }
                }
            }
            separatingLines.add(new Line(minLengthEdge.getPointLeft().getX(), minLengthEdge.getPointLeft().getY(), minLengthEdge.getPointRight().getX(), minLengthEdge.getPointRight().getY()));
            /*Line l = new Line(minLengthEdge.getPointLeft().getX(), minLengthEdge.getPointLeft().getY(), minLengthEdge.getPointRight().getX(), minLengthEdge.getPointRight().getY());
            l.setStrokeWidth(6);
            l.setFill(Color.ALICEBLUE);
            Main.pane.getChildren().add(l);*/
            separatingEdges.add(minLengthEdge);
            reconnectingEdges.add(maxLengthEdge);
        }
        return new Triplet<>(separatingLines, reconnectingEdges, separatingEdges);
    }

    private Tuple<int[][], ArrayList<ArrayList<DTriangle>>> computeReconnectedAdjacency(ArrayList<DTriangle> triangles, ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<DEdge> reconnectingEdges, int[][] disconnectedAdjacencyMatrix, ArrayList<DTriangle> separatingTriangles) {
        int[][] reconnectedAdjacencyMatrix = new int[disconnectedAdjacencyMatrix.length][disconnectedAdjacencyMatrix.length];
        for (int i = 0; i < disconnectedAdjacencyMatrix.length; i++) {
            System.arraycopy(disconnectedAdjacencyMatrix[i], 0, reconnectedAdjacencyMatrix[i], 0, disconnectedAdjacencyMatrix.length);
        }
        ArrayList<ArrayList<DTriangle>> reconnectedComponents = new ArrayList<>();
        ArrayList<DTriangle> temp;
        for (ArrayList<DTriangle> al : simplyConnectedComponents) {
            temp = new ArrayList<>();
            temp.addAll(al);
            reconnectedComponents.add(temp);
        }
        int index1, index2, connectingTriangleIndex, reconnectingTriangleIndex;
        for (DEdge de : reconnectingEdges) {
            index1 = triangles.indexOf(de.getLeft());
            index2 = triangles.indexOf(de.getRight());
            /*for (int i = 0; i < triangles.size(); i++) {
                if (triangles.get(i).isEdgeOf(de)) {
                    if (index1 == -1) {
                        index1 = i;
                        Polygon p = new Polygon(triangles.get(i).getPoint(0).getX(), triangles.get(i).getPoint(0).getY(), triangles.get(i).getPoint(1).getX(), triangles.get(i).getPoint(1).getY(), triangles.get(i).getPoint(2).getX(), triangles.get(i).getPoint(2).getY());
                        p.setFill(Color.LAWNGREEN);
                        Main.pane.getChildren().add(p);
                    } else {
                        index2 = i;
                        Polygon p = new Polygon(triangles.get(i).getPoint(0).getX(), triangles.get(i).getPoint(0).getY(), triangles.get(i).getPoint(1).getX(), triangles.get(i).getPoint(1).getY(), triangles.get(i).getPoint(2).getX(), triangles.get(i).getPoint(2).getY());
                        p.setFill(Color.ORANGE);
                        Main.pane.getChildren().add(p);
                    }
                }
            }*/
            reconnectedAdjacencyMatrix[index1][index2] = 1;
            reconnectedAdjacencyMatrix[index2][index1] = 1;

            if (!separatingTriangles.contains(triangles.get(index1))) {
                connectingTriangleIndex = index1;
                reconnectingTriangleIndex = index2;
            } else {
                connectingTriangleIndex = index2;
                reconnectingTriangleIndex = index1;
            }
            for (ArrayList<DTriangle> al : reconnectedComponents) {
                if (al.contains(triangles.get(connectingTriangleIndex))) {
                    al.add(triangles.get(reconnectingTriangleIndex));
                }
            }
        }
        return new Tuple<>(reconnectedAdjacencyMatrix, reconnectedComponents);
    }

    private Tuple<ArrayList<DTriangle>, ArrayList<DTriangle>> triangulate(MapRepresentation map) throws DelaunayError {
        ArrayList<Polygon> mapPolygons = map.getAllPolygons();
        ArrayList<DEdge> constraintEdges = new ArrayList<>();
        for (Polygon p : mapPolygons) {
            if (p != null) {
                for (int i = 0; i < p.getPoints().size(); i += 2) {
                    constraintEdges.add(new DEdge(new DPoint(p.getPoints().get(i), p.getPoints().get(i + 1), 0), new DPoint(p.getPoints().get((i + 2) % p.getPoints().size()), p.getPoints().get((i + 3) % p.getPoints().size()), 0)));
                }
            }
        }
        ConstrainedMesh mesh = new ConstrainedMesh();
        mesh.setConstraintEdges(constraintEdges);
        mesh.processDelaunay();

        List<DTriangle> triangles = mesh.getTriangleList();
        ArrayList<DTriangle> includedTriangles = new ArrayList<>();
        ArrayList<DTriangle> holeTriangles = new ArrayList<>();
        for (DTriangle dt : triangles) {
            // check if triangle in polygon
            double centerX = dt.getBarycenter().getX();
            double centerY = dt.getBarycenter().getY();
            boolean inPolygon = true;
            boolean inHole = false;
            if (!mapPolygons.get(0).contains(centerX, centerY)) {
                inPolygon = false;
            }
            for (int i = 1; inPolygon && i < mapPolygons.size(); i++) {
                if (mapPolygons.get(i).contains(centerX, centerY)) {
                    inPolygon = false;
                    inHole = true;
                }
            }
            if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                inPolygon = false;
            }
            if (inPolygon) {
                includedTriangles.add(dt);
            }
            if (inHole) {
                holeTriangles.add(dt);
            }
        }
        return new Tuple<>(includedTriangles, holeTriangles);
    }

    private Tuple<int[][], int[]> computeAdjacency(ArrayList<DTriangle> nodes) {
        int[][] originalAdjacencyMatrix = new int[nodes.size()][nodes.size()];

        // checking for adjacency between nodes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        DEdge de;
        for (int i = 0; i < nodes.size(); i++) {
            dt1 = nodes.get(i);
            // go through the edges of each triangle
            for (int j = 0; j < 3; j++) {
                de = dt1.getEdge(j);
                if (!checkedEdges.contains(de)) {
                    int neighbourIndex = -1;
                    for (int k = 0; neighbourIndex == -1 && k < nodes.size(); k++) {
                        dt2 = nodes.get(k);
                        if (k != i && dt2.isEdgeOf(de)) {
                            // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                            neighbourIndex = k;
                        }
                    }
                    if (neighbourIndex != -1) {
                        originalAdjacencyMatrix[i][neighbourIndex] = 1;
                        originalAdjacencyMatrix[neighbourIndex][i] = 1;
                    }
                    checkedEdges.add(de);
                }
            }
        }

        int[] degreeMatrix = new int[nodes.size()];
        int degreeCount;
        for (int i = 0; i < nodes.size(); i++) {
            degreeCount = 0;
            for (int j = 0; j < nodes.size(); j++) {
                if (originalAdjacencyMatrix[i][j] == 1) {
                    degreeCount++;
                }
            }
            degreeMatrix[i] = degreeCount;
        }

        return new Tuple<>(originalAdjacencyMatrix, degreeMatrix);
    }

    private ArrayList<ArrayList<DTriangle>> computeHoles(ArrayList<DTriangle> holeTriangles) {
        // 1. group hole triangles by hole
        // 2. choose degree-2 triangle adjacent to the hole (can be according to some criterion)
        // 3. ???
        // 4. profit

            /*
            for each triangle, find all the triangles that are connected to it from holeTriangles
            then do the same for the triangles added in that iteration
            start with the first hole/triangle
            find all its immediate neighbours from holeTriangles (and remove them from that list)
            find their immediate neighbours (and so forth) until no new triangles are added to the hole
             */

        // checking for adjacency of triangles in the holes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1;
        ArrayList<ArrayList<DTriangle>> holes = new ArrayList<>();
        ArrayList<DTriangle> temp;
        for (int i = 0; i < holeTriangles.size(); i++) {
            dt1 = holeTriangles.get(i);
            temp = new ArrayList<>();
            temp.add(dt1);
            holeTriangles.remove(dt1);

            boolean addedToHole = true;
            while (addedToHole) {
                addedToHole = false;
                // go through the remaining triangles and see if they are adjacent to the ones already in the hole
                for (int j = 0; j < temp.size(); j++) {
                    for (int k = 0; k < holeTriangles.size(); k++) {
                        if (temp.get(j) != holeTriangles.get(k) && (holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(0)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(1)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(2)))) {
                            temp.add(holeTriangles.get(k));
                            holeTriangles.remove(k);
                            addedToHole = true;
                            k--;
                        }
                    }
                }
            }
            holes.add(temp);
            i--;
        }

        System.out.println("Nr. holes: " + holes.size());
        for (ArrayList<DTriangle> hole : holes) {
            System.out.println("Hole with " + hole.size() + (hole.size() > 1 ? " triangles" : " triangle"));
        }

        Polygon tempTriangle;
        Color currentColor;
        for (ArrayList<DTriangle> hole : holes) {
            //currentColor = new Color(Math.random(), Math.random(), Math.random(), 0.7);
            currentColor = Color.rgb(255, 251, 150, 0.4);
            for (DTriangle dt : hole) {
                tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                tempTriangle.setFill(currentColor);
                tempTriangle.setStroke(Color.GREY);
                Main.pane.getChildren().add(tempTriangle);
            }
        }

        return holes;
    }

    private Triplet<ArrayList<DTriangle>, ArrayList<DEdge>, int[][]> computeSeparatingTriangles(ArrayList<DTriangle> nodes, ArrayList<ArrayList<DTriangle>> holes, int[][] originalAdjacencyMatrix, int[] degreeMatrix) {
        // separating triangles are those adjacent to a hole and (for now) degree 2
        ArrayList<DTriangle> separatingTriangles = new ArrayList<>();
        ArrayList<DEdge> nonSeparatingEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        for (ArrayList<DTriangle> hole : holes) {
            boolean triangleFound = false;
            // go through triangles in the hole
            for (int i = 0; !triangleFound && i < hole.size(); i++) {
                dt1 = hole.get(i);
                // go through triangles outside the hole
                for (int j = 0; !triangleFound && j < nodes.size(); j++) {
                    dt2 = nodes.get(j);
                    // if the triangle has degree two and is adjacent to the holeTriangle, make it a separating triangle
                    if (degreeMatrix[j] == 2 && (dt2.isEdgeOf(dt1.getEdge(0)) || dt2.isEdgeOf(dt1.getEdge(1)) || dt2.isEdgeOf(dt1.getEdge(2)))) {
                        int vertexCount = 0;
                        for (DTriangle holeTriangle : hole) {
                            for (int k = 0; k < dt2.getPoints().size(); k++) {
                                if (holeTriangle.getPoints().contains(dt2.getPoint(k))) {
                                    vertexCount++;
                                }
                            }
                        }
                        int what = 0;
                        for (int z = 0; z < separatingTriangles.size(); z++) {
                            for (int k = 0; k < dt2.getPoints().size(); k++) {
                                if (separatingTriangles.get(z).getPoints().contains(dt2.getPoint(k))) {
                                    what++;
                                }
                            }
                        }
                        System.out.println("vertexCount = " + vertexCount);
                        if (vertexCount <= 4 && what < 2) {
                            separatingTriangles.add(dt2);
                            if (dt2.isEdgeOf(dt1.getEdge(0))) {
                                nonSeparatingEdges.add(dt1.getEdge(0));
                            }
                            if (dt2.isEdgeOf(dt1.getEdge(1))) {
                                nonSeparatingEdges.add(dt1.getEdge(1));
                            }
                            if (dt2.isEdgeOf(dt1.getEdge(2))) {
                                nonSeparatingEdges.add(dt1.getEdge(2));
                            }
                            triangleFound = true;
                        }
                    }
                }
            }
        }
        // if they form a loop, then change one?
        // run spanning tree on the generated graph and see whether branches "meet"
        // if so, break that loop either by adding a separating triangle or by changing a separating triangle
        // also, could just use this from the start?

        //int[][] spanningTreeAdjacencyMatrix = originalAdjacencyMatrix.clone();
        int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
        for (int i = 0; i < originalAdjacencyMatrix.length; i++) {
            System.arraycopy(originalAdjacencyMatrix[i], 0, spanningTreeAdjacencyMatrix[i], 0, originalAdjacencyMatrix[0].length);
        }
        for (DTriangle dt : separatingTriangles) {
            for (int i = 0; i < nodes.size(); i++) {
                spanningTreeAdjacencyMatrix[nodes.indexOf(dt)][i] = 0;
                spanningTreeAdjacencyMatrix[i][nodes.indexOf(dt)] = 0;
            }
        }

        return new Triplet<>(separatingTriangles, nonSeparatingEdges, spanningTreeAdjacencyMatrix);
    }

    private Tuple<ArrayList<ArrayList<DTriangle>>, int[]> computeConnectedComponents(ArrayList<DTriangle> nodes, ArrayList<DTriangle> componentNodes, int[][] spanningTreeAdjacencyMatrix) {
        ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = new ArrayList<>();
        ArrayList<DTriangle> temp;
        ArrayList<Integer> currentLayer, nextLayer;
        boolean[] visitedNodes = new boolean[nodes.size()];
        int[] componentParentNodes = new int[nodes.size()];
        boolean unexploredLeft;
        for (int i = 0; i < componentNodes.size(); i++) {
            temp = new ArrayList<>();
            temp.add(componentNodes.get(i));

            unexploredLeft = true;
            currentLayer = new ArrayList<>();
                        /*System.out.println("i: " + i);
                        System.out.println("componentNodes.size(): " + componentNodes.size());*/
            currentLayer.add(nodes.indexOf(componentNodes.get(i)));
            componentNodes.remove(i);
            while (unexploredLeft) {
                nextLayer = new ArrayList<>();
                for (int j : currentLayer) {
                    visitedNodes[j] = true;
                    for (int k = 0; k < componentNodes.size(); k++) {
                        if (spanningTreeAdjacencyMatrix[j][nodes.indexOf(componentNodes.get(k))] == 1 && nodes.indexOf(componentNodes.get(k)) != componentParentNodes[j] && !visitedNodes[nodes.indexOf(componentNodes.get(k))]) {
                            nextLayer.add(nodes.indexOf(componentNodes.get(k)));
                            componentParentNodes[nodes.indexOf(componentNodes.get(k))] = j;
                            visitedNodes[nodes.indexOf(componentNodes.get(k))] = true;
                            temp.add(componentNodes.get(k));
                            componentNodes.remove(k);
                            k--;
                        }
                    }
                }
                currentLayer = nextLayer;
                if (nextLayer.size() == 0) {
                    unexploredLeft = false;
                }
            }
            simplyConnectedComponents.add(temp);
            i--;
        }
        return new Tuple<>(simplyConnectedComponents, componentParentNodes);
    }

    private void computeSingleConnectedComponent(ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<ArrayList<DTriangle>> holes, ArrayList<DTriangle> nodes, ArrayList<DTriangle> separatingTriangles, int[][] spanningTreeAdjacencyMatrix, int[][] originalAdjacencyMatrix, int[] parentNodes, ArrayList<Line> tree) throws DelaunayError {
        if (parentNodes == null) {
            return;
        }
        if (simplyConnectedComponents.size() == 2) {
            simplyConnectedComponents.sort((o1, o2) -> o1.size() > o2.size() ? -1 : (o1.size() == o2.size() ? 0 : 1));
            //System.out.println("simplyConnectedComponents.size(): " + simplyConnectedComponents.size());

            // now cut through any possible loops:
            // want a triangle adjacent to one of the enclosing triangles for each cut-off region
            // (and on the outside) that is also part of the loop -> how can you identify that?
            // 1 could check whether everything is still reachable from the adjacent nodes of the one you want to make a separator
            // 2 2.1 search for nodes in the tree which are not
            //       a) separating triangles
            //       b) parent and child
            //       but are still adjacent in the separating tree graph
            //   2.2 then trace their paths back to the last common ancestor
            //       -> take path from one back to the root and store nodes, then take path from the other until you meet one of the nodes
            //       -> thereby automatically iterate over all of the nodes on the loop
            //   2.3 find a hole that has a triangle on the loop adjacent to it, then move the current adjacent
            //       separating triangle to that new-found triangle (can use some other criterion for selection too)

                        /*for (int i = 0; i < adjacencyMatrix.length; i++) {
                            System.out.print(i + " | ");
                            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                                System.out.print(adjacencyMatrix[i][j] + " ");
                            }
                            System.out.println();
                        }*/

            // finding pairs of adjacent nodes that are not parent and child (and thus form a loop)
            ArrayList<int[]> adjacentPairs = new ArrayList<>();
            for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                for (int j = 0; j < i; j++) {
                    if (spanningTreeAdjacencyMatrix[i][j] == 1 && !(parentNodes[i] == j || parentNodes[j] == i)) {
                        adjacentPairs.add(new int[]{i, j});
                        System.out.println(i + " and " + j + " adjacent but not parent and child");
                    }
                }
            }

            // trace the paths of the pair back to the last common ancestor
            ArrayList<ArrayList<Integer>> loops = new ArrayList<>();
            ArrayList<Integer>[] tempIndeces;
            for (int[] adjacentPair : adjacentPairs) {
                tempIndeces = new ArrayList[2];
                tempIndeces[0] = new ArrayList<>();
                tempIndeces[0].add(adjacentPair[0]);
                int currentParent = parentNodes[adjacentPair[0]];
                while (currentParent != -1) {
                    tempIndeces[0].add(currentParent);
                    currentParent = parentNodes[currentParent];
                }

                tempIndeces[1] = new ArrayList<>();
                tempIndeces[1].add(adjacentPair[1]);
                currentParent = parentNodes[adjacentPair[1]];
                while (!tempIndeces[0].contains(currentParent)) {
                    tempIndeces[1].add(currentParent);
                    currentParent = parentNodes[currentParent];
                }

                int commonAncestor = currentParent;
                int currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                while (currentIndex != commonAncestor) {
                    tempIndeces[0].remove(tempIndeces[0].size() - 1);
                    currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                }

                for (int j = tempIndeces[1].size() - 1; j >= 0; j--) {
                    tempIndeces[0].add(tempIndeces[1].get(j));
                }
                loops.add(tempIndeces[0]);
            }

            for (int i = 0; i < loops.size(); i++) {
                if (loops.get(i).size() <= 3) {
                    loops.remove(i);
                    i--;
                }
            }

            DTriangle dt1, dt2, dt3;
            if (loops.size() == 1) {
                // find a hole adjacent to both the loop and the disconnected component
                // then use its separating triangle to break the loop and open up the disconnected component
                ArrayList<DTriangle> currentConnectedComponent = simplyConnectedComponents.get(1);
                ArrayList<Integer> loopBreakingCandidates = new ArrayList<>();
                ArrayList<DTriangle> currentHole;
                for (int z = 0; z < holes.size(); z++) {
                    currentHole = holes.get(z);
                    // see whether the hole is adjacent to the connected components
                    // i.e. (its separating triangle) could be used to "break up" the barrier enclosing that component
                    boolean disconnectedAdjacencyFound = false;
                    boolean loopAdjacencyFound = false;
                    for (int i = 0; (!disconnectedAdjacencyFound || !loopAdjacencyFound) && i < currentHole.size(); i++) {
                        dt1 = currentHole.get(i);
                        for (int j = 0; !disconnectedAdjacencyFound && j < currentConnectedComponent.size(); j++) {
                            dt2 = currentConnectedComponent.get(j);
                            for (int k = 0; !disconnectedAdjacencyFound && k < dt1.getPoints().size(); k++) {
                                if (dt2.isOnAnEdge(dt1.getPoint(k))) {
                                    disconnectedAdjacencyFound = true;
                                }
                            }
                        }
                        for (int j = 0; !loopAdjacencyFound && j < loops.get(0).size(); j++) {
                            dt3 = nodes.get(loops.get(0).get(j));
                            for (int k = 0; !loopAdjacencyFound && k < dt1.getEdges().length; k++) {
                                if (dt3.isEdgeOf(dt1.getEdge(k))) {
                                    loopAdjacencyFound = true;
                                }
                            }
                        }
                    }
                    if (disconnectedAdjacencyFound && loopAdjacencyFound) {
                        loopBreakingCandidates.add(z);
                    }
                }
                System.out.println("loopBreakingCandidates.size(): " + loopBreakingCandidates.size());

                if (loopBreakingCandidates.size() != 0) {
                    // change separating triangle
                    // find a triangle adjacent to the loop
                    DTriangle newSeparatingTriangle = null;
                    DTriangle oldSeparatingTriangle = separatingTriangles.get(loopBreakingCandidates.get(0));
                    boolean newSeparatingTriangleFound = false;
                    for (int i = 0; !newSeparatingTriangleFound && i < holes.get(loopBreakingCandidates.get(0)).size(); i++) {
                        dt1 = holes.get(loopBreakingCandidates.get(0)).get(i);
                        for (int j = 0; !newSeparatingTriangleFound && j < loops.get(0).size(); j++) {
                            dt2 = nodes.get(loops.get(0).get(j));
                            for (int k = 0; !newSeparatingTriangleFound && k < dt1.getEdges().length; k++) {
                                if (dt2.isEdgeOf(dt1.getEdge(k))) {
                                    newSeparatingTriangle = dt2;
                                    newSeparatingTriangleFound = true;
                                }
                            }
                        }
                    }
                    if (newSeparatingTriangleFound) {
                        separatingTriangles.set(loopBreakingCandidates.get(0), newSeparatingTriangle);
                        // updating the adjacency matrix
                        for (int i = 0; i < nodes.size(); i++) {
                            spanningTreeAdjacencyMatrix[nodes.indexOf(newSeparatingTriangle)][i] = 0;
                            spanningTreeAdjacencyMatrix[i][nodes.indexOf(newSeparatingTriangle)] = 0;
                        }
                    } else {
                        System.out.println("No new separating triangle found.");
                    }

                    // updating the adjacency matrix
                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        if (originalAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] == 1 || originalAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] == 1) {
                            spanningTreeAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] = 1;
                            spanningTreeAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] = 1;
                            System.out.println("Happens");
                        }
                    }

                    // visuals, aye
                    Line tempLine;
                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        for (int j = 0; j < i; j++) {
                            if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                                tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                tempLine.setStroke(Color.RED);
                                tempLine.setStrokeWidth(4);
                                tree.add(tempLine);
                            }
                        }
                    }
                }
            } else {
                System.out.println("There are " + (loops.size() > 1 ? "multiple" : "no") + "loops.");
            }
        } else {
            System.out.println("There are " + (simplyConnectedComponents.size() == 1 ? "no more" : simplyConnectedComponents.size()) + " simply-connected components.");
        }
    }

    private ArrayList<Line> showSpanningTree(ArrayList<DTriangle> nodes, int[][] spanningTreeAdjacencyMatrix) throws DelaunayError {
        boolean unexploredLeft = true;
        ArrayList<Integer> currentLayer = new ArrayList<>();
        currentLayer.add(0);
        ArrayList<Integer> nextLayer;

        boolean[] visitedNodes = new boolean[nodes.size()];
        int[] parentNodes = new int[nodes.size()];
        parentNodes[0] = -1;

        ArrayList<Line> tree = new ArrayList<>();
        Line tempLine;
        while (unexploredLeft) {
            nextLayer = new ArrayList<>();
            for (int i : currentLayer) {
                visitedNodes[i] = true;
                for (int j = 0; j < nodes.size(); j++) {
                    if (spanningTreeAdjacencyMatrix[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                        nextLayer.add(j);
                        parentNodes[j] = i;
                        visitedNodes[j] = true;

                        tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                        tempLine.setStroke(Color.RED);
                        tempLine.setStrokeWidth(4);
                        tree.add(tempLine);
                    }
                }
            }
            currentLayer = nextLayer;
            if (nextLayer.size() == 0) {
                unexploredLeft = false;
            }
        }

        for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
            int c = 0;
            for (int j = 0; j < spanningTreeAdjacencyMatrix[0].length; j++) {
                if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                    c++;
                }
            }
            System.out.println("Node " + i + " has " + c + " neighbours");
        }

        return tree;
    }

    public static Agent testTarget;

}
