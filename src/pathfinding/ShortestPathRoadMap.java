package pathfinding;

import additionalOperations.GeometryOperations;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import simulation.*;
import ui.Main;

import java.util.ArrayList;

public class ShortestPathRoadMap {

    public static boolean SHOW_ON_CANVAS = false;
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
        Line temp;
        for (Line l : excludedLines) {
            temp = new Line(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            temp.setStroke(Color.RED);
            temp.setStrokeWidth(2);
            Main.pane.getChildren().add(temp);
        }
        System.out.println("excludedLines.size(): " + this.excludedLines.size());
        System.out.println("excludedPolygons.size(): " + excludedPolygons.size());
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
                                System.out.printf("Point (%.3f|%.3f) has %d adjacent separating lines\n", currentPoints.get(j).getX(), currentPoints.get(j).getY(), l.size());
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
                                System.out.printf("Point (%.3f|%.3f) has %d adjacent separating lines\n", currentPoints.get(j).getX(), currentPoints.get(j).getY(), l.size());
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
                            shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(v1, v2), Math.sqrt(differenceSquared));
                        }
                    }
                }
            }
        }
    }

    public MapRepresentation getMap() {
        return map;
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

    public PlannedPath getShortestPath(PathVertex source, PathVertex sink) {
        PlannedPath plannedPath = new PlannedPath();

        // if there is a straight line between source and sink point just use that as shortest path
        if (map.isVisible(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeIntersectingPolygon(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeAdjacentToPolygon(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()) &&
                !isEdgeIntersectingLine(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY())) {

            for (PathVertex v : shortestPathGraph.vertexSet()) {
                if (v.getEstX() == source.getEstX() && v.getEstY() == source.getEstY()) {
                    source = v;
                    System.out.println("Contains source");
                    break;
                }
            }
            for (PathVertex v : shortestPathGraph.vertexSet()) {
                if (v.getEstX() == sink.getEstX() && v.getEstY() == sink.getEstY()) {
                    sink = v;
                    System.out.println("Contains sink");
                    break;
                }
            }

            plannedPath.addPathVertex(source);
            plannedPath.addPathVertex(sink);
            plannedPath.addLine(new Line(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY()));
            Line line = new Line(source.getEstX(), source.getEstY(), sink.getEstX(), sink.getEstY());
            line.setStrokeWidth(2);
            Label l = new Label("sz: " + excludedPolygons.size());
            l.setTranslateX(sink.getEstX());
            l.setTranslateY(sink.getEstY());
            //Main.pane.getChildren().addAll(line, l);
            return plannedPath;
        }

        // add new vertices to the graph
        boolean containsSource = false;
        for (PathVertex v : shortestPathGraph.vertexSet()) {
            if (v.getEstX() == source.getEstX() && v.getEstY() == source.getEstY()) {
                containsSource = true;
                source = v;
                System.out.println("Contains source");
                break;
            }
        }
        if (!containsSource) {
            shortestPathGraph.addVertex(source);
        }
        boolean containsSink = false;
        for (PathVertex v : shortestPathGraph.vertexSet()) {
            if (v.getEstX() == sink.getEstX() && v.getEstY() == sink.getEstY()) {
                containsSink = true;
                sink = v;
                System.out.println("Contains sink");
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
        if (!containsSource) {
            for (PathVertex pv : shortestPathGraph.vertexSet()) {
                if (!pv.equals(source) && !pv.equals(sink)) {
                    // check whether pv is visible from source (and whether there are separating polygons in between)
                    if (map.isVisible(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                            !isEdgeIntersectingPolygon(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                            !isEdgeAdjacentToPolygon(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY()) &&
                            !isEdgeIntersectingLine(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY())) {
                        double differenceSquared = Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2);
                        shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(source, pv), Math.sqrt(differenceSquared));
                        /*if (drawLines) {
                            Line l = new Line(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY());
                            //l.setStroke(new Color(Math.random(), Math.random(), Math.random(), 1));
                            Main.pane.getChildren().add(l);
                            Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 1, Color.LIGHTBLUE));
                        }*/
                        System.out.printf("(%.4f|%.4f) visible from source (which is (%.4f|%.4f))\n", pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY());
                        Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 0.5, Color.DARKGRAY));

                        if (Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2) < minDistance) {
                            minDistance = Math.pow(pv.getEstX() - source.getEstX(), 2) + Math.pow(pv.getEstY() - source.getEstY(), 2);
                            minX = pv.getEstX();
                            minY = pv.getEstY();
                        }
                    }
                }
            }
        }
        //Main.pane.getChildren().add(new Circle(minX, minY, 1.5, Color.RED));

        // then for the sink
        if (!containsSink) {
            for (PathVertex pv : shortestPathGraph.vertexSet()) {
                if (!pv.equals(source) && !pv.equals(sink)) {
                    // check whether pv is visible from source (and whether there are separating polygons in between)
                    if (map.isVisible(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                            !isEdgeIntersectingPolygon(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                            !isEdgeAdjacentToPolygon(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY()) &&
                            !isEdgeIntersectingLine(pv.getEstX(), pv.getEstY(), sink.getEstX(), sink.getEstY())) {
                        double differenceSquared = Math.pow(pv.getEstX() - sink.getEstX(), 2) + Math.pow(pv.getEstY() - sink.getEstY(), 2);
                        shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(pv, sink), Math.sqrt(differenceSquared));
                        /*if (drawLines) {
                            Line l = new Line(pv.getEstX(), pv.getEstY(), source.getEstX(), source.getEstY());
                            l.setStroke(new Color(Math.random(), Math.random(), Math.random(), 1));
                            Main.pane.getChildren().add(l);
                            Main.pane.getChildren().add(new Circle(pv.getEstX(), pv.getEstY(), 1, Color.LIGHTBLUE));
                        }*/
                    }
                }
            }
        }

        // calculate the shortest path and construct the PlannedPath object which will be returned
        DijkstraShortestPath<PathVertex, DefaultWeightedEdge> shortestPathCalculator = new DijkstraShortestPath<>(shortestPathGraph);
        GraphPath<PathVertex, DefaultWeightedEdge> graphPath = shortestPathCalculator.getPath(source, sink);
        PathVertex pv1, pv2;
        try {
            plannedPath.addPathVertex(graphPath.getVertexList().get(0));
            for (int i = 0; i < graphPath.getVertexList().size() - 1; i++) {
                pv1 = graphPath.getVertexList().get(i);
                pv2 = graphPath.getVertexList().get(i + 1);
                plannedPath.addLine(new Line(pv1.getEstX(), pv1.getEstY(), pv2.getEstX(), pv2.getEstY()));
                plannedPath.addPathVertex(pv2);
                Line line = new Line(pv1.getEstX(), pv1.getEstY(), pv2.getEstX(), pv2.getEstY());
                line.setStrokeWidth(2);
                Label l = new Label("sz: " + excludedPolygons.size());
                l.setTranslateX(sink.getEstX());
                l.setTranslateY(sink.getEstY());
                //Main.pane.getChildren().addAll(line, l);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("containsSource: " + containsSource);
            System.out.println("containsSink: " + containsSink);

            //Main.pane.getChildren().add(new Circle(source.getEstX(), source.getEstY(), 50, Color.DARKGRAY));
            //Main.pane.getChildren().add(new Circle(sink.getEstX(), sink.getEstY(), 50, Color.DARKGRAY));

            AdaptedSimulation.masterPause("in ShortestPathRoadMap");
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
        }

        // remove the source and sink vertices
        if (!containsSource) {
            shortestPathGraph.removeVertex(source);
        }
        if (!containsSink) {
            shortestPathGraph.removeVertex(sink);
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


/*
    *//*
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(source.getEstX(), source.getEstY(), reflexPoints.get(i).getEstX(), reflexPoints.get(i).getEstY())) {
                double differenceSquared = (reflexPoints.get(i).getEstX() - source.getEstX()) * (reflexPoints.get(i).getEstX() - source.getEstX()) + (reflexPoints.get(i).getEstY() - source.getEstY()) * (reflexPoints.get(i).getEstY() - source.getEstY());
                // sightList.addEdge(source, reflexPoints.get(i));
                sightList.setEdgeWeight(sightList.addEdge(sourceVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(sink.getEstX(), sink.getEstY(), reflexPoints.get(i).getEstX(), reflexPoints.get(i).getEstY())) {
                double differenceSquared = (reflexPoints.get(i).getEstX() - sink.getEstX()) * (reflexPoints.get(i).getEstX() - sink.getEstX()) + (reflexPoints.get(i).getEstY() - sink.getEstY()) * (reflexPoints.get(i).getEstY() - sink.getEstY());
                // sightList.addWeightedEdge(sink, reflexPoints.get(i), Math.sqrt(differenceSquared));
                sightList.setEdgeWeight(sightList.addEdge(sinkVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        DijkstraShortestPath test = new DijkstraShortestPath(sightList);
        return test.getPath(sourceVertex, sinkVertex);
     *//*

    public PlannedPath getShortestPathVertices(MapRepresentation map, PathVertex source, PathVertex sink) {
        reflexInLineOfSight(map, findReflex(map));
        //GraphPath graphPath = calculateShortestPath(map, reflexInLineOfSight(map, findReflex(map)), source, sink);
        PlannedPath plannedPath = new PlannedPath();
       *//* PathVertex pv1, pv2;
        for (int i = 0; i < graphPath.getVertexList().size() - 1; i++) {
            pv1 = (PathVertex) graphPath.getVertexList().get(i);
            pv2 = (PathVertex) graphPath.getVertexList().get(i + 1);
            plannedPath.addLine(new Line(pv1.getEstX(), pv1.getEstY(), pv2.getEstX(), pv2.getEstY()));
        }*//*
        return plannedPath;
    }

    public GraphPath getShortestPath(MapRepresentation map, PathVertex source, PathVertex sink) {
        return calculateShortestPath(map, reflexInLineOfSight(map, findReflex(map)), source, sink);
    }

    public GraphPath generateGraphMap(MapRepresentation map, PathVertex source, PathVertex sink) {
        Polygon polygon = map.getBorderPolygon();

        double list[] = new double[polygon.getPoints().size()];
        int i = 0;
        for (Double entry : polygon.getPoints()) {
            list[i] = entry;
            i++;
        }


        double minx = -10;
        double miny = -10;

        double maxx = Double.MIN_VALUE;
        double maxy = Double.MIN_VALUE;


        for (int j = 0; j < list.length; j++) {
            if (j % 2 == 0) {
                if (list[j] > maxx) {
                    maxx = list[j];
                }
                if (list[j] < minx) {
                    minx = list[j];
                }

            } else {
                if (list[j] > maxy) {
                    maxy = list[j];
                }
                if (list[j] < miny) {
                    miny = list[j];
                }
            }
        }


        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> graph1 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        int index = 0;
        for (int p = (int) minx; minx < maxx; minx++) {
            for (double l = (int) miny; miny < maxy; miny++) {
                //if point(p,l) is in the area
                // set point(p,l) as a vertex

                // is point(p-1,l) , point(p-1,l-1), point(p,l-1) in the possible area
                //if yes point(p,l) has neighbour and other one has neighbour as point(p,l)
                if (map.legalPosition(p, l)) {
                    graph1.addVertex(new PathVertex(p, l));
                    if (map.legalPosition(p, l - 1)) {
                        graph1.addEdge(new PathVertex(p, l), new PathVertex(p, l - 1));
                    }
                    if (map.legalPosition(p - 1, l)) {
                        graph1.addEdge(new PathVertex(p, l), new PathVertex(p - 1, l));
                    }
                    if (map.legalPosition(p - 1, l - 1)) {
                        graph1.addEdge(new PathVertex(p, l), new PathVertex(p - 1, l - 1));
                    }


                }

            }


        }
        DijkstraShortestPath test = new DijkstraShortestPath(graph1);
        return test.getPath(source, sink);
    }

    private ArrayList<PathVertex> findReflex(MapRepresentation map) {
        ArrayList<PathVertex> reflexVertices = new ArrayList<>();

        ArrayList<Polygon> polygons = map.getAllPolygons();
        ArrayList<PathVertex> currentPoints;
        ArrayList<Integer> currentIndeces;
        PathVertex prevPoint, nextPoint;
        double prevVectorX, prevVectorY, nextVectorX, nextVectorY;
        for (int i = 0; i < polygons.size(); i++) {
            boolean isClockwise = GeometryOperations.isClockwise(polygons.get(i));
            currentPoints = GeometryOperations.polyToPoints(polygons.get(i));
            currentIndeces = new ArrayList<>();
            if (isClockwise) {
                // iterate over the points in normal order
                for (int j = 0; j < currentPoints.size(); j++) {
                    //System.out.printf("Checking (%.3f")
                    prevPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);
                    nextPoint = currentPoints.get((j + 1) % currentPoints.size());

                    prevVectorX = currentPoints.get(j).getEstX() - prevPoint.getEstX();
                    prevVectorY = currentPoints.get(j).getEstY() - prevPoint.getEstY();
                    nextVectorX = currentPoints.get(j).getEstX() - nextPoint.getEstX();
                    nextVectorY = currentPoints.get(j).getEstY() - nextPoint.getEstY();

                    // check if smallest rotation angle of first vector to second vector is positive or negative
                    // if it is positive, then the smallest rotation is clockwise
                    // the vertex is a reflex vertex on an obstacle polygon if the rotation is clockwise
                    double signedAngle = Math.atan2(-prevVectorY, prevVectorX) - Math.atan2(-nextVectorY, nextVectorX);
                    //double signedAngle = Math.min(calc, Math.PI - calc);
                    //if ((i > 0 && signedAngle < 0 && signedAngle > -Math.PI) || (i == 0 && signedAngle > 0 && signedAngle < Math.PI)) {
                    boolean check = i > 0 && ((signedAngle < 0 && signedAngle > -Math.PI) || signedAngle > Math.PI);
                    check = check || (i == 0 && ((signedAngle > 0 && signedAngle < Math.PI) || (signedAngle < -Math.PI)));
                    if (check) {
                        reflexVertices.add(new PathVertex(currentPoints.get(j)));
                        currentIndeces.add(j);
                    }
                    //Circle circle = new Circle(currentPoints.get(j).getEstX(), currentPoints.get(j).getEstY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getEstX() + 5);
                    label.setTranslateY(currentPoints.get(j).getEstY() + 5);
                    //Main.pane.getChildren().add(circle);
                    //Main.pane.getChildren().add(label);
                }
            } else {
                // iterate over the points in reverse order
                for (int j = currentPoints.size() - 1; j >= 0; j--) {
                    prevPoint = currentPoints.get((j + 1) % currentPoints.size());
                    nextPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);

                    prevVectorX = currentPoints.get(j).getEstX() - prevPoint.getEstX();
                    prevVectorY = currentPoints.get(j).getEstY() - prevPoint.getEstY();
                    nextVectorX = currentPoints.get(j).getEstX() - nextPoint.getEstX();
                    nextVectorY = currentPoints.get(j).getEstY() - nextPoint.getEstY();

                    // check if smallest rotation angle of first vector to second vector is positive or negative
                    // if it is positive, then the smallest rotation is clockwise
                    // the vertex is a reflex vertex on an obstacle polygon if the rotation is clockwise
                    double signedAngle = Math.atan2(-prevVectorY, prevVectorX) - Math.atan2(-nextVectorY, nextVectorX);
                    //double signedAngle = Math.min(calc, Math.PI - calc);
                    boolean check = i > 0 && ((signedAngle < 0 && signedAngle > -Math.PI) || signedAngle > Math.PI);
                    check = check || (i == 0 && ((signedAngle > 0 && signedAngle < Math.PI) || (signedAngle < -Math.PI)));
                    if (check) {
                        reflexVertices.add(new PathVertex(currentPoints.get(j)));
                        currentIndeces.add(j);
                    }
                    //Circle circle = new Circle(currentPoints.get(j).getEstX(), currentPoints.get(j).getEstY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getEstX() + 5);
                    label.setTranslateY(currentPoints.get(j).getEstY() + 5);
                    //Main.pane.getChildren().add(circle);
                    //Main.pane.getChildren().add(label);
                }
            }
            for (int j = 0; j < currentIndeces.size(); j++) {
                // if the points are adjacent in the polygon, i.e. their indeces are one apart
                if (Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == 1) {
                    // add an edge between them
                    Line line = new Line(reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY(), reflexVertices.get((j + 1) % currentIndeces.size()).getEstX(), reflexVertices.get((j + 1) % currentIndeces.size()).getEstY());
                    line.setStroke(Color.BLUE);
                    line.setStrokeWidth(1);
                    Main.pane.getChildren().add(line);
                }
            }

        }

        *//*ArrayList<Polygon> polygons = map.getAllPolygons();
        GeometryOperations geometryOperations = new GeometryOperations();
        ArrayList<PathVertex> reflex = new ArrayList<>();
        int reflexIndex = 0;
        //  ArrayList<javafx.geometry.PathVertex> polygon= geometryOperations.polyToPoints(polygons);
        PathVertex left;
        PathVertex right;
        for (int i = 0; i < polygons.size(); i++) {
            ArrayList<PathVertex> polygon = geometryOperations.polyToPoints(polygons.get(i));

            for (int j = 0; j < polygon.size(); j++) {
                if (j != 0 && j != polygon.size() - 1) {
                    left = polygon.get(j - 1);
                    right = polygon.get(j + 1);

                } else if (j == 0) {
                    left = polygon.get(polygon.size() - 1);
                    right = polygon.get(j + 1);
                } else {

                    left = polygon.get(j - 1);
                    right = polygon.get(0);

                }
                //Line between= new Line(left.getEstX(),left.getEstY(), right.getEstX(),right.getEstY());
                double x = (right.getEstX() - left.getEstX()) / 2;
                double y = (right.getEstY() - left.getEstY()) / 2;

                PathVertex mid = new PathVertex(right.getEstX() - x, right.getEstY() - y);

                PathVertex main = polygon.get(j);

                int count = 0;
                Line between = new Line(mid.getEstX(), mid.getEstY(), main.getEstX(), main.getEstY());
                boolean legal = map.legalPosition(mid.getEstX(), mid.getEstY());
                count = allIntersect(polygons, between).size();

                if (count % 2 == 0 && !legal) {
                    reflex.add(reflexIndex, new PathVertex(polygon.get(j)));
                    reflexIndex++;

                } else if (count % 2 == 1 && legal) {
                    reflex.add(reflexIndex, new PathVertex(polygon.get(j)));
                    reflexIndex++;
                }

                //map.legalPosition(mid.getEstX(),mid.getEstY());






                *//**//*
take 2 neighbouring points
find line between 2
make a line from any point on this line to the orriginal point


STILL TODO
	point of intersect betweeen 2 lines if inside polygon then
		crosses even number of boundaries?
			yes- it is not pointy
			no- it is pointy
	else if it is outside polygon
		crosse even number of boundaries
			yes- it is pointy
			no- it is not pointy

                for(int i =0; i<map.getAllPolygons().size();i++){
                if(lineIntersectInPoly(map.getAllPolygons().get(i), between))   {
                    count++;
                }





                while (!geometryOperations.polysIntersect(map.getAllPolygons(), between)){


                }

                *//**//*
                double leftvector = Math.toDegrees(Math.atan2(left.getEstX() - polygon.get(j).getEstX(), left.getEstY() - polygon.get(j).getEstY()));
                double rightvector = Math.toDegrees(Math.atan2(right.getEstX() - polygon.get(j).getEstX(), right.getEstY() - polygon.get(j).getEstY()));

                if (rightvector < 0) {
                    rightvector += 360;
                }
                if (leftvector < 0) {
                    leftvector += 360;
                }

                if ((leftvector + rightvector) % 360 > 180) {
                    reflex.add(reflexIndex, new PathVertex(polygon.get(j)));
                    reflexIndex++;
                }
*//**//*
            }


        }

        return reflex;*//*
        return reflexVertices;
    }

    private SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> reflexInLineOfSight(MapRepresentation map, ArrayList<PathVertex> reflexVertices) {
        double eps = 0.0001;
        double angle;
        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> graph1 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < reflexVertices.size(); i++) {
            //for (int j = 9; j >= i; j--) {
            for (int j = reflexVertices.size() - 1; j >= i; j--) {
                if (i != j) {
                    if (map.isVisible(reflexVertices.get(i).getEstX(), reflexVertices.get(i).getEstY(), reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY())) {
                        System.out.printf("Visibility between (%.2f|%.2f) and (%.2f|%.2f)\n", reflexVertices.get(i).getEstX(), reflexVertices.get(i).getEstY(), reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY());

                        angle = Math.toDegrees(Math.atan2(reflexVertices.get(i).getEstY() - reflexVertices.get(j).getEstY(), reflexVertices.get(i).getEstX() - reflexVertices.get(j).getEstX()));
                        PathVertex above, below, inside1, inside2;
                        above = new PathVertex(reflexVertices.get(i).getEstX() + eps, reflexVertices.get(i).getEstY() + eps * angle);
                        below = new PathVertex(reflexVertices.get(j).getEstX() - eps, reflexVertices.get(j).getEstY() - eps * angle);
                        inside1 = new PathVertex(reflexVertices.get(i).getEstX() - eps, reflexVertices.get(i).getEstY() - eps * angle);
                        inside2 = new PathVertex(reflexVertices.get(j).getEstX() + eps, reflexVertices.get(j).getEstY() + eps * angle);

                        double xDiff = (reflexVertices.get(i).getEstX() - reflexVertices.get(j).getEstX());
                        double yDiff = (reflexVertices.get(i).getEstY() - reflexVertices.get(j).getEstY());
                        double length = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
                        xDiff /= length;
                        yDiff /= length;
                        xDiff *= eps;
                        yDiff *= eps;
                        inside1 = new PathVertex(reflexVertices.get(i).getEstX() + xDiff, reflexVertices.get(i).getEstY() + yDiff);
                        inside2 = new PathVertex(reflexVertices.get(j).getEstX() - xDiff, reflexVertices.get(j).getEstY() - yDiff);
                        //if (map.legalPosition(above.getEstX(), above.getEstY()) && map.legalPosition(below.getEstX(), below.getEstY()) && map.legalPosition(inside1.getEstX(), inside1.getEstY()) && map.legalPosition(inside2.getEstX(), inside2.getEstY())) {
                        if (map.legalPosition(inside1.getEstX(), inside1.getEstY()) && map.legalPosition(inside2.getEstX(), inside2.getEstY())) {
                            PathVertex v1 = reflexVertices.get(i);
                            PathVertex v2 = reflexVertices.get(j);

                            graph1.addVertex(v1);
                            graph1.addVertex(v2);

                            double differenceSquare = (reflexVertices.get(i).getEstX() - reflexVertices.get(j).getEstX()) * (reflexVertices.get(i).getEstX() - reflexVertices.get(j).getEstX()) + (reflexVertices.get(i).getEstY() - reflexVertices.get(j).getEstY()) * (reflexVertices.get(i).getEstY() - reflexVertices.get(j).getEstY());
                            // graph1.addWeightedEdge(v1, v2, Math.sqrt(differenceSquare));

                            graph1.setEdgeWeight(graph1.addEdge(v1, v2), Math.sqrt(differenceSquare));

                            Line line = new Line(reflexVertices.get(i).getEstX(), reflexVertices.get(i).getEstY(), reflexVertices.get(j).getEstX(), reflexVertices.get(j).getEstY());
                            line.setStroke(Color.BLUE);
                            line.setStrokeWidth(1);
                            Main.pane.getChildren().add(line);
                        }
                    }
                }
            }
        }
        return graph1;
    }

    private GraphPath calculateShortestPath(MapRepresentation map, SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> sightList, PathVertex source, PathVertex sink) {
        ArrayList<Polygon> polygons = map.getAllPolygons();

        PathVertex sourceVertex = new PathVertex(source);
        PathVertex sinkVertex = new PathVertex(sink);

        sightList.addVertex(sourceVertex);
        sightList.addVertex(sinkVertex);
        ArrayList<PathVertex> reflexPoints = findReflex(map);

        for (PathVertex pv : reflexPoints) {
            sightList.addVertex(pv);
        }

        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(source.getEstX(), source.getEstY(), reflexPoints.get(i).getEstX(), reflexPoints.get(i).getEstY())) {
                double differenceSquared = (reflexPoints.get(i).getEstX() - source.getEstX()) * (reflexPoints.get(i).getEstX() - source.getEstX()) + (reflexPoints.get(i).getEstY() - source.getEstY()) * (reflexPoints.get(i).getEstY() - source.getEstY());
                // sightList.addEdge(source, reflexPoints.get(i));
                sightList.setEdgeWeight(sightList.addEdge(sourceVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(sink.getEstX(), sink.getEstY(), reflexPoints.get(i).getEstX(), reflexPoints.get(i).getEstY())) {
                double differenceSquared = (reflexPoints.get(i).getEstX() - sink.getEstX()) * (reflexPoints.get(i).getEstX() - sink.getEstX()) + (reflexPoints.get(i).getEstY() - sink.getEstY()) * (reflexPoints.get(i).getEstY() - sink.getEstY());
                // sightList.addWeightedEdge(sink, reflexPoints.get(i), Math.sqrt(differenceSquared));
                sightList.setEdgeWeight(sightList.addEdge(sinkVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        DijkstraShortestPath test = new DijkstraShortestPath(sightList);
        return test.getPath(sourceVertex, sinkVertex);
    }*/

    //o
    // how to find reflex vertices
    // take the two neighbours
    // find the vector directed to the middle point

    //find all reflex vertices
    // set edge between neighbours and weight as distance
    // for all reflex vertices find all it has line of sight to
    // generate vector from first to second and vice versa
    // if x+epsilon for each vector is in playable region then set edge and weight as distance


    // test if direct line of sight is possible for 2 positions
    //if no
    // run djikstra on all starting vertices i.e. the ones source can see and end vertices i.e. ones sink can see
    // add on the distance form each starting vertex and end vertex to source and sink
    // take shortest path.

}

//graph construction - list of vertices that each contain an edge wich each contain a vertices with a weight.