package entities.specific;

import additionalOperations.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import entities.base.Entity;
import entities.base.PartitioningEntity;
import entities.guarding.*;
import entities.utils.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import simulation.*;
import ui.Main;

import java.util.*;

/**
 * DCRL = Divide and Conquer, Randomised, Line Guards
 */
public class DCRLEntity extends PartitioningEntity {

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
    private ArrayList<PathLine> pathLines;
    private int searcherPathLineCounter, catcherPathLineCounter;

    private ArrayList<ArrayList<DTriangle>> gSqrIntersectingTriangles;

    private ShortestPathRoadMap testSPRM;
    private ArrayList<GuardManager> testGuardManagers;
    private ArrayList<Line> testExcludedLines;
    private ArrayList<Line> testCrossedLines;
    private ArrayList<Coordinate> testAddedCoordinates;
    private boolean testInGuardedSquare, updated, spottedOnce;

    private Coordinate previousTargetPosition, currentTargetPosition;
    private ArrayList<Line> initCrossableLines;
    private boolean initInGuardingSquare;

    private Group catchGraphics;
    private Group guardGraphics;

    public DCRLEntity(MapRepresentation map) {
        super(map);
        testGuardManagers = new ArrayList<>();
        testExcludedLines = new ArrayList<>();
        testCrossedLines = new ArrayList<>();
        testAddedCoordinates = new ArrayList<>();
        initCrossableLines = new ArrayList<>();
        catchGraphics = new Group();
        guardGraphics = new Group();
        Main.pane.getChildren().addAll(catchGraphics, guardGraphics);
        testExcludedLines.addAll(separatingLines);
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
                            if (((SquareGuardManager) gm).inGuardedSquare()) {
                                initCrossableLines.add(((SquareGuardManager) gm).getOriginalSeparatingLine());
                                testCrossedLines.add(initCrossableLines.get(initCrossableLines.size() - 1));
                                testExcludedLines.remove(initCrossableLines.get(initCrossableLines.size() - 1));
                                initInGuardingSquare = true;
                                //testInGuardedSquare = true;
                                System.out.println("target is in square: " + ((SquareGuardManager) gm).getGuardedSquare());
                            }
                        }
                        break outer;
                    }
                }
            }
        }
    }

    @Override
    protected void doPrecedingOperations() {
        if (target != null) {
            if (currentTargetPosition != null) {
                previousTargetPosition = currentTargetPosition;
            } else {
                previousTargetPosition = new Coordinate(target.getXPos(), target.getYPos());
            }
            currentTargetPosition = new Coordinate(target.getXPos(), target.getYPos());
        }
    }

    @Override
    protected void doGuardOperations() {
        if (spottedOnce) {
            int crossedLineOrdinal;
            boolean changed = false;
            SquareGuardManager gm;
            if (testInGuardedSquare) {
                // check whether the evader exited any of the guarded squares that it was in
                testInGuardedSquare = false;
                for (int i = 0; i < testGuardManagers.size(); i++) {
                    gm = (SquareGuardManager) testGuardManagers.get(i);
                    if (!gm.inGuardedSquare()) {
                        testGuardManagers.remove(gm);
                        testExcludedLines.removeAll(gm.getSquareSideLines());
                        testCrossedLines.removeAll(gm.getSquareSideLines());
                        if (!testExcludedLines.contains(gm.getOriginalSeparatingLine())) {
                            testExcludedLines.add(gm.getOriginalSeparatingLine());
                        }
                        testAddedCoordinates.remove(gm.getSquareSides().get(i).getCoordinates()[0]);
                        testAddedCoordinates.remove(gm.getSquareSides().get(i).getCoordinates()[1]);
                        changed = true;
                        i--;
                    } else {
                        //testInGuardedSquare = true;
                    }
                }
                for (GuardManager gmr : guardManagers) {
                    gm = (SquareGuardManager) gmr;
                    if (!testGuardManagers.contains(gm)) {
                        crossedLineOrdinal = gm.crossedLineOrdinal();
                        /*if (crossedLineOrdinal == 0 && ((SquareGuardManager) gmr).enteredSquare()) {
                            testExcludedLines.remove(((SquareGuardManager) gmr).getOriginalSeparatingLine());
                            testCrossedLines.add(((SquareGuardManager) gmr).getOriginalSeparatingLine());
                            testInGuardedSquare = true;
                            changed = true;
                        }*/
                        if (crossedLineOrdinal != -1 && gm.enteredSquare()) {
                            System.out.println("crossedLineOrdinal: " + crossedLineOrdinal);
                            testGuardManagers.add(gm);
                            for (int i = 0; i < 4; i++) {
                                if (i != crossedLineOrdinal) {
                                    if (!testExcludedLines.contains(gm.getSquareSideLines().get(i))) {
                                        testExcludedLines.add(gm.getSquareSideLines().get(i));
                                    }
                                } else {
                                    //if (!testCrossedLines.contains(gm.getSquareSideLines().get(i))) {
                                    testCrossedLines.add(gm.getSquareSideLines().get(i));
                                    //}
                                    testExcludedLines.remove(gm.getSquareSideLines().get(i));
                                    // TODO: also need to add new vertices to the graph, but only if they are in free space, i.e. not a coordinate of a separating line?
                                    if (!(gm.getSquareSides().get(0).getCoordinates()[0].equals2D(gm.getSquareSides().get(i).getCoordinates()[0]) || gm.getSquareSides().get(0).getCoordinates()[1].equals2D(gm.getSquareSides().get(i).getCoordinates()[0]))) {
                                        testAddedCoordinates.add(gm.getSquareSides().get(i).getCoordinates()[0]);
                                    }
                                    if (!(gm.getSquareSides().get(0).getCoordinates()[0].equals2D(gm.getSquareSides().get(i).getCoordinates()[1]) || gm.getSquareSides().get(0).getCoordinates()[1].equals2D(gm.getSquareSides().get(i).getCoordinates()[1]))) {
                                        testAddedCoordinates.add(gm.getSquareSides().get(i).getCoordinates()[1]);
                                    }
                                }
                            }
                            //testInGuardedSquare = true;
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    updated = true;
                } else {
                    updated = false;
                }
                if (!testInGuardedSquare) {
                    testSPRM = null;
                }
            } else {
                // check whether evader has entered a guarding square
                for (GuardManager gmr : guardManagers) {
                    gm = (SquareGuardManager) gmr;
                    if (!testGuardManagers.contains(gm)) {
                        crossedLineOrdinal = gm.crossedLineOrdinal();
                        /*if (crossedLineOrdinal == 0 && ((SquareGuardManager) gm).enteredSquare()) {
                            testExcludedLines.remove(((SquareGuardManager) gm).getOriginalSeparatingLine());
                            testCrossedLines.add(((SquareGuardManager) gm).getOriginalSeparatingLine());
                            testInGuardedSquare = true;
                            changed = true;
                        }*/
                        if (crossedLineOrdinal != -1 && gm.enteredSquare()) {
                            System.out.println("crossedLineOrdinal: " + crossedLineOrdinal);
                            testGuardManagers.add(gm);
                            for (int i = 0; i < 4; i++) {
                                if (i != crossedLineOrdinal) {
                                    if (!testExcludedLines.contains(gm.getSquareSideLines().get(i))) {
                                        testExcludedLines.add(gm.getSquareSideLines().get(i));
                                    }
                                } else {
                                    //if (!testCrossedLines.contains(gm.getSquareSideLines().get(i))) {
                                    testCrossedLines.add(gm.getSquareSideLines().get(i));
                                    //}
                                    testExcludedLines.remove(gm.getSquareSideLines().get(i));
                                    if (!(gm.getSquareSides().get(0).getCoordinates()[0].equals2D(gm.getSquareSides().get(i).getCoordinates()[0]) || gm.getSquareSides().get(0).getCoordinates()[1].equals2D(gm.getSquareSides().get(i).getCoordinates()[0]))) {
                                        testAddedCoordinates.add(gm.getSquareSides().get(i).getCoordinates()[0]);
                                    }
                                    if (!(gm.getSquareSides().get(0).getCoordinates()[0].equals2D(gm.getSquareSides().get(i).getCoordinates()[1]) || gm.getSquareSides().get(0).getCoordinates()[1].equals2D(gm.getSquareSides().get(i).getCoordinates()[1]))) {
                                        testAddedCoordinates.add(gm.getSquareSides().get(i).getCoordinates()[1]);
                                    }
                                }
                            }
                            //testInGuardedSquare = true;
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    updated = true;
                } else {
                    updated = false;
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

    @Override
    protected void doSucceedingOperations() {
        //previousTargetPosition = new Coordinate(catcher.getXPos(), catcher.getYPos());
        if (initInGuardingSquare) {
            for (int i = 0; i < initCrossableLines.size(); i++) {
                if (!((SquareGuardManager) guardManagers.get(separatingLines.indexOf(initCrossableLines.get(i)))).inGuardedSquare()) {
                    initCrossableLines.remove(initCrossableLines.get(i));
                    i--;
                    System.out.println("outside initial guarded square");
                } else {
                    System.out.println("not outside initial guarded square");
                }
            }
            if (initCrossableLines.size() == 0) {
                initInGuardingSquare = false;
            }
        }
    }

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
                if (target.isActive() && map.isVisible(target, searcher) /*&& !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos(), separatingLines) && !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos(), nastyBullshitLines)*/) {
                    System.out.println("Target found");
                    spottedOnce = true;
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
        /*boolean legal = true;
        for (Line l : separatingLines) {
            if (GeometryOperations.lineIntersect(l, target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos()) && ) {
                legal = false;
                break;
            }
        }*/
        LineSegment ls = new LineSegment(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
        LineSegment tempLs;
        boolean legal = true;
        //if (testInGuardedSquare) {
        for (Line l : separatingLines) {
            tempLs = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            if (tempLs.intersection(ls) != null && !testCrossedLines.contains(l)) {
                legal = false;
                break;
            }
        }
        //}

        /*ls = new LineSegment(previousTargetPosition, currentTargetPosition);
        DEdge tempEdge = null;
        for (Line l : initCrossableLines) {
            tempLs = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            if (ls.intersection(tempLs) != null) {
                tempEdge = separatingEdges.get(separatingLines.indexOf(l));
                System.out.println("tempEdge: " + tempEdge);
                break;
            }
        }
        try {
            if (tempEdge != null) {
                if (tempEdge.getLeft().contains(new DPoint(currentTargetPosition.x, currentTargetPosition.y, 0))) {
                    // make the left triangle the next "target" for search
                    currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), tempEdge.getLeft().getBarycenter().getX(), tempEdge.getLeft().getBarycenter().getY());
                    currentCatcherPath = null;
                } else {
                    // make the right triangle the next "target" for search
                    currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), tempEdge.getRight().getBarycenter().getX(), tempEdge.getRight().getBarycenter().getY());
                    currentCatcherPath = null;
                }
                currentStage = Stage.INIT_FIND_TARGET;
                return;
            }

        } catch (DelaunayError e) {
            e.printStackTrace();
        }*/


        if (map.isVisible(target, catcher) && length <= catcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER) {
            pseudoBlockingVertex = null;
            //System.out.println("pseudoBlockingVertex null because target visible in FOLLOW_TARGET (can capture)");
            lastPointVisible = null;
            catcher.moveBy(target.getXPos() - catcher.getXPos(), target.getYPos() - catcher.getYPos());
            target.setActive(false);
            target = null;
            origin = null;
            spottedOnce = false;
            initInGuardingSquare = false;
            currentStage = Stage.CATCHER_TO_SEARCHER;
        } else if (map.isVisible(target, catcher) /*&& legal/* || !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines))*/) {
            //System.out.println("Respotted (legal: " + legal + ", other metric: " + !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines) + ")");
            /*if (GeometryOperations.lineIntersectSeparatingLines(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos(), separatingEdges)) {
                System.out.println("Evader behind separating line");
                return;
            }*/

            pseudoBlockingVertex = null;
            lastPointVisible = null;

            // first case: target is visible
            // perform simple lion's move

            /*if (inGuardedSquareOverNonSeparating && specialShortestPathRoadMap == null) {
                ArrayList<Line> temp = new ArrayList<>();
                temp.add(currentGuardedSquare.getOriginalSeparatingLine());
                specialShortestPathRoadMap = new ShortestPathRoadMap(temp, map);
            } else if (inGuardedSquareOverSeparating && specialShortestPathRoadMap == null) {
                ArrayList<Line> temp = new ArrayList<>();
                for (int i = 1; i < currentGuardedSquare.getSquareSideLines().size(); i++) {
                    temp.add(currentGuardedSquare.getSquareSideLines().get(i));
                }
                specialShortestPathRoadMap = new ShortestPathRoadMap(temp, map);
            }*/

            if (testInGuardedSquare /*&& testSPRM == null*/ && (updated || testSPRM == null)) {
                testSPRM = new ShortestPathRoadMap(testExcludedLines, map);
                for (Line l : testExcludedLines) {
                    if (!Main.pane.getChildren().contains(l)) {
                        l.setStrokeWidth(5);
                        Main.pane.getChildren().add(l);
                    }
                }
                testSPRM.addExtraVertices(testAddedCoordinates);
            }

            PlannedPath temp;
            /*if (inGuardedSquareOverNonSeparating && specialShortestPathRoadMap != null) {
                temp = specialShortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
            } else if (!spottedOutsideGuardingSquare) {
                //temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), origin);
                temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
            } else if (inGuardedSquareOverSeparating && specialShortestPathRoadMap != null) {
                temp = specialShortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
            } else {
                temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), origin);
            }*/

            Line lionsMoveLine = null;
            if (testInGuardedSquare) {
                //temp = testSPRM.getShortestPath(target.getXPos(), target.getYPos(), origin);
                temp = testSPRM.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                temp.addPathToEnd(testSPRM.getShortestPath(catcher.getXPos(), catcher.getYPos(), origin));

                // TODO: need some other check to see whether we are may have to use the second path line for the lions move because reasons
                // should only do this if our view is even obstructed by one of the excluded lines
                boolean noLineCrossing = true;
                ls = new LineSegment(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                for (Line l : testExcludedLines) {
                    tempLs = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
                    if (tempLs.intersection(ls) != null) {
                        noLineCrossing = false;
                        break;
                    }
                }
                if (noLineCrossing) {
                    lionsMoveLine = temp.getPathLine(0);
                } else {
                    lionsMoveLine = temp.getPathLine(temp.getPathLines().size() - 1);
                }
            } else {
                //temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), origin);
                /*temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                temp.addPathToEnd(traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), origin));*/
                temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                temp.addPathToEnd(shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), origin));
                lionsMoveLine = temp.getPathLine(0);
            }

            temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
            temp.addPathToEnd(shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), origin));
            lionsMoveLine = temp.getPathLine(0);

            //PlannedPath temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), origin);
            //temp.draw();

            /*guardGraphics.getChildren().clear();
            lionsMoveLine.setStroke(Color.INDIANRED);
            guardGraphics.getChildren().add(lionsMoveLine);*/

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
        } /*else if (initInGuardingSquare && map.isVisible(target, catcher) && GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), initCrossableLines)) {
            System.out.println("HELLOOOOOOOO");
            // find the triangle on the other side of the separating line the target just crossed over
            ls = new LineSegment(previousTargetPosition, currentTargetPosition);
            System.out.println(ls);
            DEdge tempEdge = null;
            for (Line l : initCrossableLines) {
                tempLs = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
                System.out.println(tempLs);
                if (ls.intersection(tempLs) != null) {
                    tempEdge = separatingEdges.get(separatingLines.indexOf(l));
                    // ban the other ones from being spotted through
                    for (int i = 1; i < 4; i++) {
                        nastyBullshitLines.add(((SquareGuardManager) guardManagers.get(separatingLines.indexOf(l))).getSquareSideLines().get(i));
                    }
                    ;
                    System.out.println("tempEdge: " + tempEdge);
                    break;
                }
            }
            try {
                if (tempEdge != null) {
                    if (tempEdge.getLeft().contains(new DPoint(currentTargetPosition.x, currentTargetPosition.y, 0))) {
                        // make the left triangle the next "target" for search
                        currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), tempEdge.getLeft().getBarycenter().getX(), tempEdge.getLeft().getBarycenter().getY());
                        currentSearcherPath.setEndIndex(traversalHandler.getNodeIndex(tempEdge.getLeft().getBarycenter().getX(), tempEdge.getLeft().getBarycenter().getY()));
                    } else {
                        // make the right triangle the next "target" for search
                        currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), tempEdge.getRight().getBarycenter().getX(), tempEdge.getRight().getBarycenter().getY());
                        currentSearcherPath.setEndIndex(traversalHandler.getNodeIndex(tempEdge.getRight().getBarycenter().getX(), tempEdge.getRight().getBarycenter().getY()));
                    }
                    if (catcher.shareLocation(searcher)) {
                        currentCatcherPath = currentSearcherPath;
                        searcherPathLineCounter = 0;
                        catcherPathLineCounter = 0;
                    } else {
                        System.out.println("something went wrong when the target exited one of the squares that it was initially in");
                    }
                    currentStage = Stage.INIT_FIND_TARGET;
                }

            } catch (DelaunayError e) {
                e.printStackTrace();
            }
        } */ else {
            // second case: target is not visible anymore (disappeared around corner)
            // the method used here is cheating somewhat but assuming minimum feature size it just makes the computation easier
            if (pseudoBlockingVertex == null) {
                System.out.println("target around corner, calculate path to first vertex");
                ShortestPathRoadMap.drawLines = true;

                if (/*testInGuardedSquare && testSPRM == null*/(updated || testSPRM == null)) {
                    testSPRM = new ShortestPathRoadMap(testExcludedLines, map);
                    System.out.println("testExcludedLines.size(): " + testExcludedLines.size());
                    for (Line l : testExcludedLines) {
                        if (!Main.pane.getChildren().contains(l)) {
                            l.setStrokeWidth(5);
                            Main.pane.getChildren().add(l);
                        }
                    }
                }/*
                PlannedPath temp;
                if (testInGuardedSquare) {
                    temp = testSPRM.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                    //temp.draw();
                } else {
                    //temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                    temp = shortestPathRoadMap.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                }*/

                //shortestPathRoadMap.drawVerts();
                PlannedPath temp = testSPRM.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                //catchGraphics.getChildren().addAll(temp.getPathLines());

                //PlannedPath temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());

                //PlannedPath temp = shortestPathRoadMap.getShortestPath(target.getXPos() - 1, target.getYPos(), origin.getEstX() + 1, origin.getEstY());
                pseudoBlockingVertex = new Point2D(temp.getPathVertex(1).getEstX(), temp.getPathVertex(1).getEstY());
                lastPointVisible = new Point2D(catcher.getXPos(), catcher.getYPos());
                pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), target.getXPos(), -target.getYPos());
                //pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), temp.getPathVertex(2).getEstX(), -temp.getPathVertex(2).getEstY());

                catchGraphics.getChildren().add(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 4, Color.BLUEVIOLET));

                /*currentSearcherPath = (testInGuardedSquare ? testSPRM : traversalHandler.getRestrictedShortestPathRoadMap()).getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                currentCatcherPath = catcher.shareLocation(searcher) ? currentSearcherPath : (testInGuardedSquare ? testSPRM : traversalHandler.getRestrictedShortestPathRoadMap()).getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);*/
                /*currentSearcherPath = (testInGuardedSquare ? testSPRM : shortestPathRoadMap).getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                currentCatcherPath = catcher.shareLocation(searcher) ? currentSearcherPath : (testInGuardedSquare ? testSPRM : shortestPathRoadMap).getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);*/
                currentSearcherPath = shortestPathRoadMap.getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                currentCatcherPath = catcher.shareLocation(searcher) ? currentSearcherPath : shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
                System.out.println("currentSearcherPath: " + currentSearcherPath + ", testInGuardedSquare: " + testInGuardedSquare);
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
                    System.out.println("Searcher reached end of line (1)");
                    System.out.println("Evader still visible (1): " + map.isVisible(searcher, target));
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

            if (catcher.getXPos() == pseudoBlockingVertex.getX() && catcher.getYPos() == pseudoBlockingVertex.getY() && (!map.isVisible(catcher, target) /*|| (map.isVisible(catcher, target) && !legal /*&& GeometryOperations.lineIntersectSeparatingLines(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos(), separatingLines)*/)) {
                // do randomised search in pocket
                // pocket to be calculated from blocking vertex and position that the evader was last seen from (?)
                currentStage = Stage.FIND_TARGET;

                // pocket from lastPointVisible over pseudoBlockingVertex to polygon boundary
                // needs to be the current component though
                int componentIndex = 0;
                if (componentBoundaryLines.size() != 1) {
                    for (int i = 0; i < traversalHandler.getComponents().size(); i++) {
                        if (componentBoundaryShapes.get(i).contains(target.getXPos(), target.getYPos())) {
                            componentIndex = i;
                            break;
                        }
                    }
                }
                if (updated || testSPRM == null) {
                    testSPRM = new ShortestPathRoadMap(testExcludedLines, map);
                    System.out.println("testExcludedLines.size(): " + testExcludedLines.size());
                    for (Line l : testExcludedLines) {
                        if (!Main.pane.getChildren().contains(l)) {
                            l.setStrokeWidth(5);
                            Main.pane.getChildren().add(l);
                        }
                    }
                }
                /*PlannedPath temp = testSPRM.getShortestPath(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
                catchGraphics.getChildren().addAll(temp.getPathLines());
                pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), temp.getPathVertex(2).getEstX(), -temp.getPathVertex(2).getEstY());*/
                double rayStartX = lastPointVisible.getX();
                double rayStartY = lastPointVisible.getY();
                double rayDeltaX = pseudoBlockingVertex.getX() - rayStartX;
                double rayDeltaY = pseudoBlockingVertex.getY() - rayStartY;
                // determine pocket boundary line
                Point2D currentPoint, pocketBoundaryEndPoint = null;
                double minLengthSquared = Double.MAX_VALUE, currentLengthSquared;
                Line intersectedLine = null;
                for (Line line : componentBoundaryLines.get(componentIndex)) {
                    if (!separatingLines.contains(line) || testExcludedLines.contains(line) || !(((SquareGuardManager) guardManagers.get(separatingLines.indexOf(line))).inGuardedSquare(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY()))) {
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
                }

                // TODO: possibly extend the pocket to include the intersected parts of the guarding square
                if (/*!found || */pocketBoundaryEndPoint == null) {
                    System.out.println("No pocket boundary end point found.");
                } else {
                    Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), catcher.getXPos(), catcher.getYPos());
                    catchGraphics.getChildren().add(boundaryLine);
                    Label l = new Label("v");
                    l.setTranslateX(pseudoBlockingVertex.getX() + 5);
                    l.setTranslateY(pseudoBlockingVertex.getY() + 5);
                    catchGraphics.getChildren().addAll(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 7, Color.BLUEVIOLET), l);

                    // find the new "pocket component"
                    System.out.printf("Catcher at (%f|%f)\nReal at (%f|%f)\nFake at (%f|%f)\n", catcher.getXPos(), catcher.getYPos(), currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY(), currentCatcherPath.getLastPathVertex().getEstX(), currentCatcherPath.getLastPathVertex().getEstY());
                    Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, currentCatcherPath.getLastPathVertex().getRealX(), currentCatcherPath.getLastPathVertex().getRealY(), separatingLines.contains(intersectedLine) ? intersectedLine : null);
                    traversalHandler.restrictToPocket(pocketInfo.getFirst(), pocketInfo.getSecond(), map, separatingLines.contains(intersectedLine) ? intersectedLine : null);

                    System.out.println("Pocket component size: " + pocketInfo.getFirst().size());

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

        LineSegment ls = new LineSegment(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos());
        LineSegment tempLs;
        boolean legal = true;
        for (Line l : separatingLines) {
            tempLs = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            if (tempLs.intersection(ls) != null && !testCrossedLines.contains(l)) {
                /*if (!Main.pane.getChildren().contains(l)) {
                    Main.pane.getChildren().add(l);
                }*/
                legal = false;
                break;
            }
        }
        if (map.isVisible(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos()) && (legal || !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), catcher.getXPos(), catcher.getYPos(), separatingLines))) {
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
                System.out.println("Searcher reached end of line (2)");
                System.out.println("Evader still visible (2): " + map.isVisible(searcher, target));
            }

            if (map.isVisible(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos()) /*&& (legal || !GeometryOperations.lineIntersectSeparatingLines(target.getXPos(), target.getYPos(), searcher.getXPos(), searcher.getYPos(), separatingLines))*/) {
                System.out.println("target found again by searcher");
                catchGraphics.getChildren().clear();

                if (/*testInGuardedSquare && */(updated || testSPRM == null)) {
                    testSPRM = new ShortestPathRoadMap(testExcludedLines, map);
                    for (Line l : testExcludedLines) {
                        if (!Main.pane.getChildren().contains(l)) {
                            l.setStrokeWidth(5);
                            Main.pane.getChildren().add(l);
                        }
                    }
                    //testSPRM.addExtraVertices(testAddedCoordinates);
                }/*

                PlannedPath temp;

                if (testInGuardedSquare) {
                    temp = testSPRM.getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                } else {
                    //temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                    temp = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                }*/

                //PlannedPath temp = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                PlannedPath temp = testSPRM.getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                catchGraphics.getChildren().addAll(temp.getPathLines());


                //PlannedPath temp = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(catcher.getXPos(), catcher.getYPos(), target.getXPos(), target.getYPos());
                lastPointVisible = new Point2D(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                pseudoBlockingVertex = new Point2D(temp.getPathVertex(1).getEstX(), temp.getPathVertex(1).getEstY());
                //pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), target.getXPos(), -target.getYPos());
                pocketCounterClockwise = GeometryOperations.leftTurnPredicate(lastPointVisible.getX(), -lastPointVisible.getY(), pseudoBlockingVertex.getX(), -pseudoBlockingVertex.getY(), temp.getPathVertex(2).getEstX(), -temp.getPathVertex(2).getEstY());

                catchGraphics.getChildren().add(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 5, Color.MEDIUMPURPLE));

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
                boolean ignored;
                for (Line line : componentBoundaryLines.get(componentIndex)) {
                    ignored = false;
                    for (GuardManager gm : testGuardManagers) {
                        if (((SquareGuardManager) gm).getSquareSideLines().contains(line)) {
                            ignored = true;
                            break;
                        }
                    }
                    if (!ignored) {
                        currentPoint = GeometryOperations.rayLineSegIntersection(rayStartX, rayStartY, rayDeltaX, rayDeltaY, line);
                        if (currentPoint != null && (currentLengthSquared = Math.pow(catcher.getXPos() - currentPoint.getX(), 2) + Math.pow(catcher.getYPos() - currentPoint.getY(), 2)) < minLengthSquared/*&& map.isVisible(catcher.getXPos(), catcher.getYPos(), pocketBoundaryEndPoint.getEstX(), pocketBoundaryEndPoint.getEstY())*/) {
                            minLengthSquared = currentLengthSquared;
                            pocketBoundaryEndPoint = currentPoint;
                            intersectedLine = line;
                        }
                    }
                }
                Line boundaryLine = new Line(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY());
                catchGraphics.getChildren().add(boundaryLine);
                catchGraphics.getChildren().add(new Circle(pocketBoundaryEndPoint.getX(), pocketBoundaryEndPoint.getY(), 6, Color.BLACK));
                Tuple<ArrayList<DTriangle>, int[][]> pocketInfo = findPocketComponent(boundaryLine, componentIndex, pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), separatingLines.contains(intersectedLine) ? intersectedLine : null);
                traversalHandler.restrictToPocket(pocketInfo.getFirst(), pocketInfo.getSecond(), map, separatingLines.contains(intersectedLine) ? intersectedLine : null);

                System.out.println("Pocket component size: " + pocketInfo.getFirst().size());

                Label l = new Label("v");
                l.setTranslateX(pseudoBlockingVertex.getX() + 5);
                l.setTranslateY(pseudoBlockingVertex.getY() + 5);
                catchGraphics.getChildren().addAll(new Circle(pseudoBlockingVertex.getX(), pseudoBlockingVertex.getY(), 7, Color.BLUEVIOLET), l);

                currentSearcherPath = null;
                //currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), pseudoBlockingVertex);
                // not sure about which one of these it should be
                // I think the first one probably works if the random traversal truly includes the triangles in the guarding square
                // but it would fuck up otherwise because the pseudo-blocking vertex might lie somewhere completely different
                //currentCatcherPath = (testInGuardedSquare ? testSPRM : traversalHandler.getRestrictedShortestPathRoadMap()).getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
                /*currentCatcherPath = (testInGuardedSquare ? testSPRM : traversalHandler.getRestrictedShortestPathRoadMap()).getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);*/
                //currentCatcherPath = (testInGuardedSquare ? testSPRM : shortestPathRoadMap).getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
                currentCatcherPath = shortestPathRoadMap.getShortestPath(catcher.getXPos(), catcher.getYPos(), pseudoBlockingVertex);
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

            // compute the simply connected components in the graph
            ArrayList<DTriangle> componentNodes = new ArrayList<>();
            for (DTriangle dt : nodes) {
                if (!separatingTriangles.contains(dt)) {
                    componentNodes.add(dt);
                }
            }
            Tuple<ArrayList<ArrayList<DTriangle>>, int[]> componentInfo = computeConnectedComponents(nodes, componentNodes, spanningTreeAdjacencyMatrix);
            ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = componentInfo.getFirst();

            Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> lineSeparation = computeGuardingLines(separatingTriangles, nonSeparatingLines);
            ArrayList<Line> separatingLines = lineSeparation.getValue0();
            ArrayList<DEdge> reconnectingEdges = lineSeparation.getValue1();
            ArrayList<DEdge> separatingEdges = lineSeparation.getValue2();
            this.separatingLines = separatingLines;
            this.separatingEdges = separatingEdges;

            Tuple<int[][], ArrayList<ArrayList<DTriangle>>> reconnectedAdjacency = computeReconnectedAdjacency(nodes, simplyConnectedComponents, reconnectingEdges, spanningTreeAdjacencyMatrix, separatingTriangles);
            int[][] reconnectedAdjacencyMatrix = reconnectedAdjacency.getFirst();
            ArrayList<ArrayList<DTriangle>> reconnectedComponents = reconnectedAdjacency.getSecond();

            Tuple<ArrayList<ArrayList<Line>>, ArrayList<Shape>> componentBoundaries = computeComponentBoundaries(reconnectedComponents, separatingEdges, separatingLines);
            componentBoundaryLines = componentBoundaries.getFirst();
            componentBoundaryShapes = componentBoundaries.getSecond();

            guardManagers = computeGuardManagers(separatingLines);

            //gSqrIntersectingTriangles = computeGuardingSquareIntersection(guardManagers, nodes);

            traversalHandler = new TraversalHandler(shortestPathRoadMap, nodes, simplyConnectedComponents, spanningTreeAdjacencyMatrix);
            traversalHandler.separatingLineBased(separatingLines, reconnectedComponents, reconnectedAdjacencyMatrix);

            for (GuardManager gm : guardManagers) {
                requiredAgents += gm.totalRequiredGuards();
            }
            requiredAgents += 2;
            System.out.println("\nrequiredAgents: " + requiredAgents);
        } catch (DelaunayError error) {
            error.printStackTrace();
        }
    }

    private static final int TEST_THING = 3;

    private ArrayList<GuardManager> computeGuardManagers(ArrayList<Line> separatingLines) {
        ArrayList<GuardManager> lineGuardManagers = new ArrayList<>(separatingLines.size());
        LineGuardManager temp1;

        // for every reflex vertex of the polygon, calculate its visibility polygon
        // identify reflex vertices:
        // from shortest path map, get all vertices and convert them into coordinates
        List<Coordinate> vertices = Arrays.asList(map.getPolygon().getCoordinates());

        long before = System.currentTimeMillis();
        ArrayList<Tuple<Geometry, Group>> visibilityPolygons = new ArrayList<>(vertices.size());
        for (Coordinate c1 : vertices) {
            visibilityPolygons.add(computeVisibilityPolygon(c1, vertices));
        }
        Main.pane.getChildren().add(visibilityPolygons.get(TEST_THING).getSecond());
        // not working properly: 3 and 10 and 15
        System.out.println("Time taken to check for " + vertices.size() + "-vertex polygon: " + (System.currentTimeMillis() - before));

        for (Line l : separatingLines) {
            temp1 = computeSingleGuardManager(l);
            lineGuardManagers.add(temp1);
        }

        /*
        for each line segment, two guards should be assigned
        this is not done if there is already a guard in that square that has been assigned to the same points
         */

        return lineGuardManagers;
    }

    private LineGuardManager computeSingleGuardManager(Line l) {
        LineGuardManager lineGuardManager;
        lineGuardManager = new LineGuardManager();
        return lineGuardManager;
    }

    private Tuple<Geometry, Group> computeVisibilityPolygon(Coordinate c1, List<Coordinate> vertices) {
        class PointPair {
            private int index1, index2;

            private PointPair(int index1, int index2) {
                this.index1 = index1;
                this.index2 = index2;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof PointPair && ((((PointPair) o).index1 == this.index1 && ((PointPair) o).index2 == this.index2) || (((PointPair) o).index1 == this.index2 && ((PointPair) o).index2 == this.index1));
            }
        }

        // algorithm (run once for each vertex)
        Geometry visibilityPolygon;
        Group visibilityShape = new Group();;
        visibilityShape.getChildren().add(new Circle(c1.x, c1.y, 4, Color.GREEN));

        LinearRing currentTriangle;
        Line line;
        Label label;
        LineString ls;
        Geometry intersection;
        ArrayList<Tuple<Coordinate, LineString>> rays = new ArrayList<>();
        HashMap<Tuple<Coordinate, LineString>, Double> angles = new HashMap<>();

        ArrayList<PointPair> checkedPairs;
        PointPair currentPair;
        Coordinate[] temp;
        for (Coordinate c2 : vertices) {
            if (map.isVisible(c1, c2)) {
                double deltaX = c2.x - c1.x;
                double deltaY = c2.y - c1.y;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                deltaX /= length;
                deltaY /= length;
                ls = new LineString(new CoordinateArraySequence(new Coordinate[]{c2, new Coordinate(c2.x + 1E8 * deltaX, c2.y + 1E8 * deltaY)}), GeometryOperations.factory);
                intersection = ls.intersection(map.getBoundary());

                double curDistanceSquared, minDistanceSquared = Double.MAX_VALUE;
                Coordinate closest = null;
                for (Coordinate c : intersection.getCoordinates()) {
                    if (vertices.indexOf(c1) == TEST_THING) {
                        Main.pane.getChildren().addAll(new Circle(c.x, c.y, 3, Color.RED));
                    }
                    if (!c.equals2D(c2)) {
                        curDistanceSquared = Math.pow(c2.x - c.x, 2) + Math.pow(c2.y - c.y, 2);
                        if (curDistanceSquared < minDistanceSquared) {
                            minDistanceSquared = curDistanceSquared;
                            closest = c;
                        }
                    }
                }
                if (closest == null) {
                    closest = c2;
                }

                if (closest.equals2D(c2) || map.isVisible(c2, new Coordinate(closest.x - deltaX * 1E-10, closest.y - deltaY * 1E-10))) {
                    /*label.setTranslateX(closest.x + 5);
                    label.setTranslateY(closest.y - 20);*/
                    //Main.pane.getChildren().addAll(new Circle(closest.x, closest.y, 4, Color.BLUE));
                } else if (!map.isVisible(c2, new Coordinate(closest.x - deltaX * 1E-10, closest.y - deltaY * 1E-10))) {
                    closest = c2;
                    //Main.pane.getChildren().addAll(new Circle(closest.x, closest.y, 4, Color.BLUE));
                }

                double angle = Math.toDegrees(Math.atan2(c2.y - c1.y, c2.x - c1.x));
                if (angle < 0) {
                    angle += 360;
                }

                if (vertices.indexOf(c1) == TEST_THING) {
                    line = new Line(c1.x, c1.y, closest.x, closest.y);
                    Main.pane.getChildren().addAll(line, new Circle(closest.x, closest.y, 2.5, Color.BLUE));
                }

                ls = new LineString(new CoordinateArraySequence(new Coordinate[]{c1, closest}), GeometryOperations.factory);
                rays.add(new Tuple<>(closest, ls));
                angles.put(rays.get(rays.size() - 1), angle);
            }
        }

        rays.sort((l1, l2) -> {
            if (angles.get(l1) < angles.get(l2)) {
                return -1;
            } else if (angles.get(l2) < angles.get(l1)) {
                return 1;
            } else {
                return 0;
            }
        });

        Coordinate c3, c4;
        LineSegment[] segments = new LineSegment[2];
        Polygon p;

        visibilityPolygon = new com.vividsolutions.jts.geom.Polygon(null, null, GeometryOperations.factory);

        checkedPairs = new ArrayList<>();
        for (int i = 0; i < rays.size(); i++) {
            segments[0] = null;
            segments[1] = null;

            currentPair = new PointPair(i, (i + 1) % rays.size());
            if (!checkedPairs.contains(currentPair)) {
                double deltaX = rays.get(i).getSecond().getCoordinateN(1).x - rays.get(i).getSecond().getCoordinateN(0).x;
                double deltaY = rays.get(i).getSecond().getCoordinateN(1).y - rays.get(i).getSecond().getCoordinateN(0).y;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                deltaX /= length;
                deltaY /= length;
                c3 = new Coordinate(rays.get(i).getFirst().x - deltaX * 1E-8, rays.get(i).getFirst().y - deltaY * 1E-8);

                deltaX = rays.get((i + 1) % rays.size()).getSecond().getCoordinateN(1).x - rays.get(i).getSecond().getCoordinateN(0).x;
                deltaY = rays.get((i + 1) % rays.size()).getSecond().getCoordinateN(1).y - rays.get(i).getSecond().getCoordinateN(0).y;
                length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                deltaX /= length;
                deltaY /= length;
                c4 = new Coordinate(rays.get((i + 1) % rays.size()).getFirst().x - deltaX * 1E-8, rays.get((i + 1) % rays.size()).getFirst().y - deltaY * 1E-8);

                if (map.isVisible(c3, c4)) {
                    line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, rays.get((i + 1) % rays.size()).getFirst().x, rays.get((i + 1) % rays.size()).getFirst().y);
                    /*Main.pane.getChildren().addAll(new Circle(rays.get(i).getFirst().x, rays.get(i).getFirst().y, 4, Color.RED));
                    Main.pane.getChildren().addAll(line);*/
                    checkedPairs.add(currentPair);
                    currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(rays.get((i + 1) % rays.size()).getFirst().x, rays.get((i + 1) % rays.size()).getFirst().y), c1}), GeometryOperations.factory);
                    visibilityPolygon = visibilityPolygon.union(currentTriangle);

                    p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, rays.get((i + 1) % rays.size()).getFirst().x, rays.get((i + 1) % rays.size()).getFirst().y, c1.x, c1.y);
                    p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                    visibilityShape.getChildren().add(p);
                } else {
                    for (LineSegment seg : map.getBorderLines()) {
                        double distance = seg.distance(rays.get(i).getFirst());
                        if (distance < GeometryOperations.PRECISION_EPSILON) {
                        /*deltaX = seg.getCoordinate(1).x - seg.getCoordinate(0).x;
                        deltaY = seg.getCoordinate(1).y - seg.getCoordinate(0).y;
                        length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        deltaX /= length;
                        deltaY /= length;
                        line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, rays.get(i).getFirst().x + deltaX * 10, rays.get(i).getFirst().y + deltaY * 10);
                        Main.pane.getChildren().add(line);
                        line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, rays.get(i).getFirst().x - deltaX * 10, rays.get(i).getFirst().y - deltaY * 10);
                        Main.pane.getChildren().add(line);
                        Main.pane.getChildren().addAll(new Circle(rays.get(i).getFirst().x, rays.get(i).getFirst().y, 4, Color.GREEN));*/
                            if (segments[0] == null) {
                                segments[0] = seg;
                            } else {
                                segments[1] = seg;
                                break;
                            }
                        }
                    }
                    double distance1 = Double.MAX_VALUE;
                    if (segments[0] != null) {
                        distance1 = rays.get((i + 1) % rays.size()).getSecond().distance(segments[0].toGeometry(GeometryOperations.factory));
                    }
                    double distance2 = Double.MAX_VALUE;
                    if (segments[1] != null) {
                        distance2 = rays.get((i + 1) % rays.size()).getSecond().distance(segments[1].toGeometry(GeometryOperations.factory));
                    }
                    if (distance1 < distance2 && distance1 < GeometryOperations.PRECISION_EPSILON) {
                        temp = DistanceOp.nearestPoints(rays.get((i + 1) % rays.size()).getSecond(), segments[0].toGeometry(GeometryOperations.factory));
                        line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y);
                        //Main.pane.getChildren().add(line);
                        checkedPairs.add(currentPair);
                        currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                        visibilityPolygon = visibilityPolygon.union(currentTriangle);

                        p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                        p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                        visibilityShape.getChildren().add(p);
                    } else if (distance2 < distance1 && distance2 < GeometryOperations.PRECISION_EPSILON) {
                        temp = DistanceOp.nearestPoints(rays.get((i + 1) % rays.size()).getSecond(), segments[1].toGeometry(GeometryOperations.factory));
                        line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y);
                        //Main.pane.getChildren().add(line);
                        checkedPairs.add(currentPair);
                        currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                        visibilityPolygon = visibilityPolygon.union(currentTriangle);

                        p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                        p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                        visibilityShape.getChildren().add(p);
                    } else if (distance1 < GeometryOperations.PRECISION_EPSILON && distance2 < GeometryOperations.PRECISION_EPSILON) {
                        distance1 = segments[0].distance(c1);
                        distance2 = segments[1].distance(c1);
                        if (distance1 > distance2) {
                            temp = DistanceOp.nearestPoints(rays.get((i + 1) % rays.size()).getSecond(), segments[0].toGeometry(GeometryOperations.factory));
                        } else {
                            temp = DistanceOp.nearestPoints(rays.get((i + 1) % rays.size()).getSecond(), segments[1].toGeometry(GeometryOperations.factory));
                        }
                        checkedPairs.add(currentPair);
                        currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                        visibilityPolygon = visibilityPolygon.union(currentTriangle);

                        p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                        p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                        visibilityShape.getChildren().add(p);
                    }
                }
            }

            segments[0] = null;
            segments[1] = null;

            currentPair = new PointPair(i, i == 0 ? rays.size() - 1 : i - 1);
            if (!checkedPairs.contains(currentPair)) {
                for (LineSegment seg : map.getBorderLines()) {
                    double distance = seg.distance(rays.get(i).getFirst());
                    if (distance < GeometryOperations.PRECISION_EPSILON) {
                        if (segments[0] == null) {
                            segments[0] = seg;
                        } else {
                            segments[1] = seg;
                            break;
                        }
                    }
                }
                double distance1 = Double.MAX_VALUE;
                if (segments[0] != null) {
                    distance1 = rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond().distance(segments[0].toGeometry(GeometryOperations.factory));
                }
                double distance2 = Double.MAX_VALUE;
                if (segments[1] != null) {
                    distance2 = rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond().distance(segments[1].toGeometry(GeometryOperations.factory));
                }
                if (distance1 < distance2 && distance1 < GeometryOperations.PRECISION_EPSILON) {
                    temp = DistanceOp.nearestPoints(rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond(), segments[0].toGeometry(GeometryOperations.factory));
                    line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y);
                    //Main.pane.getChildren().add(line);
                    checkedPairs.add(currentPair);
                    currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                    visibilityPolygon = visibilityPolygon.union(currentTriangle);

                    p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                    p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                    visibilityShape.getChildren().add(p);
                } else if (distance2 < distance1 && distance2 < GeometryOperations.PRECISION_EPSILON) {
                    temp = DistanceOp.nearestPoints(rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond(), segments[1].toGeometry(GeometryOperations.factory));
                    line = new Line(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y);
                    //Main.pane.getChildren().add(line);
                    checkedPairs.add(currentPair);
                    currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                    visibilityPolygon = visibilityPolygon.union(currentTriangle);

                    p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                    p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                    visibilityShape.getChildren().add(p);
                } else if (distance1 < GeometryOperations.PRECISION_EPSILON && distance2 < GeometryOperations.PRECISION_EPSILON) {
                    distance1 = segments[0].distance(c1);
                    distance2 = segments[1].distance(c1);
                    if (distance1 > distance2) {
                        temp = DistanceOp.nearestPoints(rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond(), segments[0].toGeometry(GeometryOperations.factory));
                    } else {
                        temp = DistanceOp.nearestPoints(rays.get(i == 0 ? rays.size() - 1 : i - 1).getSecond(), segments[1].toGeometry(GeometryOperations.factory));
                    }
                    checkedPairs.add(currentPair);
                    currentTriangle = new LinearRing(new CoordinateArraySequence(new Coordinate[]{c1, new Coordinate(rays.get(i).getFirst().x, rays.get(i).getFirst().y), new Coordinate(temp[0].x, temp[0].y), c1}), GeometryOperations.factory);
                    visibilityPolygon = visibilityPolygon.union(currentTriangle);

                    p = new Polygon(rays.get(i).getFirst().x, rays.get(i).getFirst().y, temp[0].x, temp[0].y, c1.x, c1.y);
                    p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.3));
                    visibilityShape.getChildren().add(p);
                }
            }
        }
        return new Tuple<>(visibilityPolygon, visibilityShape);
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
                    /*if (((SquareGuardManager) guardManagers.get(i)).getGuardedSquare().covers(temp)*//*((SquareGuardManager) guardManagers.get(i)).getGuardedSquare().covers(new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY())}), GeometryOperations.factory))*//*) {
                        Polygon p = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                        p.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.3));
                        Main.pane.getChildren().add(p);
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
            AdaptedSimulation.masterPause("in DCRSEntity");
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
            //if (!testInGuardedSquare || (testInGuardedSquare && ((SquareGuardManager) testGuardManagers.get(0)).inGuardedSquare(pseudoBlockingVertX, pseudoBlockingVertY) && gSqrIntersectingTriangles.get(guardManagers.indexOf(testGuardManagers.get(0))).contains(dt))) {
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
                //catchGraphics.getChildren().add(plgn);
                pocketBoundaryTriangles.add(dt);
            }
            //}
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
                catchGraphics.getChildren().add(new Circle(closestIntersectionPoint.getX(), closestIntersectionPoint.getY(), 3, Color.BLUE));

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
                        catchGraphics.getChildren().add(l);
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
                        // TODO: (maybe) also account for separating edges instead of adjacency from the matrix
                        if (traversalHandler.getAdjacencyMatrix()[traversalHandler.getTriangles().indexOf(dt)][i] == 1 /*&& !pocketBoundaryTriangles.contains(traversalHandler.getTriangles().get(i))*/ && (crossedSeparatingLine == null || !guardingSquareTriangles.contains(dt) || guardingSquareTriangles.contains(traversalHandler.getTriangles().get(i)) /*|| thing.contains(dt) is the parent of this dt in the guarding square */)) {
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
                        //catchGraphics.getChildren().add(plgn);

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

        DEdge de0, de1, de2;
        Line l;
        for (int i = 1; i < pocketAdjacencyMatrix.length; i++) {
            for (int j = 0; j < i; j++) {
                if (pocketAdjacencyMatrix[i][j] == 1 /*&& there is actually a separating edge between these triangles, except when the evader actually entered through that separating edge*/) {
                    de0 = traversalHandler.getTriangles().get(j).getEdge(0);
                    de1 = traversalHandler.getTriangles().get(j).getEdge(1);
                    de2 = traversalHandler.getTriangles().get(j).getEdge(2);
                    /*if ((Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de0) && separatingEdges.contains(de0) && !testCrossedLines.contains(separatingLines.get(separatingEdges.indexOf(de0)))) ||
                            (Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de1) && separatingEdges.contains(de1) && !testCrossedLines.contains(separatingLines.get(separatingEdges.indexOf(de1)))) ||
                            (Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de2) && separatingEdges.contains(de2) && !testCrossedLines.contains(separatingLines.get(separatingEdges.indexOf(de2))))) {
                        pocketAdjacencyMatrix[i][j] = 0;
                        pocketAdjacencyMatrix[j][i] = 0;
                    }*/
                    if ((Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de0) && separatingEdges.contains(de0)) ||
                            (Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de1) && separatingEdges.contains(de1)) ||
                            (Arrays.asList(traversalHandler.getTriangles().get(i).getEdges()).contains(de2) && separatingEdges.contains(de2))) {
                        pocketAdjacencyMatrix[i][j] = 0;
                        pocketAdjacencyMatrix[j][i] = 0;
                    } /*else {
                        try {
                            l = new Line(traversalHandler.getTriangles().get(i).getBarycenter().getX(), traversalHandler.getTriangles().get(i).getBarycenter().getY(), traversalHandler.getTriangles().get(j).getBarycenter().getX(), traversalHandler.getTriangles().get(j).getBarycenter().getY());
                            l.setStroke(Color.INDIANRED);
                            l.setStrokeWidth(2);
                            catchGraphics.getChildren().add(l);
                        } catch (DelaunayError delaunayError) {
                            delaunayError.printStackTrace();
                        }
                    }*/
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
