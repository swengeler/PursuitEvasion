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
import simulation.MapRepresentation;
import simulation.PlannedPath;
import ui.Main;

import java.util.ArrayList;

public class ShortestPathRoadMap {

    private MapRepresentation map;
    private ArrayList<Polygon> excludedPolygons;

    private SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph;

    public ShortestPathRoadMap(MapRepresentation map) {
        this.map = map;
        excludedPolygons = new ArrayList<>();
        init();
        /*ArrayList<PathVertex> reflexVertices = findReflex(map);
        for (PathVertex pv : reflexVertices) {
            Circle circle = new Circle(pv.getX(), pv.getY(), 5, Color.CYAN);
            Main.pane.getChildren().add(circle);
        }*/
    }

    public ShortestPathRoadMap(MapRepresentation map, ArrayList<DTriangle> excludedTriangles) {
        this.map = map;
        excludedPolygons = new ArrayList<>();
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

    private void init() {
        shortestPathGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // calculate reflex vertices
        ArrayList<PathVertex> reflexVertices = new ArrayList<>();
        ArrayList<int[]> pointers = new ArrayList<>();

        ArrayList<Polygon> polygons = map.getAllPolygons();

        ArrayList<ArrayList<Point2D>> allPoints = new ArrayList<>();
        ArrayList<Point2D> currentPoints;
        ArrayList<Integer> currentIndeces;
        int[] singlePointer; // {index of polygon, index of previous point, index of next point}
        Point2D prevPoint, nextPoint;
        int prevNrReflexVertices;
        for (int i = 0; i < polygons.size(); i++) {
            prevNrReflexVertices = reflexVertices.size();
            boolean isClockwise = GeometryOperations.isClockwise(polygons.get(i));
            currentPoints = GeometryOperations.polyToPoints(polygons.get(i));
            allPoints.add(currentPoints);
            currentIndeces = new ArrayList<>();
            if (!isClockwise) {
                for (int j = currentPoints.size() - 1; j >= 0; j--) {
                    prevPoint = currentPoints.get((j + 1) % currentPoints.size());
                    nextPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);

                    // check if it is a reflex vertex
                    if ((polygons.get(i) != map.getBorderPolygon() && GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint)) ||
                            (polygons.get(i) == map.getBorderPolygon() && !GeometryOperations.leftTurnPredicate(prevPoint, currentPoints.get(j), nextPoint))) {

                        // if there are separating triangles (or polygons, technically), check whether the vertex should be moved)
                        // polygons which touch with a corner take precedence
                        boolean removePoint = false;
                        if (excludedPolygons != null && excludedPolygons.size() != 0) {
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

                            double newX = currentPoints.get(j).getX() + averageX * 0.01;
                            double newY = currentPoints.get(j).getY() + averageY * 0.01;

                            ArrayList<Polygon> p = getCornerAdjacentPolygons(currentPoints.get(j), polygons.get(i));
                            for (int k = 0; !removePoint && k < p.size(); k++) {
                                if (p.get(k).contains(newX, newY)) {
                                    removePoint = true;
                                }
                            }
                            if (!removePoint && !p.isEmpty()) {
                                currentPoints.set(j, new Point2D(newX, newY));
                            }
                        }

                        if (!removePoint) {
                            reflexVertices.add(new PathVertex(currentPoints.get(j)));
                            singlePointer = new int[]{i, (j + 1) % currentPoints.size(), j, j == 0 ? currentPoints.size() - 1 : j - 1};
                            pointers.add(singlePointer);
                            currentIndeces.add(j);

                            shortestPathGraph.addVertex(reflexVertices.get(reflexVertices.size() - 1));

                            if (!excludedPolygons.isEmpty()) {
                                Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 5, Color.GREEN);
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

                        boolean removePoint = false;
                        if (excludedPolygons != null && excludedPolygons.size() != 0) {
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

                            double newX = currentPoints.get(j).getX() + averageX * 0.01;
                            double newY = currentPoints.get(j).getY() + averageY * 0.01;

                            ArrayList<Polygon> p = getCornerAdjacentPolygons(currentPoints.get(j), polygons.get(i));
                            for (int k = 0; !removePoint && k < p.size(); k++) {
                                if (p.get(k).contains(newX, newY)) {
                                    removePoint = true;
                                }
                            }
                            if (!removePoint && !p.isEmpty()) {
                                currentPoints.set(j, new Point2D(newX, newY));
                            }
                        }

                        if (!removePoint) {
                            reflexVertices.add(new PathVertex(currentPoints.get(j)));
                            singlePointer = new int[]{i, j == 0 ? currentPoints.size() - 1 : j - 1, j, (j + 1) % currentPoints.size()};
                            pointers.add(singlePointer);
                            currentIndeces.add(j);

                            shortestPathGraph.addVertex(reflexVertices.get(reflexVertices.size() - 1));
                            if (!excludedPolygons.isEmpty()) {
                                Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 5, Color.GREEN);
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
                //if (Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == 1 || Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == currentPoints.size() - 1) {
                //System.out.println("Vertex " + currentIndeces.get(j) + " checked.");
                if (Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == 1 || Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == currentPoints.size() - 1) {
                    // add an edge between them
                    PathVertex v1 = reflexVertices.get(prevNrReflexVertices + j);
                    PathVertex v2 = reflexVertices.get(prevNrReflexVertices + (j + 1) % currentIndeces.size());
                    double differenceSquared = Math.pow(v1.getX() - v2.getX(), 2) + Math.pow(v1.getY() - v2.getY(), 2);

                    // check if edge is "covered" by excluded polygon
                    if (!isEdgeAdjacentToPolygon(v1.getX(), v1.getY(), v2.getX(), v2.getY()) && !isEdgeIntersectingPolygon(v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
                        DefaultWeightedEdge edge = shortestPathGraph.addEdge(v1, v2);
                        if (edge != null) {
                            shortestPathGraph.setEdgeWeight(edge, Math.sqrt(differenceSquared));
                            if (!excludedPolygons.isEmpty()) {
                                Line line = new Line(v1.getX(), v1.getY(), v2.getX(), v2.getY());
                                line.setStroke(Color.GREEN);
                                line.setStrokeWidth(2);
                                Main.pane.getChildren().add(line);
                                line.toFront();
                            }
                        }
                    }

                    /*System.out.println("Edge added between " + currentIndeces.get(j) + " and " + currentIndeces.get((j + 1) % currentIndeces.size()));
                    System.out.printf("(%.3f|%.3f) and (%.3f|%.3f)\n", v1.getX(), v2.getX(), v2.getX(), v2.getY());
                    //System.out.println(shortestPathGraph.vertexSet().contains(v1) + " " + shortestPathGraph.vertexSet().contains(v2));
                    System.out.printf("Line from (%.3f|%.3f) to (%.3f|%.3f)\n", line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());*/

                    // TODO: recognise when separating triangle is blocking this connection
                }
            }
        }

        // checking for non-adjacent reflex vertices that should have a bitangent edge between them
        // points are assigned as described in "Planning Algorithms" p.264
        Point2D p1, p2, p3, p4, p5, p6;
        Line excludedIntersectionLine;
        for (int i = 0; i < reflexVertices.size(); i++) {
            singlePointer = pointers.get(i);
            p1 = allPoints.get(singlePointer[0]).get(singlePointer[1]); // previous point (counter clockwise order)
            p2 = allPoints.get(singlePointer[0]).get(singlePointer[2]); // reflex vertex point
            p3 = allPoints.get(singlePointer[0]).get(singlePointer[3]); // next point
            excludedIntersectionLine = new Line(p2.getX(), p2.getY(), 0, 0);
            for (int j = i + 1; j < reflexVertices.size(); j++) {
                if (map.isVisible(reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY())) {
                    singlePointer = pointers.get(j);
                    p4 = allPoints.get(singlePointer[0]).get(singlePointer[1]);
                    p5 = allPoints.get(singlePointer[0]).get(singlePointer[2]);
                    p6 = allPoints.get(singlePointer[0]).get(singlePointer[3]);

                    boolean intersection = false;
                    if (excludedPolygons != null && excludedPolygons.size() != 0) {
                        excludedIntersectionLine.setEndX(p5.getX());
                        excludedIntersectionLine.setEndY(p5.getY());
                        for (int k = 0; !intersection && k < excludedPolygons.size(); k++) {
                            if (GeometryOperations.lineIntersectInPoly(excludedPolygons.get(k), excludedIntersectionLine)) {
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
                            Line line = new Line(reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY());
                            if (!excludedPolygons.isEmpty()) {
                                line.setStroke(Color.GREEN);
                                line.setStrokeWidth(2);
                                Main.pane.getChildren().add(line);
                                line.toFront();
                            }

                            PathVertex v1 = reflexVertices.get(i);
                            PathVertex v2 = reflexVertices.get(j);
                            double differenceSquared = Math.pow(v1.getX() - v2.getX(), 2) + Math.pow(v1.getY() - v2.getY(), 2);
                            shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(v1, v2), Math.sqrt(differenceSquared));
                        }
                    }
                }
            }
        }
    }

    public PlannedPath getShortestPath(double sourceX, double sourceY, double sinkX, double sinkY) {
        return getShortestPath(new Point2D(sourceX, sourceY), new Point2D(sinkX, sinkY));
    }

    public PlannedPath getShortestPath(Point2D source, Point2D sink) {
        // TODO: add visibility check through separating triangles

        PlannedPath plannedPath = new PlannedPath();

        // if there is a straight line between source and sink point just use that as shortest path
        if (map.isVisible(source.getX(), source.getY(), sink.getX(), sink.getY()) && !
                isEdgeIntersectingPolygon(source.getX(), source.getY(), sink.getX(), sink.getY()) &&
                !isEdgeAdjacentToPolygon(source.getX(), source.getY(), sink.getX(), sink.getY())) {
            plannedPath.addLine(new Line(source.getX(), source.getY(), sink.getX(), sink.getY()));
            Line line = new Line(source.getX(), source.getY(), sink.getX(), sink.getY());
            if (excludedPolygons.size() != 0) {
                line.setFill(Color.RED);
            }
            line.setStrokeWidth(2);
            Label l = new Label("sz: " + excludedPolygons.size());
            l.setTranslateX(sink.getX());
            l.setTranslateY(sink.getY());
            //Main.pane.getChildren().addAll(line, l);
            return plannedPath;
        }

        // add new vertices to the graph
        PathVertex sourceVertex = new PathVertex(source);
        PathVertex sinkVertex = new PathVertex(sink);
        shortestPathGraph.addVertex(sourceVertex);
        shortestPathGraph.addVertex(sinkVertex);

        // add new edges between source and sink and the visible nodes of the graph
        // first for the source
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            if (!pv.equals(sourceVertex) && !pv.equals(sinkVertex)) {
                // check whether pv is visible from source (and whether there are separating polygons in between)
                if (map.isVisible(pv.getX(), pv.getY(), sourceVertex.getX(), sourceVertex.getY()) &&
                        !isEdgeIntersectingPolygon(pv.getX(), pv.getY(), sourceVertex.getX(), sourceVertex.getY()) &&
                        !isEdgeAdjacentToPolygon(pv.getX(), pv.getY(), sourceVertex.getX(), sourceVertex.getY())) {
                    double differenceSquared = Math.pow(pv.getX() - sourceVertex.getX(), 2) + Math.pow(pv.getY() - sourceVertex.getY(), 2);
                    shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(sourceVertex, pv), Math.sqrt(differenceSquared));
                }
            }
        }
        // then for the sink
        for (PathVertex pv : shortestPathGraph.vertexSet()) {
            if (!pv.equals(sourceVertex) && !pv.equals(sinkVertex)) {
                // check whether pv is visible from source (and whether there are separating polygons in between)
                if (map.isVisible(pv.getX(), pv.getY(), sinkVertex.getX(), sinkVertex.getY()) &&
                        !isEdgeIntersectingPolygon(pv.getX(), pv.getY(), sinkVertex.getX(), sinkVertex.getY()) &&
                        !isEdgeAdjacentToPolygon(pv.getX(), pv.getY(), sinkVertex.getX(), sinkVertex.getY())) {
                    double differenceSquared = Math.pow(pv.getX() - sinkVertex.getX(), 2) + Math.pow(pv.getY() - sinkVertex.getY(), 2);
                    shortestPathGraph.setEdgeWeight(shortestPathGraph.addEdge(pv, sinkVertex), Math.sqrt(differenceSquared));
                }
            }
        }

        // calculate the shortest path and construct the PlannedPath object which will be returned
        DijkstraShortestPath<PathVertex, DefaultWeightedEdge> shortestPathCalculator = new DijkstraShortestPath<>(shortestPathGraph);
        GraphPath<PathVertex, DefaultWeightedEdge> graphPath = shortestPathCalculator.getPath(sourceVertex, sinkVertex);
        PathVertex pv1, pv2;
        try {
            for (int i = 0; i < graphPath.getVertexList().size() - 1; i++) {
                pv1 = graphPath.getVertexList().get(i);
                pv2 = graphPath.getVertexList().get(i + 1);
                plannedPath.addLine(new Line(pv1.getX(), pv1.getY(), pv2.getX(), pv2.getY()));
                Line line = new Line(pv1.getX(), pv1.getY(), pv2.getX(), pv2.getY());
                if (excludedPolygons.size() != 0) {
                    line.setFill(Color.RED);
                }
                line.setStrokeWidth(2);
                Label l = new Label("sz: " + excludedPolygons.size());
                l.setTranslateX(sink.getX());
                l.setTranslateY(sink.getY());
                //Main.pane.getChildren().addAll(line, l);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            /*System.out.println("graphPath: " + graphPath);
            System.out.printf("Source: (%.3f|%.3f)\n", source.getX(), source.getY());
            System.out.printf("Sink: (%.3f|%.3f)\n", sink.getX(), sink.getY());

            for (PathVertex vertex : shortestPathGraph.vertexSet()) {
                Main.pane.getChildren().add(new Circle(vertex.getX(), vertex.getY(), 5, Color.CYAN));
            }
            Main.pane.getChildren().add(new Circle(source.getX(), source.getY(), 3, Color.ORANGE));
            Main.pane.getChildren().add(new Circle(sink.getX(), sink.getY(), 3, Color.ORANGE));

            for (DefaultWeightedEdge edge : shortestPathGraph.edgeSet()) {
                Main.pane.getChildren().add(new Line(shortestPathGraph.getEdgeSource(edge).getX(), shortestPathGraph.getEdgeSource(edge).getY(), shortestPathGraph.getEdgeTarget(edge).getX(), shortestPathGraph.getEdgeTarget(edge).getY()));
            }*/
        }

        // remove the source and sink vertices
        shortestPathGraph.removeVertex(sourceVertex);
        shortestPathGraph.removeVertex(sinkVertex);
        return plannedPath;
    }

    private ArrayList<Polygon> getCornerAdjacentPolygons(Point2D cornerPoint, Polygon restOfPolygon) {
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
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                if (GeometryOperations.lineIntersectInPoly(p, new Line(x1, y1, x2, y2))) {
                    return true;
                }
            }
        }
        return false;
    }

/*
    *//*
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(source.getX(), source.getY(), reflexPoints.get(i).getX(), reflexPoints.get(i).getY())) {
                double differenceSquared = (reflexPoints.get(i).getX() - source.getX()) * (reflexPoints.get(i).getX() - source.getX()) + (reflexPoints.get(i).getY() - source.getY()) * (reflexPoints.get(i).getY() - source.getY());
                // sightList.addEdge(source, reflexPoints.get(i));
                sightList.setEdgeWeight(sightList.addEdge(sourceVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(sink.getX(), sink.getY(), reflexPoints.get(i).getX(), reflexPoints.get(i).getY())) {
                double differenceSquared = (reflexPoints.get(i).getX() - sink.getX()) * (reflexPoints.get(i).getX() - sink.getX()) + (reflexPoints.get(i).getY() - sink.getY()) * (reflexPoints.get(i).getY() - sink.getY());
                // sightList.addWeightedEdge(sink, reflexPoints.get(i), Math.sqrt(differenceSquared));
                sightList.setEdgeWeight(sightList.addEdge(sinkVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        DijkstraShortestPath test = new DijkstraShortestPath(sightList);
        return test.getPath(sourceVertex, sinkVertex);
     *//*

    public PlannedPath getShortestPathVertices(MapRepresentation map, Point2D source, Point2D sink) {
        reflexInLineOfSight(map, findReflex(map));
        //GraphPath graphPath = calculateShortestPath(map, reflexInLineOfSight(map, findReflex(map)), source, sink);
        PlannedPath plannedPath = new PlannedPath();
       *//* PathVertex pv1, pv2;
        for (int i = 0; i < graphPath.getVertexList().size() - 1; i++) {
            pv1 = (PathVertex) graphPath.getVertexList().get(i);
            pv2 = (PathVertex) graphPath.getVertexList().get(i + 1);
            plannedPath.addLine(new Line(pv1.getX(), pv1.getY(), pv2.getX(), pv2.getY()));
        }*//*
        return plannedPath;
    }

    public GraphPath getShortestPath(MapRepresentation map, Point2D source, Point2D sink) {
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
        ArrayList<Point2D> currentPoints;
        ArrayList<Integer> currentIndeces;
        Point2D prevPoint, nextPoint;
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

                    prevVectorX = currentPoints.get(j).getX() - prevPoint.getX();
                    prevVectorY = currentPoints.get(j).getY() - prevPoint.getY();
                    nextVectorX = currentPoints.get(j).getX() - nextPoint.getX();
                    nextVectorY = currentPoints.get(j).getY() - nextPoint.getY();

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
                    //Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getX() + 5);
                    label.setTranslateY(currentPoints.get(j).getY() + 5);
                    //Main.pane.getChildren().add(circle);
                    //Main.pane.getChildren().add(label);
                }
            } else {
                // iterate over the points in reverse order
                for (int j = currentPoints.size() - 1; j >= 0; j--) {
                    prevPoint = currentPoints.get((j + 1) % currentPoints.size());
                    nextPoint = j == 0 ? currentPoints.get(currentPoints.size() - 1) : currentPoints.get(j - 1);

                    prevVectorX = currentPoints.get(j).getX() - prevPoint.getX();
                    prevVectorY = currentPoints.get(j).getY() - prevPoint.getY();
                    nextVectorX = currentPoints.get(j).getX() - nextPoint.getX();
                    nextVectorY = currentPoints.get(j).getY() - nextPoint.getY();

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
                    //Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getX() + 5);
                    label.setTranslateY(currentPoints.get(j).getY() + 5);
                    //Main.pane.getChildren().add(circle);
                    //Main.pane.getChildren().add(label);
                }
            }
            for (int j = 0; j < currentIndeces.size(); j++) {
                // if the points are adjacent in the polygon, i.e. their indeces are one apart
                if (Math.abs(currentIndeces.get(j) - currentIndeces.get((j + 1) % currentIndeces.size())) == 1) {
                    // add an edge between them
                    Line line = new Line(reflexVertices.get(j).getX(), reflexVertices.get(j).getY(), reflexVertices.get((j + 1) % currentIndeces.size()).getX(), reflexVertices.get((j + 1) % currentIndeces.size()).getY());
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
        //  ArrayList<javafx.geometry.Point2D> polygon= geometryOperations.polyToPoints(polygons);
        Point2D left;
        Point2D right;
        for (int i = 0; i < polygons.size(); i++) {
            ArrayList<Point2D> polygon = geometryOperations.polyToPoints(polygons.get(i));

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
                //Line between= new Line(left.getX(),left.getY(), right.getX(),right.getY());
                double x = (right.getX() - left.getX()) / 2;
                double y = (right.getY() - left.getY()) / 2;

                Point2D mid = new Point2D(right.getX() - x, right.getY() - y);

                Point2D main = polygon.get(j);

                int count = 0;
                Line between = new Line(mid.getX(), mid.getY(), main.getX(), main.getY());
                boolean legal = map.legalPosition(mid.getX(), mid.getY());
                count = allIntersect(polygons, between).size();

                if (count % 2 == 0 && !legal) {
                    reflex.add(reflexIndex, new PathVertex(polygon.get(j)));
                    reflexIndex++;

                } else if (count % 2 == 1 && legal) {
                    reflex.add(reflexIndex, new PathVertex(polygon.get(j)));
                    reflexIndex++;
                }

                //map.legalPosition(mid.getX(),mid.getY());






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
                double leftvector = Math.toDegrees(Math.atan2(left.getX() - polygon.get(j).getX(), left.getY() - polygon.get(j).getY()));
                double rightvector = Math.toDegrees(Math.atan2(right.getX() - polygon.get(j).getX(), right.getY() - polygon.get(j).getY()));

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
                    if (map.isVisible(reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY())) {
                        System.out.printf("Visibility between (%.2f|%.2f) and (%.2f|%.2f)\n", reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY());

                        angle = Math.toDegrees(Math.atan2(reflexVertices.get(i).getY() - reflexVertices.get(j).getY(), reflexVertices.get(i).getX() - reflexVertices.get(j).getX()));
                        PathVertex above, below, inside1, inside2;
                        above = new PathVertex(reflexVertices.get(i).getX() + eps, reflexVertices.get(i).getY() + eps * angle);
                        below = new PathVertex(reflexVertices.get(j).getX() - eps, reflexVertices.get(j).getY() - eps * angle);
                        inside1 = new PathVertex(reflexVertices.get(i).getX() - eps, reflexVertices.get(i).getY() - eps * angle);
                        inside2 = new PathVertex(reflexVertices.get(j).getX() + eps, reflexVertices.get(j).getY() + eps * angle);

                        double xDiff = (reflexVertices.get(i).getX() - reflexVertices.get(j).getX());
                        double yDiff = (reflexVertices.get(i).getY() - reflexVertices.get(j).getY());
                        double length = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
                        xDiff /= length;
                        yDiff /= length;
                        xDiff *= eps;
                        yDiff *= eps;
                        inside1 = new PathVertex(reflexVertices.get(i).getX() + xDiff, reflexVertices.get(i).getY() + yDiff);
                        inside2 = new PathVertex(reflexVertices.get(j).getX() - xDiff, reflexVertices.get(j).getY() - yDiff);
                        //if (map.legalPosition(above.getX(), above.getY()) && map.legalPosition(below.getX(), below.getY()) && map.legalPosition(inside1.getX(), inside1.getY()) && map.legalPosition(inside2.getX(), inside2.getY())) {
                        if (map.legalPosition(inside1.getX(), inside1.getY()) && map.legalPosition(inside2.getX(), inside2.getY())) {
                            PathVertex v1 = reflexVertices.get(i);
                            PathVertex v2 = reflexVertices.get(j);

                            graph1.addVertex(v1);
                            graph1.addVertex(v2);

                            double differenceSquare = (reflexVertices.get(i).getX() - reflexVertices.get(j).getX()) * (reflexVertices.get(i).getX() - reflexVertices.get(j).getX()) + (reflexVertices.get(i).getY() - reflexVertices.get(j).getY()) * (reflexVertices.get(i).getY() - reflexVertices.get(j).getY());
                            // graph1.addWeightedEdge(v1, v2, Math.sqrt(differenceSquare));

                            graph1.setEdgeWeight(graph1.addEdge(v1, v2), Math.sqrt(differenceSquare));

                            Line line = new Line(reflexVertices.get(i).getX(), reflexVertices.get(i).getY(), reflexVertices.get(j).getX(), reflexVertices.get(j).getY());
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

    private GraphPath calculateShortestPath(MapRepresentation map, SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> sightList, Point2D source, Point2D sink) {
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
            if (map.isVisible(source.getX(), source.getY(), reflexPoints.get(i).getX(), reflexPoints.get(i).getY())) {
                double differenceSquared = (reflexPoints.get(i).getX() - source.getX()) * (reflexPoints.get(i).getX() - source.getX()) + (reflexPoints.get(i).getY() - source.getY()) * (reflexPoints.get(i).getY() - source.getY());
                // sightList.addEdge(source, reflexPoints.get(i));
                sightList.setEdgeWeight(sightList.addEdge(sourceVertex, reflexPoints.get(i)), Math.sqrt(differenceSquared));
            }
        }
        for (int i = 0; i < reflexPoints.size(); i++) {
            if (map.isVisible(sink.getX(), sink.getY(), reflexPoints.get(i).getX(), reflexPoints.get(i).getY())) {
                double differenceSquared = (reflexPoints.get(i).getX() - sink.getX()) * (reflexPoints.get(i).getX() - sink.getX()) + (reflexPoints.get(i).getY() - sink.getY()) * (reflexPoints.get(i).getY() - sink.getY());
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