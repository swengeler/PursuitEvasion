package shadowPursuit;

import additionalOperations.GeometryOperations;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.Agent;

import java.util.ArrayList;

import static additionalOperations.GeometryOperations.*;

/**
 * Created by Robins on 29.04.2017.
 */
public class shadowOperations {


    public static ArrayList<Point2D> getX1Points(Polygon environment, ArrayList<Polygon> obstacles, ArrayList<Point2D> agents) {


        //First you add all points


        ArrayList<Point2D> all = polyToPoints(environment);


        ArrayList<Point2D> vis = new ArrayList<>();
        if (obstacles.size() > 0 && obstacles != null) {
            for (Polygon poly : obstacles) {
                if (poly != null) {
                    all.addAll(polyToPoints(poly));
                }
            }
        }


        /* Possible cases for a type1 vertex:
        *   1. environment PathVertex is blocked by environment itself
        *   2. environment PathVertex is blocked by an obstacle
        *   3. Obstacle PathVertex is blocked by enironment
        *   4. Obstacle vertex is blocked by obstacle
         */

        Line temp;
        double agentX, agentY, x2, y2;
        int i;
        boolean visib = true;


        for (Point2D agent : agents) {
            //System.out.println("For Agent = " + agent);
            agentX = agent.getX();
            agentY = agent.getY();


            i = 0;
            while (i < all.size()) {
                //System.out.println("For Point = " + all.get(i));
                x2 = all.get(i).getX();
                y2 = all.get(i).getY();

                //System.out.println("\nFor point: \tX = " + x2 + "\tY = " + y2);
                temp = new Line(agentX, agentY, x2, y2);

                visib = true;
                if (!lineIntersectInPoly(environment, temp)) {
                    for (int j = 0; j < obstacles.size(); j++) {
                        //And if just one obstacle blocks ths view for the controlledAgents this controlledAgents cannnot see the point
                        if (lineIntersectInPoly(obstacles.get(j), temp)) {
                            visib = false;
                            //break;
                        }
                    }
                    if (x2 == 658 && y2 == 257) {
                        Point2D tmp = new Point2D(658, 257);
                        System.out.println("VISIBLE for " + tmp + " ????? -> " + visib);
                    }
                    if (visib == true) {
                        //System.out.println("Removed = " + all.get(i));
                        all.remove(i);
                        i = i - 1;
                    }
                }
                i++;
            }
            System.out.println();

        }

        System.out.println(all);

        return all;
    }


    public static ArrayList<Point2D> getX2Points(Polygon environment, ArrayList<Polygon> obstacles, ArrayList<Point2D> type1Points, ArrayList<Agent> agents) {
        Point2D temp;
        ArrayList<Point2D> type2 = new ArrayList<>();

        //For every type1 identify the corresponding polygon
        for (Point2D point : type1Points) {

            if (inPolygon(point, environment)) {
                getAdjT2(type1Points, type2, environment);
            } else {
                for (Polygon poly : obstacles) {
                    getAdjT2(type1Points, type2, poly);
                }
            }

        }
        if (type2.size() == 0) {
            System.exit(000111000);
        }

        return type2;
    }

    public static boolean inPolygon(Point2D dot, Polygon poly) {
        ArrayList<Point2D> pointList = polyToPoints(poly);
        return inPolygon(dot, pointList);
    }

    public static boolean inPolygon(Point2D dot, ArrayList<Point2D> polyPoints) {
        if (polyPoints.size() == 0) {
            //System.out.println("Size = 0");
            return false;
        }

        for (Point2D point : polyPoints) {
            //System.out.println("point = " + point + "\tDot = " + dot);
            if (point.getX() == dot.getX() && point.getY() == dot.getY()) {
                //System.out.println("returning true");
                return true;
            }
        }
        return false;
    }


    public static void getAdjT2(ArrayList<Point2D> type1Points, ArrayList<Point2D> type2, Polygon poly) {
        ArrayList<Point2D> polyPoints = polyToPoints(poly);
        Point2D point, temp;


        if (type1Points.size() > 1) {
            //For every point in the non-visible set
            for (int i = 0; i < type1Points.size(); i++) {
                point = type1Points.get(i);
                //Check to which polygon the point belongs

                //Check if the adjacent points in the original polygon are also in the non-visible set

                //Current one in polygon
                if (inPolygon(type1Points.get(i), polyPoints)) {

                    //go to position in polygon
                    int j = 0;

                    while (polyPoints.get(j) != point) {
                        j++;
                    }

                    //j is now the position in the polygon where the unkonown point is
                    //check right side


                    if ((j + 1) < polyPoints.size() && !inPolygon(polyPoints.get(j + 1), type1Points)) {
                        temp = polyPoints.get(j + 1);

                        if (!inPolygon(temp, type2)) {
                            type2.add(temp);
                        }
                    } else if (j == polyPoints.size() - 1 && !inPolygon(polyPoints.get(0), type1Points)) {
                        temp = polyPoints.get(0);

                        if (!inPolygon(temp, type2)) {
                            type2.add(temp);
                        }
                    }

                    if (j == 0 && !inPolygon(polyPoints.get(polyPoints.size() - 1), type1Points)) {
                        temp = polyPoints.get(polyPoints.size() - 1);

                        if (!inPolygon(temp, type2)) {
                            type2.add(temp);
                        }
                    } else if (j > 0 && !inPolygon(polyPoints.get(j - 1), type1Points)) {
                        temp = polyPoints.get(j - 1);

                        if (!inPolygon(temp, type2)) {
                            type2.add(temp);
                        }
                    }
                }
            }
        } else if (type1Points.size() == 1) {

        } else {
            System.exit(0);
        }


    }

