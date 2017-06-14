package entities;

import additionalOperations.GeometryOperations;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import simulation.Agent;
import ui.Main;

import java.util.ArrayList;
import java.util.HashMap;

public class GuardManager {

    private Group graphics;

    private Line originalSeparatingLine;
    private Polygon guardedSquare; // the square shape, used for covers() checks
    private ArrayList<LineString> squareSides;

    private ArrayList<LineString> currentGuardedLines;
    private ArrayList<LineSegment> currentGuardedLineSegments;
    private ArrayList<Agent> currentGuards;
    private LineString crossingLine, entranceLine;
    private Point currentTargetPos, lastTargetPos;

    private HashMap<LineString, ArrayList<LineString>> entranceToGuarded; // ordered: parallel, perpendicular1, perpendicular2
    private HashMap<LineString, ArrayList<LineSegment>> guardedToSegments;
    private HashMap<LineSegment, ArrayList<Agent>> segmentToGuards; // might want to make this directed later (?)

    private ArrayList<Coordinate> originalPositions;
    private HashMap<Coordinate, LineSegment[]> ogPositionsToSegments;
    private HashMap<Agent, Coordinate> guardsToOgPositions;
    private HashMap<Agent, LineSegment> currentAssignment;

    private ArrayList<Agent> guards;
    private ArrayList<Agent> alreadyAssigned;

    public GuardManager(Line originalSeparatingLine, Polygon guardedSquare, ArrayList<LineString> squareSides, HashMap<LineString, ArrayList<LineString>> entranceToGuarded, HashMap<LineString, ArrayList<LineSegment>> guardedToSegments) {
        currentTargetPos = new Point(new CoordinateArraySequence(1), GeometryOperations.factory);
        lastTargetPos = new Point(new CoordinateArraySequence(1), GeometryOperations.factory);
        crossingLine = new LineString(new CoordinateArraySequence(new Coordinate[]{lastTargetPos.getCoordinate(), currentTargetPos.getCoordinate()}), GeometryOperations.factory);
        entranceLine = new LineString(new CoordinateArraySequence(2), GeometryOperations.factory);
        currentAssignment = new HashMap<>();
        alreadyAssigned = new ArrayList<>();

        this.originalSeparatingLine = originalSeparatingLine;
        this.guardedSquare = guardedSquare;
        this.squareSides = squareSides;
        this.entranceToGuarded = entranceToGuarded;
        this.guardedToSegments = guardedToSegments;

        /*for (int i = 0; i < guardedSquare.getCoordinates().length - 1; i++) {
            Line l = new Line(guardedSquare.getCoordinates()[i].x, guardedSquare.getCoordinates()[i].y, guardedSquare.getCoordinates()[i + 1].x, guardedSquare.getCoordinates()[i + 1].y);
            l.setStrokeWidth(4);
            l.setStroke(Color.LIGHTBLUE);
            Main.pane.getChildren().add(l);
        }*/

        // compute original positions
        originalPositions = new ArrayList<>();
        ogPositionsToSegments = new HashMap<>();
        for (LineString ls1 : squareSides) {
            for (LineSegment ls2 : guardedToSegments.get(ls1)) {
                boolean containsFirst = false, containsSecond = false;
                for (Coordinate c : originalPositions) {
                    if (c.equals2D(ls2.getCoordinate(0))) {
                        containsFirst = true;
                    }
                    if (c.equals2D(ls2.getCoordinate(1))) {
                        containsSecond = true;
                    }
                }
                if (!containsFirst) {
                    originalPositions.add(ls2.getCoordinate(0));
                    ogPositionsToSegments.put(ls2.getCoordinate(0), new LineSegment[]{ls2, null});
                } else {
                    ogPositionsToSegments.get(ls2.getCoordinate(0))[1] = ls2;
                }
                if (!containsSecond) {
                    originalPositions.add(ls2.getCoordinate(1));
                    ogPositionsToSegments.put(ls2.getCoordinate(1), new LineSegment[]{ls2, null});
                } else {
                    ogPositionsToSegments.get(ls2.getCoordinate(1))[1] = ls2;
                }
            }
        }

        graphics = new Group();
        Main.pane.getChildren().add(graphics);
    }

