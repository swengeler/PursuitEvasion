package entities.specific;

import additionalOperations.GeometryOperations;
import entities.base.DistributedEntity;
import entities.utils.*;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.*;

import java.util.ArrayList;

public class TestEntity extends DistributedEntity {

    private static PlannedPath currentPath;
    private static int pathCounter;
    private static ShortestPathRoadMap sprm;
    private static Agent controlledAgent;
    private static MapRepresentation mapThing;

    //private double targetX = 110.0, targetY = 317.0; // default for searcher_catcher_test_2: 111.0, 317.0
    public static double targetX, targetY;

    public TestEntity(MapRepresentation map) {
        super(map);
        targetX = map.getBorderPolygon().getPoints().get(0);
        targetY = map.getBorderPolygon().getPoints().get(1);
        sprm = new ShortestPathRoadMap(map);
        mapThing = map;
    }

    public TestEntity(MapRepresentation map, double targetX, double targetY) {
        super(map);
        TestEntity.targetX = targetX;
        TestEntity.targetY = targetY;
        sprm = new ShortestPathRoadMap(map);
        mapThing = map;
    }

    public static void setTargetLocation(double x, double y) {
        if (mapThing != null && mapThing.legalPosition(x, y)) {
            currentPath = sprm.getShortestPath(controlledAgent.getXPos(), controlledAgent.getYPos(), x, y);
            pathCounter = 0;
        }
    }

    @Override
    public void move() {
        // check if any agent is caught
        /*if (currentPath == null) {
            for (Entity e : map.getPursuingEntities()) {
                for (Agent a : e.getControlledAgents()) {
                    if (map.isVisible(a, controlledAgent)) {
                        // find point in polygon to go to
                        *//*double maxVertexDistance = -Double.MAX_VALUE;
                        PlannedPath maxDistancePath = null, temp;
                        for (Point2D p : getPolygonPoints()) {
                            temp = shortestPathRoadMap.getShortestPath(controlledAgent.getXPos(), controlledAgent.getYPos(), p);
                            boolean danger = false;
                            for (Line l : temp.getPathLines()) {
                                System.out.println(Math.sqrt(Math.pow(l.getEndX() - a.getXPos(), 2) + Math.pow(l.getEndY() - a.getYPos(), 2) - Math.pow(controlledAgent.getXPos() - a.getXPos(), 2) + Math.pow(controlledAgent.getYPos() - a.getYPos(), 2)));
                                if (Math.sqrt(Math.pow(l.getEndX() - a.getXPos(), 2) + Math.pow(l.getEndY() - a.getYPos(), 2) - Math.pow(controlledAgent.getXPos() - a.getXPos(), 2) + Math.pow(controlledAgent.getYPos() - a.getYPos(), 2)) < -50) {
                                    danger = true;
                                    break;
                                }
                            }
                            if (!danger && temp.getPathLines().size() - 1 > maxVertexDistance) {
                                maxVertexDistance = temp.getPathLines().size() - 1;
                                maxDistancePath = temp;
                            }
                        }
                        if (maxDistancePath != null) {*//*
                        currentPath = shortestPathRoadMap.getShortestPath(controlledAgent.getXPos(), controlledAgent.getYPos(), new Point2D(targetX, targetY));
                        pathCounter = 0;
                        //}
                    }
                }
            }
        }*/

        if (currentPath != null) {
            ArrayList<PathLine> pathLines = currentPath.getPathLines();
            double length = Math.sqrt(Math.pow(pathLines.get(pathCounter).getEndX() - pathLines.get(pathCounter).getStartX(), 2) + Math.pow(pathLines.get(pathCounter).getEndY() - pathLines.get(pathCounter).getStartY(), 2));
            double deltaX = (pathLines.get(pathCounter).getEndX() - pathLines.get(pathCounter).getStartX()) / length * controlledAgent.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
            double deltaY = (pathLines.get(pathCounter).getEndY() - pathLines.get(pathCounter).getStartY()) / length * controlledAgent.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
            if (pathLines.get(pathCounter).contains(controlledAgent.getXPos() + deltaX, controlledAgent.getYPos() + deltaY)) {
                // move along line
                controlledAgent.moveBy(deltaX, deltaY);
            } else {
                // move to end of line
                controlledAgent.moveBy(pathLines.get(pathCounter).getEndX() - controlledAgent.getXPos(), pathLines.get(pathCounter).getEndY() - controlledAgent.getYPos());
                pathCounter++;
                if (pathCounter > pathLines.size() - 1) {
                    currentPath = null;
                    pathCounter = 0;
                }
            }
        }
    }

    @Override
    public void setAgent(Agent a) {
        controlledAgent = a;
    }

    @Override
    public boolean isActive() {
        return controlledAgent.isActive();
    }

    @Override
    public ArrayList<Agent> getControlledAgents() {
        ArrayList<Agent> result = new ArrayList<>();
        result.add(controlledAgent);
        return result;
    }

    private ArrayList<Point2D> getPolygonPoints() {
        ArrayList<Point2D> result = new ArrayList<>();
        for (Polygon p : map.getAllPolygons()) {
            result.addAll(GeometryOperations.polyToPoints(p));
        }
        return result;
    }

}