    //First we need to find all type-2 points visible for a certain agent
    public static ArrayList<Point2D> getOcclusionPoints(Agent agent, ArrayList<Point2D> type2Points, Polygon environment, ArrayList<Polygon> obstacles) {
        ArrayList<Point2D> ocPoints = new ArrayList<>();
        double agentX, agentY, pointX, pointY;

        Line temp;

        agentX = agent.getXPos();
        agentY = agent.getYPos();
        boolean visible = true;


        for (Point2D point : type2Points) {
            pointX = point.getX();
            pointY = point.getY();

            temp = new Line(agentX, agentY, pointX, pointY);
            visible = true;

            if (!lineIntersectInPoly(environment, temp)) {
                for (Polygon obst : obstacles) {
                    if (lineIntersectInPoly(obst, temp)) {
                        visible = false;
                    }
                }
            } else {
                visible = false;
            }

            if (visible == true) {
                ocPoints.add(point);
            }
        }
        return ocPoints;
    }


    public static ArrayList<Point2D> getType3(Polygon environment, ArrayList<Polygon> obstacles, ArrayList<Agent> agents, ArrayList<Point2D> type2) {
        //do  for every tpe2 Point

        //find visble agents

        //create occ ray and connect


        //TODO continue here
        return null;
    }

    public static ArrayList<ArrayList<Point2D>> getConnectedType1(ArrayList<Point2D> type1, Polygon environment, ArrayList<Polygon> obstacles) {
        ArrayList<ArrayList<Point2D>> overviewConnected = new ArrayList<ArrayList<Point2D>>();
        ArrayList<Point2D> connected;

        Point2D temp;
        int j = 0;

        for (int i = 0; i < type1.size() - 1; i++) {
            connected = new ArrayList<>();
            temp = type1.get(i);
            connected.add(temp);
            j = i + 1;
            if (directConnectInPoly(temp, type1.get(j), environment)) {
                while (directConnectInPoly(temp, type1.get(j), environment)) {
                    i = j;
                    j = i + 1;
                    temp = type1.get(i);
                    connected.add(temp);
                }
            } else {
                //Find obstacle in question
                for (Polygon obst : obstacles) {
                    if (directConnectInPoly(temp, type1.get(j), obst)) {
                        while (directConnectInPoly(temp, type1.get(j), obst)) {
                            i = j;
                            j = i + 1;
                            temp = type1.get(i);
                            connected.add(temp);
                        }
                        break;
                    }
                }
                overviewConnected.add(connected);
            }

        }
        return overviewConnected;

    }

    public static ArrayList<ShadowNode> orderByClosestToPoint(ArrayList<ShadowNode> shadNodes, Point2D start) {
        ArrayList<ShadowNode> returnList = new ArrayList<>();
        double minDist;
        ShadowNode tmp;
        int pos;
        while (shadNodes.size() != 0) {
            minDist = Double.MAX_VALUE;
            pos = 0;
            for (int i = 0; i < shadNodes.size(); i++) {
                tmp = shadNodes.get(i);
                if (distance(tmp.getPosition(), start) < minDist) {
                    minDist = distance(tmp.getPosition(), start);
                    pos = i;
                }
            }
            returnList.add(shadNodes.remove(pos));
        }
        return returnList;
    }


    public static ArrayList<Point2D> getAdjacentPoints(Point2D point, ArrayList<Polygon> allPolys) {


        ArrayList<Point2D> tempPoints, retPoints;
        retPoints = new ArrayList<>();

        for (Polygon poly : allPolys) {

            tempPoints = polyToPoints(poly);
            //System.out.println(poly);
            if (inPolygon(point, poly)) {

                //System.out.println(tempPoints);

                for (int i = 0; i < tempPoints.size(); i++) {
                    if (tempPoints.get(i).getX() == point.getX() && tempPoints.get(i).getY() == point.getY()) {
                        //System.out.println("ENTEREEED");
                        if (i == 0) {
                            //System.out.println("i = 0 entered");
                            retPoints.add(tempPoints.get(tempPoints.size() - 1));
                            retPoints.add(tempPoints.get(1));
                            break;
                        } else if (i == tempPoints.size() - 1) {
                            //System.out.println("i = size-1 entered");
                            retPoints.add(tempPoints.get(i - 1));
                            retPoints.add(tempPoints.get(0));
                            break;
                        } else {
                            //System.out.println("i = " + i + "Size = " + tempPoints.size());
                            retPoints.add(tempPoints.get(i - 1));
                            retPoints.add(tempPoints.get(i + 1));
                            break;
                        }
                    } else {
                        //System.out.println("ELSE Entered!!!!");
                    }
                }
            }
        }
        return retPoints;
    }

