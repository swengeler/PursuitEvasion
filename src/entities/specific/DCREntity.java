package entities.specific;

import additionalOperations.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import entities.base.Entity;
import entities.base.PartitioningEntity;
import entities.utils.GuardManager;
import entities.utils.SquareGuardManager;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import pathfinding.ShortestPathRoadMap;
import simulation.*;
import ui.Main;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * DCR = Divide and Conquer, Randomised
 */
public class DCREntity extends PartitioningEntity {

    private static final boolean CONSTANT_TARGET_TEST = true;

    private enum Stage {
        CATCHER_TO_SEARCHER, FIND_TARGET, INIT_FIND_TARGET, FOLLOW_TARGET
    }

    private TraversalHandler traversalHandler;

    private Point2D origin, pseudoBlockingVertex, lastPointVisible;
    private boolean pocketCounterClockwise;
    private Stage currentStage;

    private Agent searcher, catcher;
    private PlannedPath currentSearcherPath, currentCatcherPath;
    private ArrayList<Line> pathLines;
    private int searcherPathLineCounter, catcherPathLineCounter;

    private ArrayList<ArrayList<DTriangle>> gSqrIntersectingTriangles;

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
    protected void determineTarget() {
        outer:
        for (Entity e : map.getEvadingEntities()) {
            if (e.isActive()) {
                for (Agent a : e.getControlledAgents()) {
                    if (a.isActive()) {
                        target = a;
                        for (GuardManager gm : guardManagers) {
                            gm.initTargetPosition(target);
                        }
                        break outer;
                    }
                }
            }
        }
    }

