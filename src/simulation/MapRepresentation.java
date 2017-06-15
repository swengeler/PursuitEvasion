package simulation;

import additionalOperations.GeometryOperations;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import entities.Entity;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import ui.Main;
import ui.MapPolygon;

import java.util.ArrayList;
import java.util.Random;

public class MapRepresentation {

    private Polygon borderPolygon;
    private ArrayList<javafx.scene.shape.Polygon> obstaclePolygons;
    private ArrayList<javafx.scene.shape.Polygon> allPolygons;
    private ArrayList<Line> polygonEdges;

    private com.vividsolutions.jts.geom.Polygon polygon;
    private Geometry boundary;
    private Point tempPoint = new Point(new CoordinateArraySequence(1), GeometryOperations.factory);
    private LineString tempLine = new LineString(new CoordinateArraySequence(2), GeometryOperations.factory);
    private ArrayList<LineSegment> allLines;
    private ArrayList<LineSegment> borderLines;
    private ArrayList<LineSegment> obstacleLines;

    private ArrayList<Entity> pursuingEntities;
    private ArrayList<Entity> evadingEntities;

    public MapRepresentation(ArrayList<MapPolygon> map) {
        init(map);
        pursuingEntities = new ArrayList<>();
        evadingEntities = new ArrayList<>();
    }

    public MapRepresentation(ArrayList<MapPolygon> map, ArrayList<Entity> pursuingEntities, ArrayList<Entity> evadingEntities) {
        init(map);
        this.pursuingEntities = pursuingEntities == null ? new ArrayList<>() : pursuingEntities;
        this.evadingEntities = evadingEntities == null ? new ArrayList<>() : evadingEntities;
    }

    private void init(ArrayList<MapPolygon> map) {
        allPolygons = new ArrayList<>();
        obstaclePolygons = new ArrayList<>();
        for (MapPolygon p : map) {
            if (p.getPoints().size() > 0) {
                allPolygons.add(p.getPolygon());
                obstaclePolygons.add(allPolygons.get(allPolygons.size() - 1));
            }
        }
        borderPolygon = allPolygons.get(0);
        obstaclePolygons.remove(0);

        polygonEdges = new ArrayList<>();
        for (Polygon p : allPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                polygonEdges.add(new Line(p.getPoints().get(i), p.getPoints().get(i + 1), (p.getPoints().get((i + 2) % p.getPoints().size())), (p.getPoints().get((i + 3) % p.getPoints().size()))));
            }
        }