    public void assignGuards(ArrayList<Agent> guards) {
        assert guards.size() == originalPositions.size();
        this.guards = new ArrayList<>();
        this.guards.addAll(guards);
        segmentToGuards = new HashMap<>();
        guardsToOgPositions = new HashMap<>();
        ArrayList<Agent> temp;
        for (int i = 0; i < originalPositions.size(); i++) {
            for (LineSegment ls : ogPositionsToSegments.get(originalPositions.get(i))) {
                if (!segmentToGuards.containsKey(ls)) {
                    temp = new ArrayList<>();
                    temp.add(guards.get(i));
                    segmentToGuards.put(ls, temp);
                    guardsToOgPositions.putIfAbsent(guards.get(i), originalPositions.get(i));
                } else {
                    segmentToGuards.get(ls).add(guards.get(i));
                    guardsToOgPositions.putIfAbsent(guards.get(i), originalPositions.get(i));
                }
            }
        }

        /*for (LineSegment ls : segmentToGuards.keySet()) {
            Color currentColor1 = new Color(Math.random(), Math.random(), Math.random(), 1.0);
            Color currentColor2 = new Color(Math.random(), Math.random(), Math.random(), 1.0);
            graphics.getChildren().add(new Circle(ls.getCoordinate(0).x, ls.getCoordinate(0).y, 7.5, currentColor1));
            graphics.getChildren().add(new Circle(ls.getCoordinate(1).x, ls.getCoordinate(1).y, 7.5, currentColor2));

            double deltaX = ls.getCoordinate(1).x - ls.getCoordinate(0).x;
            double deltaY = ls.getCoordinate(1).y - ls.getCoordinate(0).y;
            double lineLength = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
            deltaX /= lineLength;
            deltaY /= lineLength;

            Line l1 = new Line(ls.getCoordinate(0).x, ls.getCoordinate(0).y, ls.getCoordinate(0).x + 15 * deltaX, ls.getCoordinate(0).y + 15 * deltaY);
            l1.setStroke(currentColor1);
            l1.setStrokeWidth(3);
            Line l2 = new Line(ls.getCoordinate(1).x, ls.getCoordinate(1).y, ls.getCoordinate(1).x - 15 * deltaX, ls.getCoordinate(1).y - 15 * deltaY);
            l2.setStroke(currentColor2);
            l2.setStrokeWidth(3);
            graphics.getChildren().addAll(l1, l2);
        }*/
    }