    @Override
    protected void doGuardOperations() {
        if (spottedOnce) {
            // TODO: additionally or alternatively keep track of guarding squares the evader is in and restrict the SPRM to exclude the uncrossable lines of those squares
            if ((inGuardedSquareOverNonSeparating || inGuardedSquareOverSeparating) && !currentGuardedSquare.inGuardedSquare(target.getXPos(), target.getYPos())) {
                System.out.println("Exited square");
                specialShortestPathRoadMap = null;
                currentGuardedSquare = null;
                inGuardedSquareOverNonSeparating = false;
                inGuardedSquareOverSeparating = false;
            } else /*if (map.isVisible(catcher, target))*/ {
                for (GuardManager gm : guardManagers) {
                    if (((SquareGuardManager) gm).crossedNonSeparatingLine()) {
                        currentGuardedSquare = (SquareGuardManager) gm;
                        inGuardedSquareOverNonSeparating = true;
                        inGuardedSquareOverSeparating = false;
                        System.out.println("Entered square over non-separating line");
                        break;
                    } else if (((SquareGuardManager) gm).crossedSeparatingLine()) {
                        currentGuardedSquare = (SquareGuardManager) gm;
                        inGuardedSquareOverNonSeparating = false;
                        inGuardedSquareOverSeparating = true;
                        System.out.println("Entered square over separating line");
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void doSearchAndCatchOperations() {
        //System.out.println(currentStage);
        if (currentStage == Stage.CATCHER_TO_SEARCHER) {
            catcherToSearcher();
        } else if (currentStage == Stage.INIT_FIND_TARGET) {
            initFindTarget();
        } else if (currentStage == Stage.FOLLOW_TARGET) {
            followTarget();
        } else if (currentStage == Stage.FIND_TARGET) {
            findTarget();
        }
    }

    protected void doOtherOperations() {}

    private void catcherToSearcher() {
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
    }

    private void initFindTarget() {
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
                    for (GuardManager gm : guardManagers) {
                        if (((SquareGuardManager) gm).inGuardedSquare(target.getXPos(), target.getYPos())) {
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
                            for (GuardManager gm : guardManagers) {
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
    }

    private void followTarget() {
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
        } /*else if (map.isVisible(target, catcher) && GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines)) {
                System.out.println("WANT TO GET CATCHER BACK TO SEARCHER");
                    *//*MapRepresentation.showVisible = true;
                    System.out.println("map.isVisible(catcher, searcher): " + map.isVisible(catcher, searcher));
                    System.out.println("map.isVisible(searcher, catcher): " + map.isVisible(searcher, catcher));
                    MapRepresentation.showVisible = false;
                    currentCatcherPath = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), searcher.getXPos(), searcher.getYPos());
                    System.out.println("currentCatcherPath: " + currentCatcherPath);*//*
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
            }*/ else {
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
                Line intersectedLine = null;
                for (Line line : componentBoundaryLines.get(componentIndex)) {
                    currentPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, line);
                    if (currentPoint != null && (currentLengthSquared = Math.pow(catcher.getXPos() - currentPoint.getX(), 2) + Math.pow(catcher.getYPos() - currentPoint.getY(), 2)) < minLengthSquared/*&& map.isVisible(catcher.getXPos(), catcher.getYPos(), pocketBoundaryEndPoint.getEstX(), pocketBoundaryEndPoint.getEstY())*/) {
                        minLengthSquared = currentLengthSquared;
                        pocketBoundaryEndPoint = currentPoint;
                        intersectedLine = line;
                        //Main.pane.getChildren().add(new Circle(currentPoint.getX(), currentPoint.getY(), 5, Color.DARKGRAY));
                        //found = true;
                        //break;
                    }/* else if (currentPoint != null) {
                                Main.pane.getChildren().add(new Circle(currentPoint.getEstX(), currentPoint.getEstY(), 2, Color.BLACK));
                            }*/
                }

                // TODO: possibly extend the pocket to include the intersected parts of the guarding square
                if (/*!found || */pocketBoundaryEndPoint == null) {
                    System.out.println("No pocket boundary end point found.");
                } else {
                    Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), catcher.getXPos(), catcher.getYPos());
                    catchGraphics.getChildren().add(boundaryLine);
                    catchGraphics.getChildren().add(new Circle(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), 5, Color.BLACK));

                    // find the new "pocket component"
                    System.out.printf("Catcher at (%f|%f)\nReal at (%f|%f)\nFake at (%f|%f)\n", catcher.getXPos(), catcher.getYPos(), currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY(), currentCatcherPath.getLastPathVertex().getEstX(), currentCatcherPath.getLastPathVertex().getEstY());
                    Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY(), separatingLines.contains(intersectedLine) ? intersectedLine : null);
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
    }

    private void findTarget() {
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
                Line intersectedLine = null;
                for (Line line : componentBoundaryLines.get(componentIndex)) {
                    currentPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, line);
                    if (currentPoint != null && (currentLengthSquared = Math.pow(catcher.getXPos() - currentPoint.getX(), 2) + Math.pow(catcher.getYPos() - currentPoint.getY(), 2)) < minLengthSquared/*&& map.isVisible(catcher.getXPos(), catcher.getYPos(), pocketBoundaryEndPoint.getEstX(), pocketBoundaryEndPoint.getEstY())*/) {
                        minLengthSquared = currentLengthSquared;
                        pocketBoundaryEndPoint = currentPoint;
                        intersectedLine = line;
                    }
                }
                Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                catchGraphics.getChildren().add(boundaryLine);
                catchGraphics.getChildren().add(new Circle(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), 6, Color.BLACK));
                Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), separatingLines.contains(intersectedLine) ? intersectedLine : null);
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

    @Override
    protected void assignTasks() {
        super.assignTasks();
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

    @Override
    protected void computeRequirements() {
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

            guardManagers = computeGuardManagers(separatingLines);

            gSqrIntersectingTriangles = computeGuardingSquareIntersection(guardManagers, nodes);

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

            for (GuardManager gm : guardManagers) {
                requiredAgents += gm.totalRequiredGuards();
            }
            requiredAgents += 2;
            System.out.println("\nrequiredAgents: " + requiredAgents);
        } catch (DelaunayError error) {
            error.printStackTrace();
        }
    }

    @Override
    protected ArrayList<GuardManager> computeGuardManagers(ArrayList<Line> separatingLines) {
        ArrayList<GuardManager> squareGuardManagers = new ArrayList<>(separatingLines.size());
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
        //guardManagers.add(squareGuardManager);

        /*Main.pane.getChildren().add(l);
        Main.pane.getChildren().add(new Line(lines[1].getCoordinate(0).x, lines[1].getCoordinate(0).y, lines[1].getCoordinate(1).x, lines[1].getCoordinate(1).y));
        Main.pane.getChildren().add(new Line(lines[2].getCoordinate(0).x, lines[2].getCoordinate(0).y, lines[2].getCoordinate(1).x, lines[2].getCoordinate(1).y));
        Main.pane.getChildren().add(new Line(lines[3].getCoordinate(0).x, lines[3].getCoordinate(0).y, lines[3].getCoordinate(1).x, lines[3].getCoordinate(1).y));*/
        return squareGuardManager;
    }

    private ArrayList<ArrayList<DTriangle>> computeGuardingSquareIntersection(ArrayList<GuardManager> guardManagers, ArrayList<DTriangle> triangles) {
        LinearRing temp;
        Geometry intersection;
        Coordinate first;
        ArrayList<Color> colors = new ArrayList<>();
        ArrayList<ArrayList<DTriangle>> guardingSquareIntersectingTriangles = new ArrayList<>(guardManagers.size());
        for (int i = 0; i < guardManagers.size(); i++) {
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
            for (int i = 0; i < guardManagers.size(); i++) {
                //intersection = guardManagers.get(i).getGuardedSquare().intersection(temp);
                //System.out.println(triangles.indexOf(dt) + " - intersection size: " + intersection.getCoordinates().length + ", array: " + Arrays.toString(intersection.getCoordinates()));
                if (((SquareGuardManager) guardManagers.get(i)).getGuardedSquare().crosses(temp) || ((SquareGuardManager) guardManagers.get(i)).getGuardedSquare().covers(temp)) {
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

    private Tuple<ArrayList<DTriangle>, int[][]> findPocketComponent(Line boundaryLine, int componentIndex, double pseudoBlockingVertX, double pseudoBlockingVertY, Line crossedSeparatingLine) {
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
        double distance0, distance1, distance2;
        Coordinate firstCoord;
        for (DTriangle dt : traversalHandler.getComponents().get(componentIndex)) {
            firstCoord = new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY());
            linearRing = new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                    firstCoord,
                    new Coordinate(dt.getPoint(1).getX(), dt.getPoint(1).getY()),
                    new Coordinate(dt.getPoint(2).getX(), dt.getPoint(2).getY()),
                    firstCoord
            }), GeometryOperations.factory);
            distance0 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(0).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(0).getY(), 2));
            distance1 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(1).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(1).getY(), 2));
            distance2 = Math.sqrt(Math.pow(pseudoBlockingVertX - dt.getPoint(2).getX(), 2) + Math.pow(pseudoBlockingVertY - dt.getPoint(2).getY(), 2));
            if (dt != currentTriangle && (linearRing.intersects(lineString)) && distance0 > minDistance && distance1 > minDistance && distance2 > minDistance) {
                plgn = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                plgn.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.1));
                catchGraphics.getChildren().add(plgn);
                pocketBoundaryTriangles.add(dt);
            }
        }

        // also go through guarding square triangles
        // get all triangles in the guard square which are intersected by the boundary line
        ArrayList<DTriangle> guardingSquareTriangles = null;
        if (crossedSeparatingLine != null) {
            System.out.println("Crossed separating line");
            // first identify the guarding square in question
            DEdge tempEdge = separatingEdges.get(separatingLines.indexOf(crossedSeparatingLine));
            DTriangle squareTriangle = pocketBoundaryTriangles.contains(tempEdge.getLeft()) ? tempEdge.getRight() : tempEdge.getLeft();
            guardingSquareTriangles = new ArrayList<>();
            for (ArrayList<DTriangle> arr : gSqrIntersectingTriangles) {
                if (arr.contains(squareTriangle)) {
                    guardingSquareTriangles = arr;
                    break;
                }
            }

            // find the extended boundary line (either ending at the square's other side or at the map boundary)
            double rayStartX = boundaryLine.getStartX();
            double rayStartY = boundaryLine.getStartY();
            double rayDeltaX = boundaryLine.getStartX() - boundaryLine.getEndX();
            double rayDeltaY = boundaryLine.getStartY() - boundaryLine.getEndY();
            rayStartX += rayDeltaX * 1E-8;
            rayStartY += rayDeltaY * 1E-8;

            double intersectionMinDistance = Double.MAX_VALUE, curIntersectionDistance;
            Point2D closestIntersectionPoint = null, currentIntersectionPoint;
            for (int i = 1; i < 4; i++) {
                closestIntersectionPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, ((SquareGuardManager) guardManagers.get(gSqrIntersectingTriangles.indexOf(guardingSquareTriangles))).getSquareSideLines().get(i));
            }

            if (closestIntersectionPoint != null) {
                intersectionMinDistance = Math.pow(closestIntersectionPoint.getX() - boundaryLine.getStartX(), 2) + Math.pow(closestIntersectionPoint.getY() - boundaryLine.getStartY(), 2);
            }

            for (ArrayList<Line> arr : componentBoundaryLines) {
                for (Line l : arr) {
                    currentIntersectionPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, l);
                    if (currentIntersectionPoint != null && (curIntersectionDistance = Math.pow(currentIntersectionPoint.getX() - boundaryLine.getStartX(), 2) + Math.pow(currentIntersectionPoint.getY() - boundaryLine.getStartY(), 2)) < intersectionMinDistance) {
                        intersectionMinDistance = curIntersectionDistance;
                        closestIntersectionPoint = currentIntersectionPoint;
                    }
                }
            }

            if (closestIntersectionPoint != null) {
                Main.pane.getChildren().add(new Circle(closestIntersectionPoint.getX(), closestIntersectionPoint.getY(), 5, Color.BLUE));

                // find out which triangles of the square are intersected
                lineString = new LineString(new CoordinateArraySequence(new Coordinate[]{new Coordinate(boundaryLine.getStartX(), boundaryLine.getStartY()), new Coordinate(closestIntersectionPoint.getX(), closestIntersectionPoint.getY())}), GeometryOperations.factory);
                for (DTriangle dt : guardingSquareTriangles) {
                    firstCoord = new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY());
                    linearRing = new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                            firstCoord,
                            new Coordinate(dt.getPoint(1).getX(), dt.getPoint(1).getY()),
                            new Coordinate(dt.getPoint(2).getX(), dt.getPoint(2).getY()),
                            firstCoord
                    }), GeometryOperations.factory);
                    if (linearRing.intersects(lineString)) {
                        pocketBoundaryTriangles.add(dt);
                    }
                }
            }
            // now have to make sure that triangles adjacent to these boundary triangles are actually also in the square
        }


        ArrayList<DTriangle> connectingTriangles = new ArrayList<>();
        ArrayList<DEdge> connectingEdges = new ArrayList<>();
        for (DTriangle dt1 : pocketBoundaryTriangles) {
            for (DEdge de : dt1.getEdges()) {
                boolean found = pocketBoundaryTriangles.contains(de.getOtherTriangle(dt1));
                /*boolean found = false;
                for (DTriangle dt2 : pocketBoundaryTriangles) {
                    if (dt1 != dt2 && dt2.isEdgeOf(de)) {
                        found = true;
                        break;
                    }
                }*/
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
                        Main.pane.getChildren().add(l);
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
        ArrayList<DTriangle> currentParents;
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
                        if (traversalHandler.getAdjacencyMatrix()[traversalHandler.getTriangles().indexOf(dt)][i] == 1 && !pocketBoundaryTriangles.contains(traversalHandler.getTriangles().get(i)) && (crossedSeparatingLine == null || !guardingSquareTriangles.contains(dt) || guardingSquareTriangles.contains(traversalHandler.getTriangles().get(i)) /*|| thing.contains(dt) is the parent of this dt in the guarding square */)) {
                            if (!first || (traversalHandler.getTriangles().get(i).equals(connectingEdges.get(connectingTriangles.indexOf(dt)).getOtherTriangle(dt)))) {
                                nextLayer.add(traversalHandler.getTriangles().get(i));
                                pocketAdjacencyMatrix[traversalHandler.getTriangles().indexOf(dt)][i] = 1;
                                pocketAdjacencyMatrix[i][traversalHandler.getTriangles().indexOf(dt)] = 1;
                            }
                        }
                    }
                }
                // find out whether the current triangle shares a separating edge with a guarded square
                if (crossedSeparatingLine == null || !guardingSquareTriangles.contains(dt)) {
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

}
