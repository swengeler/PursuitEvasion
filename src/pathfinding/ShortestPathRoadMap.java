package pathfinding;

import additionalOperations.GeometryOperations;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import simulation.MapRepresentation;
import simulation.PlannedPath;
import ui.AltMain;

import java.util.ArrayList;


public class ShortestPathRoadMap {

    private ArrayList<PathVertex> graph;

    public ShortestPathRoadMap(MapRepresentation map) {
        init(map);
        ArrayList<PathVertex> reflexVertices = findReflex(map);
        for (PathVertex pv : reflexVertices) {
            Circle circle = new Circle(pv.getX(), pv.getY(), 5, Color.CYAN);
            AltMain.pane.getChildren().add(circle);
        }
    }

    private void init(MapRepresentation map) {
        graph = new ArrayList<>();

        // determine reflex vertices
        double currentAngle, a1, a2, b1, b2;
        boolean inPolygon;
        Polygon currentPolygon = map.getBorderPolygon();
        // outside polygon
        for (int i = 0; i < currentPolygon.getPoints().size(); i += 2) {
            a1 = currentPolygon.getPoints().get(i) - currentPolygon.getPoints().get((i == 0 ? currentPolygon.getPoints().size() : i) - 2); // x
            a2 = currentPolygon.getPoints().get(i + 1) - currentPolygon.getPoints().get((i == 0 ? currentPolygon.getPoints().size() : i) - 1); // y
            //b1 = currentPolygon.getPoints().get(i) - currentPolygon.getPoints().get((i + 2) % currentPolygon.getPoints().size()); // x
            //b2 = currentPolygon.getPoints().get(i + 1) - currentPolygon.getPoints().get((i + 3) % currentPolygon.getPoints().size()); // y
            //currentAngle = Math.acos((a1 * b1 + a2 * b2) / (Math.sqrt(Math.pow(a1, 2) + Math.pow(a2, 2)) * Math.sqrt(Math.pow(b1, 2) + Math.pow(b2, 2))));
            inPolygon = currentPolygon.contains(currentPolygon.getPoints().get(i) + 0.0001 * a1, currentPolygon.getPoints().get(i + 1) + 0.0001 * a2);
            if (inPolygon) {
                graph.add(new PathVertex(currentPolygon.getPoints().get(i) + 0.0001 * a1, currentPolygon.getPoints().get(i + 1) + +0.0001 * a2));
            }
        }
        System.out.println("---------");
        // obstacles
        for (Polygon p : map.getObstaclePolygons()) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                a1 = p.getPoints().get(i) - p.getPoints().get((i == 0 ? p.getPoints().size() : i) - 2); // x
                a2 = p.getPoints().get(i + 1) - p.getPoints().get((i == 0 ? p.getPoints().size() : i) - 1); // y
                //b1 = p.getPoints().get(i) - p.getPoints().get((i + 2) % p.getPoints().size()); // x
                //b2 = p.getPoints().get(i + 1) - p.getPoints().get((i + 3) % p.getPoints().size()); // y
                //currentAngle = Math.acos((a1 * b1 + a2 * b2) / (Math.sqrt(Math.pow(a1, 2) + Math.pow(a2, 2)) * Math.sqrt(Math.pow(b1, 2) + Math.pow(b2, 2))));
                inPolygon = p.contains(p.getPoints().get(i) + 0.0001 * a1, p.getPoints().get(i + 1) + +0.0001 * a2);
                if (!inPolygon) {
                    graph.add(new PathVertex(p.getPoints().get(i) + 0.0001 * a1, p.getPoints().get(i + 1) + 0.0001 * a2));
                    for (int j = 0; j < p.getPoints().size(); j += 2) {
                        if (i != j) {

                        }
                    }
                }
            }
            System.out.println("Polygon");
            // consecutive reflex vertices
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                System.out.println(p.getPoints().get(i) + " - " + p.getPoints().get(i + 1));
            }
        }

        /*
        for (int i = 0; i < graph.size(); i += 1){
            double e1 = graph.get(i);
            double e2 = graph.get(i+1);
            new Edge(e1, e2);


        }*/

    }

    public PlannedPath getShortestPathVertices(MapRepresentation map, Point2D source, Point2D sink) {
        GraphPath graphPath = calculateShortestPath(map, reflexInLineOfSight(map, findReflex(map)), source, sink);
        for (int i = 0; i < graphPath.getVertexList().size(); i++) {
            System.out.println(((PathVertex) graphPath.getVertexList().get(i)).getX() + " | " + ((PathVertex) graphPath.getVertexList().get(i)).getY());
            Circle circle = new Circle(((PathVertex) graphPath.getVertexList().get(i)).getX(), ((PathVertex) graphPath.getVertexList().get(i)).getY(), 5, Color.CYAN);
            //AltMain.pane.getChildren().add(circle);
        }
        return null;
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
        Point2D prevPoint, nextPoint;
        double prevVectorX, prevVectorY, nextVectorX, nextVectorY;
        for (int i = 0; i < polygons.size(); i++) {
            boolean isClockwise = GeometryOperations.isClockwise(polygons.get(i));
            currentPoints = GeometryOperations.polyToPoints(polygons.get(i));
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
                    }
                    //Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getX() + 5);
                    label.setTranslateY(currentPoints.get(j).getY() + 5);
                    //AltMain.pane.getChildren().add(circle);
                    //AltMain.pane.getChildren().add(label);
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
                    }
                    //Circle circle = new Circle(currentPoints.get(j).getX(), currentPoints.get(j).getY(), 10, Color.RED);
                    Label label = new Label("" + check);
                    label.setTranslateX(currentPoints.get(j).getX() + 5);
                    label.setTranslateY(currentPoints.get(j).getY() + 5);
                    //AltMain.pane.getChildren().add(circle);
                    //AltMain.pane.getChildren().add(label);
                }
            }
        }

        /*ArrayList<Polygon> polygons = map.getAllPolygons();
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






                *//*
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

                *//*
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
*//*
            }


        }

        return reflex;*/
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
                            AltMain.pane.getChildren().add(line);
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
    }

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