    public static Polygon getSharedPolygon(Point2D point1, Point2D point2, ArrayList<Polygon> allPoly) {
        for (Polygon poly : allPoly) {
            if (inPolygon(point1, poly) && inPolygon(point2, poly)) {
                return poly;
            }
        }
        return null;
    }

    public static boolean inSamePolygon(Point2D point1, Point2D point2, ArrayList<Polygon> allPoly) {
        for (Polygon poly : allPoly) {
            if (inPolygon(point1, poly) && inPolygon(point2, poly)) {
                return true;
            }
        }
        return false;
    }

    public static boolean directConnectInPoly(Point2D point1, Point2D point2, Polygon poly) {
        return directConnectInPoly(point1, point2, polyToPoints(poly));

    }

    public static boolean directConnectInPoly(Point2D point1, Point2D point2, ArrayList<Point2D> polyPoints) {
        int j = 0;

        //get To position in polygon where point1 is
        while (point1 != polyPoints.get(j)) {
            j++;
        }


        if (j == 0) {
            //point1 is at j == 0
            if (polyPoints.get(polyPoints.size() - 1) == point2 || polyPoints.get(j + 1) == point2) {
                return true;
            } else {
                return false;
            }
        } else if (j == polyPoints.size() - 1) {
            if (polyPoints.get(0) == point2 || polyPoints.get(j - 1) == point2) {
                return true;
            } else {
                return false;
            }
        } else {
            if (polyPoints.get(j - 1) == point2 || polyPoints.get(j + 1) == point2) {
                return true;
            } else {
                return false;
            }
        }

    }

    //@TODO @Rob after testing, use Simons method so ppl don´t perceive you as a total ripoff ;)
    public static boolean isVisible(double x1, double y1, double x2, double y2, ArrayList<Line> polygonEdges) {
        // check whether the second controlledAgents is visible from the position of the first controlledAgents
        // (given its field of view and the structure of the map)

        for (Line l : polygonEdges) {
            if (GeometryOperations.lineIntersect(l, x1, y1, x2, y2)) {

                System.out.println("Intersect between " + l + " and " + new Line(x1, y1, x2, y2));
                return false;
            }
        }
        return true;
    }


    public static ArrayList<Point2D> findReflex(Polygon env, ArrayList<Polygon> allPoly, ArrayList<Polygon> obstacles) {
        ArrayList<Polygon> polygons = allPoly;
        GeometryOperations geometryOperations = new GeometryOperations();
        ArrayList<Point2D> reflex = new ArrayList<>();
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
                    // System.out.println("this happens 8 times");
                } else if (j == 0) {
                    // System.out.println("this should happen once");
                    left = polygon.get(polygon.size() - 1);
                    right = polygon.get(j + 1);
                } else if (j == polygon.size() - 1) {
                    //System.out.println("this happens once");
                    left = polygon.get(j - 1);
                    right = polygon.get(0);

                } else {
                    System.out.println("you're a fucking retard");
                    left = polygon.get(j - 1);
                    right = polygon.get(0);
                }

                //Line between= new Line(left.getX(),left.getY(), right.getX(),right.getY());

                double x = (right.getX() - left.getX()) / 2;
                double y = (right.getY() - left.getY()) / 2;


                Point2D mid = new Point2D(right.getX() - x, right.getY() - y);

                Point2D main = polygon.get(j);
                /*
                System.out.println("right x = " + right.getX() + " left x = " + left.getX() + " x = " + (right.getX()-x));
                System.out.println("right y = " + right.getY() + " left y = " + left.getY() + " y = " + (right.getY()-y));
                System.out.println("x coord = " + mid.getX() +" y coord = " + mid.getY() );
                */

                int count = 0;
                Line between = new Line(mid.getX(), mid.getY(), main.getX(), main.getY());
                boolean legal = legalPosition(env, obstacles, mid.getX(), mid.getY());
                //System.out.println("is legal? " + legal);
                count = allIntersect(polygons, between).size();
                //System.out.print("intersections= " + count);

                if (count % 2 == 0 && !legal) {
                    reflex.add(reflexIndex, polygon.get(j));
                    //System.out.print("even and illegal");

                    reflexIndex++;

                } else if (count % 2 == 1 && legal) {
                    reflex.add(reflexIndex, polygon.get(j));
                    //System.out.print("odd and legal");


                    reflexIndex++;
                }


            }


        }

        return reflex;
    }

    public static boolean legalPosition(Polygon env, ArrayList<Polygon> obstacles, double xPos, double yPos) {
        if (!env.contains(xPos, yPos)) {
            return false;
        }
        for (Polygon p : obstacles) {
            if (p.contains(xPos, yPos)) {
                return false;
            }
        }
        return true;
    }


}