    public void updateTargetPosition(Agent a) {
        graphics.toFront();
        //lastTargetPos.getCoordinate().x = currentTargetPos.getX();
        //lastTargetPos.getCoordinate().y = currentTargetPos.getY();
        lastTargetPos = currentTargetPos;
        currentTargetPos = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(a.getXPos(), a.getYPos())}), GeometryOperations.factory);
        //currentTargetPos.getCoordinate().x = a.getXPos();
        //currentTargetPos.getCoordinate().y = a.getYPos();
        for (Agent g : guards) {
            if (Math.pow(a.getXPos() - g.getXPos(), 2) + Math.pow(a.getYPos() - g.getYPos(), 2) < Math.pow(g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER, 2)) {
                // catch agent and return
                g.moveTo(a.getXPos(), a.getYPos());
                a.setActive(false);
                return;
            }
        }
        //System.out.println("Target position updated: (" + a.getXPos() + "|" + a.getYPos() + "), inside square: " + guardedSquare.covers(currentTargetPos));
        crossingLine = new LineString(new CoordinateArraySequence(new Coordinate[]{lastTargetPos.getCoordinate(), currentTargetPos.getCoordinate()}), GeometryOperations.factory);
        // TODO: introduce some checks (e.g if the target is caught) to avoid doing all these computations all the time
        if (guardedSquare.covers(currentTargetPos) && !guardedSquare.covers(lastTargetPos)) {
            alreadyAssigned.clear();
            currentAssignment.clear();
            System.out.println("Target entered guarding square");
            // need to start following the thing
            // find out which line was crossed
            for (int i = 0; i < 4; i++) {
                if (squareSides.get(i).intersects(crossingLine)) {
                    entranceLine = squareSides.get(i);
                    break;
                }
            }

            //System.out.printf("Entrance line: (%.3f|%.3f) to (%.3f|%.3f)\n", entranceLine.getStartPoint().getX(), entranceLine.getStartPoint().getY(), entranceLine.getEndPoint().getX(), entranceLine.getEndPoint().getY());

            currentGuardedLines = entranceToGuarded.get(entranceLine);

            /*Line l = new Line(currentGuardedLines.get(0).getStartPoint().getX(), currentGuardedLines.get(0).getStartPoint().getY(), currentGuardedLines.get(0).getEndPoint().getX(), currentGuardedLines.get(0).getEndPoint().getY());
            l.setStroke(Color.LIGHTGREEN);
            l.setStrokeWidth(4);
            graphics.getChildren().add(l);
            l = new Line(currentGuardedLines.get(1).getStartPoint().getX(), currentGuardedLines.get(1).getStartPoint().getY(), currentGuardedLines.get(1).getEndPoint().getX(), currentGuardedLines.get(1).getEndPoint().getY());
            l.setStroke(Color.RED);
            l.setStrokeWidth(4);
            graphics.getChildren().add(l);
            l = new Line(currentGuardedLines.get(2).getStartPoint().getX(), currentGuardedLines.get(2).getStartPoint().getY(), currentGuardedLines.get(2).getEndPoint().getX(), currentGuardedLines.get(2).getEndPoint().getY());
            l.setStroke(Color.RED);
            l.setStrokeWidth(4);
            graphics.getChildren().add(l);*/

            for (LineString ls1 : currentGuardedLines) {
                currentGuardedLineSegments = guardedToSegments.get(ls1);
                for (LineSegment ls2 : currentGuardedLineSegments) {
                    currentGuards = segmentToGuards.get(ls2);
                    if (!alreadyAssigned.contains(currentGuards.get(0))) {
                        currentAssignment.put(currentGuards.get(0), ls2);
                        alreadyAssigned.add(currentGuards.get(0));
                    }
                    if (!alreadyAssigned.contains(currentGuards.get(1))) {
                        currentAssignment.put(currentGuards.get(1), ls2);
                        alreadyAssigned.add(currentGuards.get(1));
                    }
                }
            }

            Color currentColor = Color.GREEN;
            for (Agent agent : currentAssignment.keySet()) {
                //currentColor = new Color(Math.random(), Math.random(), Math.random(), 1.0);
                graphics.getChildren().add(new Circle(agent.getXPos(), agent.getYPos(), 7, currentColor));

                double length1 = Math.pow(currentAssignment.get(agent).getCoordinate(0).x - agent.getXPos(), 2) + Math.pow(currentAssignment.get(agent).getCoordinate(0).y - agent.getYPos(), 2);
                double length2 = Math.pow(currentAssignment.get(agent).getCoordinate(1).x - agent.getXPos(), 2) + Math.pow(currentAssignment.get(agent).getCoordinate(1).y - agent.getYPos(), 2);
                double deltaX = currentAssignment.get(agent).getCoordinate(1).x - currentAssignment.get(agent).getCoordinate(0).x;
                double deltaY = currentAssignment.get(agent).getCoordinate(1).y - currentAssignment.get(agent).getCoordinate(0).y;
                double lineLength = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                deltaX /= lineLength;
                deltaY /= lineLength;

                Line l = new Line(agent.getXPos(), agent.getYPos(), agent.getXPos() + 15 * (length1 < length2 ? deltaX : -deltaX), agent.getYPos() + 15 * (length1 < length2 ? deltaY : -deltaY));
                l.setStroke(currentColor);
                l.setStrokeWidth(4);
                graphics.getChildren().add(l);
            }

            // make first move towards the projection point or capture the evader if possible
            // for each agent:
            // 1. compute projection point on currently assigned line segment
            // 2. check whether point on line segment, if not compute closest point on segment to projection point
            // 3. move in straight line to projection or closest point
            Coordinate targetPoint;
            double deltaX, deltaY, length;
            for (Agent g : currentAssignment.keySet()) {
                targetPoint = currentAssignment.get(g).closestPoint(currentTargetPos.getCoordinate());
                deltaX = targetPoint.x - g.getXPos();
                deltaY = targetPoint.y - g.getYPos();
                length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                deltaX /= length;
                deltaY /= length;
                if (length <= g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER) {
                    g.moveTo(targetPoint.x, targetPoint.y);
                } else {
                    g.moveBy(deltaX * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER, deltaY * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER);
                }
            }
        } else if (guardedSquare.covers(currentTargetPos)) {
            if (currentAssignment.isEmpty()) {
                alreadyAssigned.clear();
                currentGuardedLines = entranceToGuarded.get(squareSides.get(0));

                // start with closest side
                ArrayList<LineString> squareSidesCopy = new ArrayList<>();
                squareSidesCopy.addAll(squareSides);
                squareSidesCopy.sort((obj1, obj2) -> {
                    double distance1 = obj1.distance(currentTargetPos);
                    double distance2 = obj2.distance(currentTargetPos);
                    if (distance1 < distance2) {
                        return -1;
                    } else if (distance2 < distance1) {
                        return 1;
                    } else {
                        return 0;
                    }
                });

                for (LineString ls1 : squareSidesCopy) {
                    currentGuardedLineSegments = guardedToSegments.get(ls1);
                    for (LineSegment ls2 : currentGuardedLineSegments) {
                        currentGuards = segmentToGuards.get(ls2);
                        if (!alreadyAssigned.contains(currentGuards.get(0))) {
                            currentAssignment.put(currentGuards.get(0), ls2);
                            alreadyAssigned.add(currentGuards.get(0));
                        }
                        if (currentGuards.size() > 1 && !alreadyAssigned.contains(currentGuards.get(1))) {
                            currentAssignment.put(currentGuards.get(1), ls2);
                            alreadyAssigned.add(currentGuards.get(1));
                        }
                    }
                }
            }
            //if (!lastTargetPos.getCoordinate().equals2D(currentTargetPos.getCoordinate())) {
                // moved within the square, need to adjust target points to move to
                Coordinate targetPoint;
                double deltaX, deltaY, length;
                for (Agent g : currentAssignment.keySet()) {
                    targetPoint = currentAssignment.get(g).closestPoint(currentTargetPos.getCoordinate());
                    deltaX = targetPoint.x - g.getXPos();
                    deltaY = targetPoint.y - g.getYPos();
                    length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    deltaX /= length;
                    deltaY /= length;
                    if (length <= g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER) {
                        g.moveTo(targetPoint.x, targetPoint.y);
                    } else {
                        g.moveBy(deltaX * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER, deltaY * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER);
                    }
                }
            //}
        } else if (!guardedSquare.covers(currentTargetPos) && guardedSquare.covers(lastTargetPos)) {
            System.out.println("Target left guarding square");
            graphics.getChildren().clear();
        } else {
            // not inside the square, return to previous positions
            double deltaX, deltaY, length;
            for (Agent g : currentAssignment.keySet()) {
                deltaX = guardsToOgPositions.get(g).x - g.getXPos();
                deltaY = guardsToOgPositions.get(g).y - g.getYPos();
                length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                deltaX /= length;
                deltaY /= length;
                if (length <= g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER) {
                    g.moveTo(guardsToOgPositions.get(g).x, guardsToOgPositions.get(g).y);
                } else {
                    g.moveBy(deltaX * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER, deltaY * g.getSpeed() * Entity.UNIVERSAL_SPEED_MULTIPLIER);
                }
            }
        }

    }

    public void initTargetPosition(Agent a) {
        //currentTargetPos.getCoordinate().x = a.getXPos();
        //currentTargetPos.getCoordinate().y = a.getYPos();
        currentTargetPos = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(a.getXPos(), a.getYPos())}), GeometryOperations.factory);
    }

    public int totalRequiredGuards() {
        return originalPositions.size();
    }

    public int getGuardsLeft() {
        return originalPositions.size();
    }

    public ArrayList<Coordinate> getOriginalPositions() {
        return originalPositions;
    }

    public Polygon getGuardedSquare() {
        return guardedSquare;
    }

    public Line getOriginalSeparatingLine() {
        return originalSeparatingLine;
    }

    public boolean crossedNonSeparatingLine() {
        crossingLine = new LineString(new CoordinateArraySequence(new Coordinate[]{lastTargetPos.getCoordinate(), currentTargetPos.getCoordinate()}), GeometryOperations.factory);
        return guardedSquare.covers(currentTargetPos) && !guardedSquare.covers(lastTargetPos) && !crossingLine.intersects(squareSides.get(0));
    }

    public boolean inGuardedSquare(double x, double y) {
        return guardedSquare.covers(new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(x, y)}), GeometryOperations.factory));
    }

}
