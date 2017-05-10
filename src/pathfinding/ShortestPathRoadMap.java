package pathfinding;

import additionalOperations.GeometryOperations;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import simulation.MapRepresentation;
import sun.nio.cs.ext.MacArabic;

import java.util.ArrayList;

import static additionalOperations.GeometryOperations.lineIntersectInPoly;
import static shadowPursuit.shadowOperations.inPolygon;


public class ShortestPathRoadMap {

    private ArrayList<PathVertex> graph;

    public void ShortestPathCalc(MapRepresentation map, Point2D source, Point2D sink) {


        calculateShortestPath(map, reflexInlineofsight(map, findReflex(map)), source, sink);

    }

    public ShortestPathRoadMap(MapRepresentation map) {
        init(map);
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

    public GraphPath generategraphMap(MapRepresentation map, PathVertex source, PathVertex sink) {
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


    public ArrayList<PathVertex> findReflex(MapRepresentation map) {
        ArrayList<Polygon> polygons = map.getAllPolygons();
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
                double x=(right.getX()-left.getX())/2;
                double y= (right.getY()-left.getY())/2;

                Point2D mid= new Point2D(x,y);

                Point2D main= polygon.get(j);

                Line between= new Line(mid.getX(),mid.getY(),main.getX(),main.getY());
                map.legalPosition(mid.getX(),mid.getY());




                int count = 0;


                /*
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

                /*
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
*/
            }


        }

        return reflex;
    }


    public SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> reflexInlineofsight(MapRepresentation map, ArrayList<PathVertex> reflexs) {
        double eps = 0.0001;
        double angle;
        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> graph1 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < reflexs.size(); i++) {
            for (int j = 9; j >= i; j--) {
                if (i != j) {
                    if (map.isVisible(reflexs.get(i).getX(), reflexs.get(i).getY(), reflexs.get(j).getX(), reflexs.get(j).getY())) {
                        angle = Math.toDegrees(Math.atan2(reflexs.get(i).getY() - reflexs.get(j).getY(), reflexs.get(i).getX() - reflexs.get(j).getX()));
                        PathVertex above, below, inside1, inside2;
                        above = new PathVertex(reflexs.get(i).getX() + eps, reflexs.get(i).getY() + eps * angle);
                        below = new PathVertex(reflexs.get(j).getX() - eps, reflexs.get(j).getY() - eps * angle);
                        inside1 = new PathVertex(reflexs.get(i).getX() - eps, reflexs.get(i).getY() - eps * angle);
                        inside2 = new PathVertex(reflexs.get(j).getX() + eps, reflexs.get(j).getY() + eps * angle);
                        if (map.legalPosition(above.getX(), above.getY()) && map.legalPosition(below.getX(), below.getY()) && map.legalPosition(inside1.getX(), inside1.getY()) && map.legalPosition(inside2.getX(), inside2.getY()))
                            ;

                        PathVertex v1 = reflexs.get(i);
                        PathVertex v2 = reflexs.get(j);


                        graph1.addVertex(v1);
                        graph1.addVertex(v2);

                        double differencesquare = (reflexs.get(i).getX() - reflexs.get(j).getX()) * (reflexs.get(i).getX() - reflexs.get(j).getX()) + (reflexs.get(i).getY() - reflexs.get(j).getY()) * (reflexs.get(i).getY() - reflexs.get(j).getY());
                        // graph1.addWeightedEdge(v1, v2, Math.sqrt(differencesquare));

                        graph1.setEdgeWeight(graph1.addEdge(v1, v2), Math.sqrt(differencesquare));
                    }
                }
            }
        }
        return graph1;
    }


    public GraphPath calculateShortestPath(MapRepresentation map, SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> sightList, Point2D source, Point2D sink) {


        ArrayList<Polygon> polygons = map.getAllPolygons();

        sightList.addVertex(new PathVertex(sink));
        sightList.addVertex(new PathVertex(source));
        ArrayList<PathVertex> reflexpoints = findReflex(map);

        for (int i = 0; i < reflexpoints.size(); i++) {
            if (map.isVisible(source.getX(), source.getY(), reflexpoints.get(i).getX(), reflexpoints.get(i).getY())) {

                double differencesquare = (reflexpoints.get(i).getX() - source.getX()) * (reflexpoints.get(i).getX() - source.getX()) + (reflexpoints.get(i).getY() - source.getY()) * (reflexpoints.get(i).getY() - source.getY());

                // sightList.addEdge(source, reflexpoints.get(i));
                sightList.setEdgeWeight(sightList.addEdge(new PathVertex(source), reflexpoints.get(i)), Math.sqrt(differencesquare));

            }
        }
        for (int i = 0; i < reflexpoints.size(); i++) {
            if (map.isVisible(sink.getX(), sink.getY(), reflexpoints.get(i).getX(), reflexpoints.get(i).getY())) {

                double differencesquare = (reflexpoints.get(i).getX() - sink.getX()) * (reflexpoints.get(i).getX() - sink.getX()) + (reflexpoints.get(i).getY() - sink.getY()) * (reflexpoints.get(i).getY() - sink.getY());

                // sightList.addWeightedEdge(sink, reflexpoints.get(i), Math.sqrt(differencesquare));
                sightList.setEdgeWeight(sightList.addEdge(new PathVertex(sink), reflexpoints.get(i)), Math.sqrt(differencesquare));

            }
        }
        DijkstraShortestPath test = new DijkstraShortestPath(sightList);
        return test.getPath(source, sink);
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