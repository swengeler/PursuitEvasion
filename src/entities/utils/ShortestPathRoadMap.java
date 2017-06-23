package entities.utils;

import additionalOperations.GeometryOperations;
import com.vividsolutions.jts.geom.Coordinate;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import maps.MapRepresentation;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import ui.Main;

import java.util.ArrayList;
import java.util.Set;

public class ShortestPathRoadMap {

    public static boolean SHOW_ON_CANVAS = false;
    public static boolean DRAW_VISION_LINES = false;
    public static boolean EVADER_DEBUGGING = false;
    public static boolean drawLines = false;

    private MapRepresentation map;
    private ArrayList<Polygon> excludedPolygons;
    private ArrayList<Line> excludedLines;

    private SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph;

    public ShortestPathRoadMap(MapRepresentation map) {
        this.map = map;
        excludedPolygons = new ArrayList<>();
        excludedLines = new ArrayList<>();
        init();
        /*ArrayList<PathVertex> reflexVertices = findReflex(map);
        for (PathVertex pv : reflexVertices) {
            Circle circle = new Circle(pv.getEstX(), pv.getEstY(), 5, Color.CYAN);
            Main.pane.getChildren().add(circle);
        }*/
    }

    public ShortestPathRoadMap(MapRepresentation map, ArrayList<DTriangle> excludedTriangles) {
        this.map = map;
        excludedPolygons = new ArrayList<>();
        excludedLines = new ArrayList<>();
        Polygon p;
        for (DTriangle dt : excludedTriangles) {
            p = new Polygon();
            p.getPoints().addAll(
                    dt.getPoint(0).getX(), dt.getPoint(0).getY(),
                    dt.getPoint(1).getX(), dt.getPoint(1).getY(),
                    dt.getPoint(2).getX(), dt.getPoint(2).getY()
            );
            p.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.2));
            Main.pane.getChildren().add(p);
            excludedPolygons.add(p);
        }
        init();
    }

    public ShortestPathRoadMap(ArrayList<Line> excludedLines, MapRepresentation map) {
        this.map = map;
        excludedPolygons = new ArrayList<>();
        this.excludedLines = excludedLines;
        /*Line temp;
        for (Line l : excludedLines) {
            temp = new Line(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            temp.setStroke(Color.RED);
            temp.setStrokeWidth(2);
            Main.pane.getChildren().add(temp);
        }*/
        //System.out.println("excludedLines.size(): " + this.excludedLines.size());
        //System.out.println("excludedPolygons.size(): " + excludedPolygons.size());
        init();
    }

    private void init() {
        double distanceToActual = 1;
        shortestPathGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // calculate reflex vertices
        ArrayList<PathVertex> reflexVertices = new ArrayList<>();
        ArrayList<int[]> pointers = new ArrayList<>();

        ArrayList<Polygon> polygons = map.getAllPolygons();

        ArrayList<ArrayList<PathVertex>> allPoints = new ArrayList<>();
        ArrayList<PathVertex> currentPoints;
        ArrayList<Integer> currentIndeces;
        int[] singlePointer; // {index of polygon, index of previous point, index of next point}
        PathVertex prevPoint, nextPoint;
        int prevNrReflexVertices;
        for (int i = 0; i < polygons.size(); i++) {
            prevNrReflexVertices = reflexVertices.size();
            boolean isClockwise = GeometryOperations.isClockwise(polygons.get(i));
            currentPoints = GeometryOperations.polyToPathVertices(polygons.get(i));
            allPoints.add(currentPoints);
            currentIndeces = new ArrayList<>();
            if (!isClockwise) {
                for (int j = currentPoints.size() - 1; j >= 0; j--) {
                    prevPoint = currentPoints.get((j + 1) % currentPoints.size());
                    nextPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);

                    // check if it is a reflex vertex
                    if ((polygons.get(i) != map.getBorderPolygon() && GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint)) ||
                            (polygons.get(i) == map.getBorderPolygon() && !GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint))) {

                        // get point "just off the tip" of the reflex vertex
                        double prevDeltaX = currentPoints.get(j).getX() - currentPoints.get((j + 1) % currentPoints.size()).getX();
                        double prevDeltaY = currentPoints.get(j).getY() - currentPoints.get((j + 1) % currentPoints.size()).getY();
                        double prevLength = Math.sqrt(Math.pow(prevDeltaX, 2) + Math.pow(prevDeltaY, 2));
                        prevDeltaX /= prevLength;
                        prevDeltaY /= prevLength;

                        double nextDeltaX = currentPoints.get(j).getX() - currentPoints.get(j - 1 < 0 ? currentPoints.size() - 1 : j - 1).getX();
                        double nextDeltaY = currentPoints.get(j).getY() - currentPoints.get(j - 1 < 0 ? currentPoints.size() - 1 : j - 1).getY();
                        double nextLength = Math.sqrt(Math.pow(nextDeltaX, 2) + Math.pow(nextDeltaY, 2));
                        nextDeltaX /= nextLength;
                        nextDeltaY /= nextLength;

                        double averageX = (prevDeltaX + nextDeltaX) / 2;
                        double averageY = (prevDeltaY + nextDeltaY) / 2;

                        double newX = currentPoints.get(j).getX() + averageX * distanceToActual;
                        double newY = currentPoints.get(j).getY() + averageY * distanceToActual;

                        // if there are separating triangles (or polygons, technically), check whether the vertex should be moved)
                        // polygons which touch with a corner take precedence
                        boolean removePoint = false;
                        if (!excludedPolygons.isEmpty() || !excludedLines.isEmpty()) {
                            if (!excludedPolygons.isEmpty()) {
                                ArrayList<Polygon> p = getCornerAdjacentPolygons(currentPoints.get(j), polygons.get(i));
                                for (int k = 0; !removePoint && k < p.size(); k++) {
                                    if (p.get(k).contains(newX, newY)) {
                                        removePoint = true;
                                    }
                                }
                                if (!removePoint && !p.isEmpty()) {
                                    currentPoints.set(j, new PathVertex(newX, newY));
                                }
                            } else if (!excludedLines.isEmpty()) {
                                ArrayList<Line> l = getPointAdjacentLines(currentPoints.get(j));
                                //System.out.printf("Point (%.3f|%.3f) has %d adjacent separating lines\n", currentPoints.get(j).getX(), currentPoints.get(j).getY(), l.size());
                                for (int k = 0; !removePoint && k < l.size(); k++) {
                                    if (GeometryOperations.angle(newX, newY, l.get(k).getStartX() - l.get(k).getEndX(), l.get(k).getStartY() - l.get(k).getEndY()) < 2) {
                                        removePoint = true;
                                    }
                                }
                                /*if (!removePoint && !l.isEmpty()) {
                                    currentPoints.set(j, new PathVertex(newX, newY));
                                }*/
                            }
                        }

                        currentPoints.get(j).setEstX(newX);
                        currentPoints.get(j).setEstY(newY);
                        //currentPoints.set(j, new PathVertex(newX, newY));

                        if (!removePoint) {
                            reflexVertices.add(currentPoints.get(j));
                            singlePointer = new int[]{i, (j + 1) % currentPoints.size(), j, j == 0 ? currentPoints.size() - 1 : j - 1};
                            pointers.add(singlePointer);
                            currentIndeces.add(j);

                            shortestPathGraph.addVertex(reflexVertices.get(reflexVertices.size() - 1));

                            if (SHOW_ON_CANVAS) {
                                Circle circle = new Circle(currentPoints.get(j).getEstX(), currentPoints.get(j).getEstY(), 5, Color.GREEN);
                                Main.pane.getChildren().add(circle);
                                Label index = new Label("" + j);
                                index.setTranslateX(circle.getCenterX() + 5);
                                index.setTranslateY(circle.getCenterY() + 5);
                                Main.pane.getChildren().add(index);
                            }
                        }
                    }
                }
            } else {
                for (int j = 0; j < currentPoints.size(); j++) {
                    prevPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);
                    nextPoint = currentPoints.get((j + 1) % currentPoints.size());

                    if ((polygons.get(i) != map.getBorderPolygon() && GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint)) ||
                            (polygons.get(i) == map.getBorderPolygon() && !GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint))) {

                        // get point "just off the tip" of the reflex vertex
                        double prevDeltaX = currentPoints.get(j).getX() - currentPoints.get(j - 1 < 0 ? currentPoints.size() - 1 : j - 1).getX();
                        double prevDeltaY = currentPoints.get(j).getY() - currentPoints.get(j - 1 < 0 ? currentPoints.size() - 1 : j - 1).getY();
                        double prevLength = Math.sqrt(Math.pow(prevDeltaX, 2) + Math.pow(prevDeltaY, 2));
                        prevDeltaX /= prevLength;
                        prevDeltaY /= prevLength;

                        double nextDeltaX = currentPoints.get(j).getX() - currentPoints.get((j + 1) % currentPoints.size()).getX();
                        double nextDeltaY = currentPoints.get(j).getY() - currentPoints.get((j + 1) % currentPoints.size()).getY();
                        double nextLength = Math.sqrt(Math.pow(nextDeltaX, 2) + Math.pow(nextDeltaY, 2));
                        nextDeltaX /= nextLength;
                        nextDeltaY /= nextLength;

                        double averageX = (prevDeltaX + nextDeltaX) / 2;
                        double averageY = (prevDeltaY + nextDeltaY) / 2;

                        double newX = currentPoints.get(j).getX() + averageX * distanceToActual;
                        double newY = currentPoints.get(j).getY() + averageY * distanceToActual;

                        boolean removePoint = false;
                        if (!excludedPolygons.isEmpty() || !excludedLines.isEmpty()) {
                            if (!excludedPolygons.isEmpty()) {
                                ArrayList<Polygon> p = getCornerAdjacentPolygons(currentPoints.get(j), polygons.get(i));
                                for (int k = 0; !removePoint && k < p.size(); k++) {
                                    if (p.get(k).contains(newX, newY)) {
                                        removePoint = true;
                                    }
                                }
                                if (!removePoint && !p.isEmpty()) {
                                    currentPoints.set(j, new PathVertex(newX, newY));
                                }
                            } else if (!excludedLines.isEmpty()) {
                                ArrayList<Line> l = getPointAdjacentLines(currentPoints.get(j));
                                //System.out.printf("Point (%.3f|%.3f) has %d adjacent separating lines\n", currentPoints.get(j).getX(), currentPoints.get(j).getY(), l.size());
                                for (int k = 0; !removePoint && k < l.size(); k++) {
                                    if (GeometryOperations.angle(newX, newY, l.get(k).getStartX() - l.get(k).getEndX(), l.get(k).getStartY() - l.get(k).getEndY()) < 2) {
                                        removePoint = true;
                                    }
                                }
                                /*if (!removePoint && !l.isEmpty()) {
                                    currentPoints.set(j, new PathVertex(newX, newY));
                                }*/
                            }
                        }

                        currentPoints.get(j).setEstX(newX);
                        currentPoints.get(j).setEstY(newY);
                        //currentPoints.set(j, new PathVertex(newX, newY));

                        if (!removePoint) {
                            reflexVertices.add(currentPoints.get(j));
                            singlePointer = new int[]{i, j == 0 ? currentPoints.size() - 1 : j - 1, j, (j + 1) % currentPoints.size()};
                            pointers.add(singlePointer);
                            currentIndeces.add(j);

                            shortestPathGraph.addVertex(reflexVertices.get(reflexVertices.size() - 1));

                            if (SHOW_ON_CANVAS) {
                                Circle circle = new Circle(currentPoints.get(j).getEstX(), currentPoints.get(j).getEstY(), 5, Color.GREEN);
                                Main.pane.getChildren().add(circle);
                                Label index = new Label("" + j);
                                index.setTranslateX(circle.getCenterX() + 5);
                                index.setTranslateY(circle.getCenterY() + 5);
                                Main.pane.getChildren().add(index);
                            }
                        }
                    }
                }
            }
            // check for adjacent reflex vertices
            for (int j = 0; j < currentIndeces.size(); j++) {
                // if the points are adjacent in the polygon, i.e. their indeces are one apart
                if (Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == 1 || Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == currentPoints.size() - 1) {
                    // add an edge between them
                    PathVertex v1 = reflexVertices.get(prevNrReflexVertices + j);
                    PathVertex v2 = reflexVertices.get(prevNrReflexVertices + (j + 1) % currentIndeces.size());
                    double differenceSquared = Math.pow(v1.getEstX() - v2.getEstX(), 2) + Math.pow(v1.getEstY() - v2.getEstY(), 2);

                    // check if edge is "covered" by excluded polygon
                    //if (!isEdgeAdjacentToPolygon(v1.getX(), v1.getY(), v2.getX(), v2.getY()) && !isEdgeIntersectingPolygon(v1.getX(), v1.getY(), v2.getX(), v2.getY()) && !isEdgeIntersectingLine(v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
                    if (!isEdgeAdjacentToPolygon(v1.getEstX(), v1.getEstY(), v2.getEstX(), v2.getEstY()) && !isEdgeIntersectingPolygon(v1.getEstX(), v1.getEstY(), v2.getEstX(), v2.getEstY()) && !isEdgeIntersectingLine(v1.getEstX(), v1.getEstY(), v2.getEstX(), v2.getEstY())) {
                        DefaultWeightedEdge edge = shortestPathGraph.addEdge(v1, v2);
                        if (edge != null) {
                            shortestPathGraph.setEdgeWeight(edge, Math.sqrt(differenceSquared));
                            if (SHOW_ON_CANVAS) {
                                Line line = new Line(v1.getEstX(), v1.getEstY(), v2.getEstX(), v2.getEstY());
                                line.setStroke(Color.GREEN);
                                line.setStrokeWidth(2);
                                Main.pane.getChildren().add(line);
                                line.toFront();
                            }
                        }
                    }
                }
            }
        }

        // checking for non-adjacent reflex vertices that should have a bitangent edge between them
        // points are assigned as described in "Planning Algorithms" p.264
        PathVertex p1, p2, p3, p4, p5, p6;
        Line excludedIntersectionLine;
        for (int i = 0; i < reflexVertices.size(); i++) {
            singlePointer = pointers.get(i);
            p1 = allPoints.get(singlePointer[0]).get(singlePointer[1]); // previous point (counter clockwise order)
            p2 = allPoints.get(singlePointer[0]).get(singlePointer[2]); // reflex vertex point
            p3 = allPoints.get(singlePointer[0]).get(singlePointer[3]); // next point
            //excludedIntersectionLine = new Line(p2.getX(), p2.getY(), 0, 0);
            excludedIntersectionLine = new Line(p2.getEstX(), p2.getEstY(), 0, 0);
            for (int j = i + 1; j < reflexVertices.size(); j++) {
                //if (map.isVisible(reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY())) {
                if (map.isVisible(reflexVertices.get(i).getEstX(), reflexVertices.get(i).getEstY(), reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY())) {

                    singlePointer = pointers.get(j);
                    p4 = allPoints.get(singlePointer[0]).get(singlePointer[1]);
                    p5 = allPoints.get(singlePointer[0]).get(singlePointer[2]);
                    p6 = allPoints.get(singlePointer[0]).get(singlePointer[3]);

                    boolean intersection = false;
                    if (!excludedPolygons.isEmpty() || !excludedLines.isEmpty()) {
                        //excludedIntersectionLine.setEndX(p5.getX());
                        //excludedIntersectionLine.setEndY(p5.getY());
                        excludedIntersectionLine.setEndX(p5.getEstX());
                        excludedIntersectionLine.setEndY(p5.getEstY());
                        for (int k = 0; !intersection && k < excludedPolygons.size(); k++) {
                            if (GeometryOperations.lineIntersectInPoly(excludedPolygons.get(k), excludedIntersectionLine)) {
                                intersection = true;
                            }
                        }
                        for (int k = 0; !intersection && k < excludedLines.size(); k++) {
                            if (GeometryOperations.lineIntersect(excludedLines.get(k), excludedIntersectionLine)) {
                                intersection = true;
                            }
                        }
                    }

                    if (!intersection) {
                        // check whether there should be an edge between the reflex vertices
                        boolean check = GeometryOperations.leftTurnPredicate(p1, p2, p5) ^ GeometryOperations.leftTurnPredicate(p3, p2, p5);
                        check = check || (GeometryOperations.leftTurnPredicate(p4, p5, p2) ^ GeometryOperations.leftTurnPredicate(p6, p5, p2));
                        if (!check) {
                            // there should be an edge
                            Line line = new Line(reflexVertices.get(i).getEstX(), reflexVertices.get(i).getEstY(), reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY());
                            if (SHOW_ON_CANVAS) {
                                line.setStroke(Color.GREEN);
                                line.setStrokeWidth(2);
                                Main.pane.getChildren().add(line);
                                line.toFront();
                            }

                            PathVertex v1 = reflexVertices.get(i);
                            PathVertex v2 = reflexVertices.get(j);
                            double differenceSquared = Math.pow(v1.getEstX() - v2.getEstX(), 2) + Math.pow(v1.getEstY() - v2.getEstY(), 2);
                            DefaultWeightedEdge edge = shortestPathGraph.addEdge(v1, v2);
                            if (edge != null) {
                                shortestPathGraph.setEdgeWeight(edge, Math.sqrt(differenceSquared));
                            } else {
                                System.out.println("1: " + shortestPathGraph.containsEdge(v1, v2));
                                System.out.println("2: " + shortestPathGraph.containsEdge(shortestPathGraph.getEdgeFactory().createEdge(v1, v2)));
                                shortestPathGraph.getEdgeFactory().createEdge(v1, v2);
                            }
                        }
                    }
                }
            }
        }
    }

    public MapRepresentation getMap() {
        return map;
    }

    public void addExtraVertex(PathVertex vertex) {
        for (PathVertex v : shortestPathGraph.vertexSet()) {
            if (v.getEstX() == vertex.getEstX() && v.getEstY() == vertex.getEstY()) {
                // don't add the vertex if it is already in the graph
                return;
            }
        }

        // add the vertex to the graph and connect it to all visible vertices around it
        shortestPathGraph.addVertex(vertex);
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            if (!pv.equals(vertex)) {
                // check whether pv is visible from source (and whether there are separating polygons in between)
                if (map.isVisible(pv.getEstX(), pv.getEstY(), vertex.getEstX(), vertex.getEstY()) &&
                        !isEdgeIntersectingPolygon(pv.getEstX(), pv.getEstY(), vertex.getEstX(), vertex.getEstY()) &&
                        !isEdgeAdjacentToPolygon(pv.getEstX(), pv.getEstY(), vertex.getEstX(), vertex.getEstY()) &&
                        !isEdgeIntersectingLine(pv.getEstX(), pv.getEstY(), vertex.getEstX(), vertex.getEstY())) {
                    double differenceSquared = Math.pow(pv.getEstX() - vertex.getEstX(), 2) + Math.pow(pv.getEstY() - vertex.getEstY(), 2);
                    shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(vertex, pv), Math.sqrt(differenceSquared));
                }
            }
        }
    }

    public void addExtraVertices(ArrayList<Coordinate> coordinates) {
        for (Coordinate c : coordinates) {
            addExtraVertex(new PathVertex(c.x, c.y));
        }
    }

    public Set<PathVertex> getVertices() {
        return shortestPathGraph.vertexSet();
    }

    public PlannedPath getShortestPath(Point2D source, double sinkX, double sinkY) {
        return getShortestPath(new PathVertex(source), new PathVertex(sinkX, sinkY));
    }

    public PlannedPath getShortestPath(double sourceX, double sourceY, Point2D sink) {
        return getShortestPath(new PathVertex(sourceX, sourceY), new PathVertex(sink));
    }

    public PlannedPath getShortestPath(double sourceX, double sourceY, double sinkX, double sinkY) {
        return getShortestPath(new PathVertex(sourceX, sourceY), new PathVertex(sinkX, sinkY));
    }

    public PlannedPath getShortestPath(Point2D source, Point2D sink) {
        return getShortestPath(new PathVertex(source), new PathVertex(sink));
    }

    public void drawVerts() {
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 4, Color.GREEN));
        }
    }

    public PlannedPath getShortestPath(PathVertex source, PathVertex sink) {
        PlannedPath plannedPath = new PlannedPath();

        // if there is a straight line between source and sink point just use that as shortest path
        if (map.isVisible(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeIntersectingPolygon(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeAdjacentToPolygon(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeIntersectingLine(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY())) {

            for (PathVertex v : shortestPathGraph.vertexSet()) {
                if ((v.getEstX() == source.getEstX() && v.getEstY() == source.getEstY()) || (v.getRealX() == source.getRealX() && v.getRealY() == source.getRealY())) {
                    source = v;
                    //System.out.println("Contains source");
                    break;
                }
            }
            for (PathVertex v : shortestPathGraph.vertexSet()) {
                if ((v.getEstX() == sink.getEstX() && v.getEstY() == sink.getEstY()) || (v.getRealX() == sink.getRealX() && v.getRealY() == sink.getRealY())) {
                    sink = v;
                    //System.out.println("Contains sink");
                    break;
                }
            }

            plannedPath.addPathVertex(source);
            plannedPath.addPathVertex(sink);
            plannedPath.addLine(new PathLine(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()));
            Line line = new Line(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY());
            line.setStrokeWidth(2);
            Label l = new Label("sz: " + excludedPolygons.size());
            l.setTranslateX(sink.getEstX());
            l.setTranslateY(sink.getEstY());
            //Main.pane.getChildren().addAll(line, l);
            //System.out.println("Shortest path is straight line");
            return plannedPath;
        }

        // add new vertices to the graph
        boolean containsSource = false;
        for (PathVertex v : shortestPathGraph.vertexSet()) {
            if ((v.getEstX() == source.getEstX() && v.getEstY() == source.getEstY()) || (v.getRealX() == source.getRealX() && v.getRealY() == source.getRealY())) {
                containsSource = true;
                source = v;
                //System.out.println("Contains source");
                break;
            }
        }
        if (!containsSource) {
            shortestPathGraph.addVertex(source);
        }
        boolean containsSink = false;
        for (PathVertex v : shortestPathGraph.vertexSet()) {
            if ((v.getEstX() == sink.getEstX() && v.getEstY() == sink.getEstY()) || (v.getRealX() == sink.getRealX() && v.getRealY() == sink.getRealY())) {
                containsSink = true;
                sink = v;
                //System.out.println("Contains sink");
                break;
            }
        }
        if (!containsSink) {
            shortestPathGraph.addVertex(sink);
        }

        double minDistance = Double.MAX_VALUE;
        double minX = 0, minY = 0;


        // add new edges between source and sink and the visible nodes of the graph
        // first for the source
        DefaultWeightedEdge temp;
        ArrayList<DefaultWeightedEdge> sourceToVisible = new ArrayList<>();
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            if (!pv.equals(source) && !pv.equals(sink)) {
                // check whether pv is visible from source (and whether there are separating polygons in between)
                if (map.isVisible(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                        !isEdgeIntersectingPolygon(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                        !isEdgeAdjacentToPolygon(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                        !isEdgeIntersectingLine(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY())) {
                    double differenceSquared = Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2);
                    temp = shortestPathGraph.addEdge(source, pv);
                    if (temp != null) {
                        if (containsSource) {
                            sourceToVisible.add(temp);
                        }
                        shortestPathGraph.setEdgeWeight(temp, Math.sqrt(differenceSquared));
                    }
                    if (DRAW_VISION_LINES) {
                        Line l = new Line(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY());
                        l.setStroke(Color.LAWNGREEN);
                        Main.pane.getChildren().add(l);
                        Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 2, Color.LAWNGREEN));
                    }
                    //System.out.printf("(%.4f|%.4f) visible from source (which is (%.4f|%.4f))\n", pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY());
                    //Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 0.5, Color.DARKGRAY));

                    if (Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2) < minDistance) {
                        minDistance = Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2);
                        minX = pv.getEstX();
                        minY = pv.getEstY();
                    }
                }
            }
        }
        //Main.pane.getChildren().add(new Circle(minX, minY, 1.5, Color.RED));

        // then for the sink
        ArrayList<DefaultWeightedEdge> sinkToVisible = new ArrayList<>();
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            if (!pv.equals(source) && !pv.equals(sink)) {
                // check whether pv is visible from source (and whether there are separating polygons in between)
                if (map.isVisible(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                        !isEdgeIntersectingPolygon(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                        !isEdgeAdjacentToPolygon(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                        !isEdgeIntersectingLine(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY())) {
                    double differenceSquared = Math.pow(pv.getEstX() - sink.getEstX(), 2) + Math.pow(pv.getEstY() - sink.getEstY(), 2);
                    temp = shortestPathGraph.addEdge(pv, sink);
                    if (temp != null) {
                        if (containsSink) {
                            sinkToVisible.add(temp);
                        }
                        shortestPathGraph.setEdgeWeight(temp, Math.sqrt(differenceSquared));
                    }
                    if (DRAW_VISION_LINES) {
                        Line l = new Line(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY());
                        l.setStroke(Color.LIGHTBLUE);
                        Main.pane.getChildren().add(l);
                        Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 2, Color.LIGHTBLUE));
                    }
                }
            }
        }

        // calculate the shortest path and construct the PlannedPath object which will be returned
        DijkstraShortestPath<PathVertex, DefaultWeightedEdge> shortestPathCalculator = new DijkstraShortestPath<>(shortestPathGraph);
        GraphPath<PathVertex, DefaultWeightedEdge> graphPath = shortestPathCalculator.getPath(source, sink);
        PathVertex pv1, pv2;
        if (graphPath == null) {
            return null;
        }
        plannedPath.addPathVertex(graphPath.getVertexList().get(0));
        for (int i = 0; i < graphPath.getVertexList().size() - 1; i++) {
            pv1 = graphPath.getVertexList().get(i);
            pv2 = graphPath.getVertexList().get(i + 1);
            plannedPath.addLine(new PathLine(pv1.getEstX(), pv1.getEstY(), pv2.getEstX(), pv2.getEstY()));
            plannedPath.addPathVertex(pv2);
            Line line = new Line(pv1.getEstX(), pv1.getEstY(), pv2.getEstX(), pv2.getEstY());
            line.setStrokeWidth(2);
            Label l = new Label("sz: " + excludedPolygons.size());
            l.setTranslateX(sink.getEstX());
            l.setTranslateY(sink.getEstY());
            //Main.pane.getChildren().addAll(line, l);
        }
        //try {
        //} catch (NullPointerException e) {
           /* e.printStackTrace();
            System.out.println("containsSource: " + containsSource);
            System.out.println("containsSink: " + containsSink);*/

        //Main.pane.getChildren().add(new Circle(source.getEstX(), source.getEstY(), 50, Color.DARKGRAY));
        //Main.pane.getChildren().add(new Circle(sink.getEstX(), sink.getEstY(), 50, Color.DARKGRAY));

        //AdaptedSimulation.masterPause("in ShortestPathRoadMap");
            /*System.out.println("graphPath: " + graphPath);
            System.out.printf("Source: (%.3f|%.3f)\n", source.getEstX(), source.getEstY());
            System.out.printf("Sink: (%.3f|%.3f)\n", sink.getEstX(), sink.getEstY());

            for (PathVertex vertex : shortestPathGraph.vertexSet()) {
                Main.pane.getChildren().add(new Circle(vertex.getEstX(), vertex.getEstY(), 5, Color.CYAN));
            }
            Main.pane.getChildren().add(new Circle(source.getEstX(), source.getEstY(), 3, Color.ORANGE));
            Main.pane.getChildren().add(new Circle(sink.getEstX(), sink.getEstY(), 3, Color.ORANGE));

            for (DefaultWeightedEdge edge : shortestPathGraph.edgeSet()) {
                Main.pane.getChildren().add(new Line(shortestPathGraph.getEdgeSource(edge).getEstX(), shortestPathGraph.getEdgeSource(edge).getEstY(), shortestPathGraph.getEdgeTarget(edge).getEstX(), shortestPathGraph.getEdgeTarget(edge).getEstY()));
            }*/
        //}

        // remove the source and sink vertices
        if (!containsSource) {
            shortestPathGraph.removeVertex(source);
        } else {
            shortestPathGraph.removeAllEdges(sourceToVisible);
        }
        if (!containsSink) {
            shortestPathGraph.removeVertex(sink);
        } else {
            shortestPathGraph.removeAllEdges(sinkToVisible);
        }
        return plannedPath;
    }

    private ArrayList<Line> getPointAdjacentLines(PathVertex cornerPoint) {
        ArrayList<Line> result = new ArrayList<>();
        if (excludedLines == null) {
            return result;
        }
        for (Line l : excludedLines) {
            if ((l.getStartX() == cornerPoint.getX() && l.getStartY() == cornerPoint.getY()) || (l.getEndX() == cornerPoint.getX() && l.getEndY() == cornerPoint.getY())) {
                result.add(l);
            }
        }
        return result;
    }

    private ArrayList<Polygon> getCornerAdjacentPolygons(PathVertex cornerPoint, Polygon restOfPolygon) {
        if (excludedPolygons == null) {
            return new ArrayList<>();
        }
        ArrayList<Polygon> result = new ArrayList<>();
        for (Polygon p : excludedPolygons) {
            boolean cornerAdjacent = false;
            int cornerAdjacentIndex = -1;
            for (int i = 0; !cornerAdjacent && i < p.getPoints().size(); i += 2) {
                if (cornerPoint.getX() == (double) p.getPoints().get(i) && cornerPoint.getY() == (double) p.getPoints().get(i + 1)) {
                    cornerAdjacent = true;
                    cornerAdjacentIndex = i;
                }
            }
            if (cornerAdjacent) {
                int cornerCount = 0;
                for (int i = 0; i < p.getPoints().size(); i += 2) {
                    if (i != cornerAdjacentIndex) {
                        for (int j = 0; j < restOfPolygon.getPoints().size(); j += 2) {
                            if ((double) p.getPoints().get(i) == (double) restOfPolygon.getPoints().get(j) && (double) p.getPoints().get(i + 1) == (double) restOfPolygon.getPoints().get(j + 1)) {
                                cornerCount++;
                            }
                        }
                    }
                }
                if (cornerCount == 0) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private boolean isEdgeAdjacentToPolygon(double x1, double y1, double x2, double y2) {
        for (Polygon p : excludedPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                if ((p.getPoints().get(i) == x1 && p.getPoints().get(i + 1) == y1 && p.getPoints().get((i + 2) % p.getPoints().size()) == x2 && p.getPoints().get((i + 3) % p.getPoints().size()) == y2) ||
                        (p.getPoints().get(i) == x2 && p.getPoints().get(i + 1) == y2 && p.getPoints().get((i + 2) % p.getPoints().size()) == x1 && p.getPoints().get((i + 3) % p.getPoints().size()) == y1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEdgeIntersectingPolygon(double x1, double y1, double x2, double y2) {
        for (Polygon p : excludedPolygons) {
            if (GeometryOperations.lineIntersectInPoly(p, new Line(x1, y1, x2, y2))) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeIntersectingLine(double x1, double y1, double x2, double y2) {
        for (Line l : excludedLines) {
            if (GeometryOperations.lineIntersect(l, x1, y1, x2, y2)) {
                return true;
            }
        }
        return false;
    }

}