        allLines = new ArrayList<>();
        borderLines = new ArrayList<>();
        for (int i = 0; i < borderPolygon.getPoints().size(); i += 2) {
            borderLines.add(new LineSegment(borderPolygon.getPoints().get(i), borderPolygon.getPoints().get(i + 1), borderPolygon.getPoints().get((i + 2) % borderPolygon.getPoints().size()), borderPolygon.getPoints().get((i + 3) % borderPolygon.getPoints().size())));
        }
        allLines.addAll(borderLines);
        obstacleLines = new ArrayList<>();
        for (Polygon p : obstaclePolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                obstacleLines.add(new LineSegment(p.getPoints().get(i), p.getPoints().get(i + 1), p.getPoints().get((i + 2) % p.getPoints().size()), p.getPoints().get((i + 3) % p.getPoints().size())));
            }
        }
        allLines.addAll(obstacleLines);

        // constructing the outer border polygon
        Coordinate[] coordinateArray = new Coordinate[borderPolygon.getPoints().size() / 2 + 1];
        for (int i = 0; i < borderPolygon.getPoints().size(); i += 2) {
            coordinateArray[i / 2] = new Coordinate(borderPolygon.getPoints().get(i), borderPolygon.getPoints().get(i + 1));
        }
        coordinateArray[coordinateArray.length - 1] = new Coordinate(coordinateArray[0]);
        CoordinateSequence coordinateSequence = new CoordinateArraySequence(coordinateArray);
        LinearRing shell = new LinearRing(coordinateSequence, GeometryOperations.factory);

        // constructing the inner hole polygons
        LinearRing[] holes = new LinearRing[obstaclePolygons.size()];
        for (Polygon p : obstaclePolygons) {
            coordinateArray = new Coordinate[p.getPoints().size() / 2 + 1];
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                coordinateArray[i / 2] = new Coordinate(p.getPoints().get(i), p.getPoints().get(i + 1));
            }
            coordinateArray[coordinateArray.length - 1] = new Coordinate(coordinateArray[0]);
            coordinateSequence = new CoordinateArraySequence(coordinateArray);
            holes[obstaclePolygons.indexOf(p)] = new LinearRing(coordinateSequence, GeometryOperations.factory);
        }

        polygon = new com.vividsolutions.jts.geom.Polygon(shell, holes, GeometryOperations.factory);
        boundary = polygon.getBoundary();

        // TODO: construct LinearRing objects from polygons, construct a Polygon object from that
    }

    Random rand = new Random(23548);

    public boolean legalPosition(double xPos, double yPos) {
        /*tempPoint.getCoordinate().x = xPos;
        tempPoint.getCoordinate().y = yPos;*/
        Coordinate what = new Coordinate(xPos, yPos);
        /*tempPoint.getCoordinates()[0].x = what.x;
        tempPoint.getCoordinates()[0].y = what.y;
        tempPoint.getCoordinateSequence().getCoordinate(0).x = what.x;
        tempPoint.getCoordinateSequence().getCoordinate(0).y = what.y;
        tempPoint.getCoordinate().x = what.x;
        tempPoint.getCoordinate().y = what.y;*/
        /*System.out.println("1: " + Arrays.toString(tempPoint.getCoordinateSequence().toCoordinateArray()));
        tempPoint.getCoordinates()[0].x = xPos;
        tempPoint.getCoordinates()[0].y = yPos;
        System.out.println("Distance (2): " + polygon.distance(tempPoint));
        DistanceOp dop = new DistanceOp(polygon, tempPoint);
        System.out.println("Distance (2 something): " + Arrays.toString(dop.nearestPoints()));
        System.out.println("2: " + Arrays.toString(tempPoint.getCoordinateSequence().toCoordinateArray()));
        if (help && polygon.distance(tempPoint) >= GeometryOperations.PRECISION_EPSILON) {
            Pane pane = new Pane();
            for (Coordinate c : polygon.getCoordinates()) {
                pane.getChildren().add(new Circle(c.x, c.y, 4, Color.BLACK));
            }
            WritableImage image = pane.snapshot(new SnapshotParameters(), null);
            File file = new File("E:\\Simon\\Desktop\\helppls\\what_" + rand.nextInt(100) + ".png");
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        tempPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(xPos, yPos)}), GeometryOperations.factory);
        /*System.out.println("3: " + Arrays.toString(tempPoint.getCoordinateSequence().toCoordinateArray()));
        System.out.println("Distance (3): " + polygon.distance(tempPoint));*/
        //return polygon.covers(tempPoint);
        return legalPosition(tempPoint);

        /*if (!borderPolygon.contains(xPos, yPos)) {
            return false;
        }
        for (Polygon p : obstaclePolygons) {
            if (GeometryOperations.inPolygonWithoutBorder(p, xPos, yPos)) {
                return false;
            }
            *//*if (p.contains(xPos, yPos)) {
                return false;
            }*//*
        }
        return true;*/
    }

    public boolean legalPositionSpecial(Coordinate c) {
        tempPoint.getCoordinates()[0].x = c.x;
        tempPoint.getCoordinates()[0].y = c.y;
        tempPoint.getCoordinateSequence().getCoordinate(0).x = c.x;
        tempPoint.getCoordinateSequence().getCoordinate(0).y = c.y;
        return legalPosition(tempPoint);
    }

    public boolean legalPosition(Coordinate c) {
        return legalPosition(c.x, c.y);
    }

    public boolean legalPosition(Point p) {
        return polygon.distance(p) < GeometryOperations.PRECISION_EPSILON;
    }

    public ArrayList<LineSegment> getBorderLines() {
        return borderLines;
    }

    public ArrayList<LineSegment> getObstacleLines() {
        return obstacleLines;
    }

    public ArrayList<LineSegment> getAllLines() {
        return allLines;
    }

    public Polygon getBorderPolygon() {
        return borderPolygon;
    }

    public ArrayList<Polygon> getObstaclePolygons() {
        return obstaclePolygons;
    }

    public ArrayList<Polygon> getAllPolygons() {
        return allPolygons;
    }

    public ArrayList<Line> getPolygonEdges() {
        return polygonEdges;
    }

    public com.vividsolutions.jts.geom.Polygon getPolygon() {
        return polygon;
    }

    public Geometry getBoundary() {
        return boundary;
    }

    public ArrayList<Entity> getPursuingEntities() {
        return pursuingEntities;
    }

    public ArrayList<Entity> getEvadingEntities() {
        return evadingEntities;
    }

    /*public boolean isVisible(double x1, double y1, double x2, double y2) {
        // check whether the second controlledAgents is visible from the position of the first controlledAgents
        // (given its field of view and the structure of the map)
        for (Line l : polygonEdges) {
            if (GeometryOperations.lineIntersect(l, x1, y1, x2, y2)) {
                return false;
            }
        }
        for (Polygon p : obstaclePolygons) {
            if (GeometryOperations.inPolygonWithoutBorder(p, x1, y1, x2, y2)) {
                return false;
            }
        }
        if (!GeometryOperations.inPolygon(borderPolygon, x1, y1, x2, y2)) {
            return false;
        }
        return true;
    }*/

    public static boolean showVisible = false;

    public boolean isVisible(double x1, double y1, double x2, double y2) {
        /*tempLine.getCoordinates()[0].x = x1;
        tempLine.getCoordinates()[0].y = y1;
        tempLine.getCoordinates()[1].x = x2;
        tempLine.getCoordinates()[1].y = y2;*/
        tempLine = new LineString(new CoordinateArraySequence(new Coordinate[]{new Coordinate(x1, y1), new Coordinate(x2, y2)}), GeometryOperations.factory);
        if (polygon.covers(tempLine)) {
            return true;
        }
        if (showVisible) {
            Geometry intersection = boundary.intersection(tempLine);
            Geometry difference = tempLine.difference(polygon);
            System.out.println("difference is empty: " + difference.isEmpty());
            for (Coordinate c : difference.getCoordinates()) {
                Main.pane.getChildren().add(new Circle(c.x, c.y, 2, Color.LAWNGREEN));
                System.out.println(c);
            }
            System.out.println("Intersection between vision line and boundary: (" + !intersection.isEmpty() + ")");
            for (Coordinate c : intersection.getCoordinates()) {
                Main.pane.getChildren().add(new Circle(c.x, c.y, 2, Color.INDIANRED));
                System.out.println(c);
            }
        }
        /*if (!legalPosition(tempLine.getCoordinates()[0]) || !legalPosition(tempLine.getCoordinates()[1])) {
            return false;
        }*//*
        *//*tempPoint.getCoordinate().x = tempLine.getCoordinates()[0].x;
        tempPoint.getCoordinate().y = tempLine.getCoordinates()[0].y;*//*

        tempPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(x1, y1)}), GeometryOperations.factory);
        if (!legalPosition(tempPoint)) {
            if (help) {
                System.out.println("wtf: " + polygon.distance(tempPoint));
            }
            return false;
        }
        boolean outside1 = !polygon.covers(tempPoint);
        boolean onBoundary1 = boundary.distance(tempPoint) < GeometryOperations.PRECISION_EPSILON;

        tempPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(x2, y2)}), GeometryOperations.factory);
        if (!legalPosition(tempPoint)) {
            if (help) {
                System.out.println("wtf: " + polygon.distance(tempPoint));
            }
            return false;
        }
        boolean outside2 = !polygon.covers(tempPoint);
        boolean onBoundary2 = boundary.distance(tempPoint) < GeometryOperations.PRECISION_EPSILON;

        int nrIntersections = boundary.intersection(tempLine).getCoordinates().length;
        if (help) {
            System.out.println("nrIntersections: " + nrIntersections);
        }

        *//*if (nrIntersections <= 2 && ((outside1 && outside2) || (outside1 && onBoundary2) || (outside2 && onBoundary1))) {
            if (help) {
            System.out.println("Case 1");
        }
            return true;
        }
        if (outside1 && nrIntersections <= 1) {
            if (help) {
            System.out.println("Case 2");
        }
            return true;
        }
        if (outside2 && nrIntersections <= 1) {
            if (help) {
            System.out.println("Case 3");
        }
            return true;
        }*//*

        if (onBoundary1 && onBoundary2 && nrIntersections <= 2) {
            return true;
        }
        if (onBoundary1 && nrIntersections <= 1) {
            return true;
        }
        if (onBoundary2 && nrIntersections <= 1) {
            return true;
        }*/
        return false;

        // check whether the second controlledAgents is visible from the position of the first controlledAgents
        // (given its field of view and the structure of the map)
        /*if (!GeometryOperations.inPolygon(borderPolygon, x1, y1, x2, y2)) {
            return false;
        }
        for (Polygon p : obstaclePolygons) {
            if (GeometryOperations.inPolygonWithoutBorder(p, x1, y1, x2, y2) || GeometryOperations.inPolygonWithoutBorder(p, x1, y1) || GeometryOperations.inPolygonWithoutBorder(p, x2, y2)) {
                return false;
            }
        }
        LineSegment l = new LineSegment(x1, y1, x2, y2);
        Coordinate c;
        for (LineSegment ls : allLines) {
            c = ls.intersection(l);
            *//*if (c != null && !(c.equals2D(l.getCoordinate(0)) || c.equals2D(l.getCoordinate(1)) || c.equals2D(ls.getCoordinate(0)) || c.equals2D(ls.getCoordinate(1)))) {
                return false;
            }*//*
            if (c != null && !(c.equals2D(l.getCoordinate(0)) || c.equals2D(l.getCoordinate(1)) || c.equals2D(ls.getCoordinate(0)) || c.equals2D(ls.getCoordinate(1)))) {
                return false;
            }
        }
        return true;*/
    }

    public boolean isVisible(double x1, double y1, double x2, double y2, String string1, String string2) {
        System.out.println("\nisVisible-check for index " + string1 + " and " + string2);
        // check whether the second controlledAgents is visible from the position of the first controlledAgents
        // (given its field of view and the structure of the map)
        if (!GeometryOperations.inPolygon(borderPolygon, x1, y1, x2, y2)) {
            return false;
        }
        for (Polygon p : obstaclePolygons) {
            if (GeometryOperations.inPolygonWithoutBorder(p, x1, y1, x2, y2) || GeometryOperations.inPolygonWithoutBorder(p, x1, y1) || GeometryOperations.inPolygonWithoutBorder(p, x2, y2)) {
                return false;
            }
        }
        LineSegment l = new LineSegment(x1, y1, x2, y2);
        Coordinate c;
        for (LineSegment ls : allLines) {
            c = ls.intersection(l);
            /*if (c != null && !(c.equals2D(l.getCoordinate(0)) || c.equals2D(l.getCoordinate(1)) || c.equals2D(ls.getCoordinate(0)) || c.equals2D(ls.getCoordinate(1)))) {
                return false;
            }*/
            if (c != null && !(c.equals2D(l.getCoordinate(0)) || c.equals2D(l.getCoordinate(1)) || c.equals2D(ls.getCoordinate(0)) || c.equals2D(ls.getCoordinate(1)))) {
                System.out.println("There is an intersection\n");
                return false;
            }
        }
        System.out.println("There is no intersection\n");
        return true;
    }

    public boolean isVisible(Agent a1, Agent a2) {
        return isVisible(a1.getXPos(), a1.getYPos(), a2.getXPos(), a2.getYPos());
    }

    // methods for the controlledAgents to extract the knowledge it has access to

}
