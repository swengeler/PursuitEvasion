package shadowPursuit;


import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.Agent;

import java.awt.geom.Line2D;
import java.util.ArrayList;


/**
 * Created by Robins on 29.04.2017.
 */
public class shadowOperations {

    public static ArrayList<Point2D> polyToPoints(ArrayList<Polygon> allPoly) {

        //Turn polygon into points
        double xPos, yPos;


        ArrayList<Point2D> allPoints = new ArrayList<>();

        for (Polygon poly : allPoly) {
            allPoints.addAll(polyToPoints(poly));
        }

        return allPoints;
    }

    public static ArrayList<Point2D> polyToPoints(Polygon poly) {
        //Turn polygon into points
        double xPos, yPos;
        ObservableList<Double> vertices = poly.getPoints();
        ArrayList<Point2D> points = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i += 2) {
            xPos = vertices.get(i);
            yPos = vertices.get(i + 1);

            points.add(new Point2D(xPos, yPos));
        }
        return points;
    }


    public static ArrayList<Point2D> getX1Points(Polygon environment, ArrayList<Polygon> obstacles, ArrayList<Point2D> agents) {


        //First you add all points


        ArrayList<Point2D> all = polyToPoints(environment);

        //System.out.println(all);
        //System.out.println(agents);


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

        //System.out.println(all);

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
            if (l != getLineOn(new Point2D(x1, y1), polygonEdges) && l != getLineOn(new Point2D(x2, y2), polygonEdges)) {
                if (lineIntersect(l, x1, y1, x2, y2)) {
                    //System.out.println("\n\nIntersect between " + l + "\nand " + new Line(x1,y1,x2,y2)+ "\n");
                    return false;
                }
            }
        }
        return true;
    }

    public static Line getLineOn(ShadowNode node, ArrayList<Line> polygonEdges) {
        return getLineOn(node.getPosition(), polygonEdges);
    }

    public static Line getLineOn(Point2D point, ArrayList<Line> polygonEdges) {
        for (Line line : polygonEdges) {
            if (onLine(point, line)) {
                return line;
            }
        }
        return null;
    }


    public static ArrayList<Point2D> findReflex(Polygon env, ArrayList<Polygon> allPoly, ArrayList<Polygon> obstacles) {
        ArrayList<Polygon> polygons = allPoly;

        ArrayList<Point2D> reflex = new ArrayList<>();
        int reflexIndex = 0;
        //  ArrayList<javafx.geometry.Point2D> polygon= geometryOperations.polyToPoints(polygons);
        Point2D left;
        Point2D right;
        for (int i = 0; i < polygons.size(); i++) {
            ArrayList<Point2D> polygon = polyToPoints(polygons.get(i));


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



    /*
    *Shadowpursuit: will be used to categorize points of type 1
    * Type 1: blocked entierly from  view because of an obstacles
     */

    public static ArrayList<Point2D> allIntersect(ArrayList<Polygon> allPoly, Line current) {
        ArrayList<Point2D> intersects = new ArrayList<>();
        ArrayList<Point2D> points;
        Line temp;
        Point2D t1, t2;


        for (Polygon poly : allPoly) {
            points = polyToPoints(poly);
            if (lineIntersectInPoly(points, current)) {
                for (int i = 0; i < points.size() - 1; i++) {
                    t1 = points.get(i);
                    t2 = points.get(i + 1);
                    temp = new Line(t1.getX(), t1.getY(), t2.getX(), t2.getY());
                    if (lineIntersect(temp, current)) {
                        intersects.add(FindIntersection(temp, current));
                    }
                }
                t1 = points.get(points.size() - 1);
                t2 = points.get(0);
                temp = new Line(t1.getX(), t1.getY(), t2.getX(), t2.getY());
                if (lineIntersect(temp, current)) {
                    intersects.add(FindIntersection(temp, current));
                }
            }

        }

        return intersects;
    }

    public static boolean polysIntersect(ArrayList<Polygon> allPoly, Line current) {
        boolean intersect = false;
        for (Polygon poly : allPoly) {
            if (lineIntersectInPoly(poly, current)) {
                return true;
            }
        }
        return intersect;

    }


    public static boolean lineIntersectInPoly(Polygon poly, Line current) {
        return lineIntersectInPoly(polyToPoints(poly), current);
    }

    public static boolean lineIntersectInPoly(ArrayList<Point2D> points, Line current) {
        // current = Line between Pursuer and a Point

        double sx, sy, ex, ey;

        sx = current.getStartX();
        sy = current.getStartY();

        ex = current.getEndX();
        ey = current.getEndY();

        Line temp;
        double x1, x2;
        double y1, y2;

        x1 = points.get(0).getX();
        y1 = points.get(0).getY();


        for (int i = 1; i < points.size(); i++) {

            x2 = points.get(i).getX();
            y2 = points.get(i).getY();

            temp = new Line(x1, y1, x2, y2);

            if (lineIntersect(current, temp)) {
                //Intersection if start or endpoint of line are the same as one of the points analyzed?
                //if((x1 != sx) &&(y1 != ))
                return true;
            }

            x1 = x2;
            y1 = y2;
        }

        x2 = points.get(0).getX();
        y2 = points.get(0).getY();

        temp = new Line(x1, y1, x2, y2);

        if (lineIntersect(current, temp)) {
            return true;
        }


        return false;
    }

    public static boolean lineIntersect(Line l1, Line l2) {
        //System.out.println("Comparing = " + l1 + "\nAND = " + l2 + "\n");
        return lineIntersect(l1, l2.getStartX(), l2.getStartY(), l2.getEndX(), l2.getEndY());
    }

    public static boolean lineIntersect(Line line, double startX, double startY, double endX, double endY) {
        double a1, a2, a3, a4;
        a1 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), startX, startY);
        a2 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), endX, endY);
        if (a1 * a2 < 0) {
            a3 = signed2DTriArea(endX, endY, startX, startY, line.getStartX(), line.getStartY());
            a4 = a3 + a2 - a1;
            if (a3 * a4 < 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean lineIntersect2(Line l1, Line l2) {
        //L1 is environment Line2 is ray
        //System.out.println("Comparing = " + l1 + "\nAND = " + l2 + "\n");
        Point2D start, end;
        start = new Point2D(l1.getStartX(), l1.getStartY());
        end = new Point2D(l1.getEndX(), l1.getEndY());
        if (onLine(start, l2) || onLine(end, l2)) {
            return false;
        } else {
            return lineIntersect(l1, l2.getStartX(), l2.getStartY(), l2.getEndX(), l2.getEndY());
        }
    }


    public static boolean inPolygon(Polygon polygon, double startX, double startY, double endX, double endY) {
        Line temp;
        for (int i = 0; i < polygon.getPoints().size(); i += 2) {
            temp = new Line(polygon.getPoints().get(i), polygon.getPoints().get(i + 1), polygon.getPoints().get((i + 2) % polygon.getPoints().size()), polygon.getPoints().get((i + 3) % polygon.getPoints().size()));
            if (temp.contains((startX + (endX - startX) / 2), (startY + (endY - startY) / 2))) {
                return true;
            }
        }
        return polygon.contains((startX + (endX - startX) / 2), (startY + (endY - startY) / 2));
    }


    public static Line scaleRay(Point2D agentPos, ShadowNode t2Point, double value) {
        return scaleRay(agentPos, t2Point.getPosition(), value);
    }

    public static Line scaleRay(Point2D agentPos, Point2D t2Point, double passed) {
        double value = passed;
        //note this method extends the ray by value x if you insert a value >1 if the value is less than one it will scale it by that %
        //System.out.println("Passed value = " + value);

        double endX, endY, aX, aY;

        endX = t2Point.getX();
        endY = t2Point.getY();

        aX = agentPos.getX();
        aY = agentPos.getY();
        double newX, newY;

        //y=mx+c
        //  double m = (endY-aY)/(endX-aX);
        double m = (aY - endY) / (aX - endX);
        /*System.out.println("ay= " +aY);
        System.out.println("ax= " +aX);
        System.out.println("ENDy= " +endY);
        System.out.println("endx= " +endX);
*/
        if (value > 1) {

            if (aX - endX != 0) {
                if (endX > aX) {
                    //System.out.print("wooh its working = " + m);
                    newX = endX + value;
                    newY = endY + value * m;
                } else {
                    //System.out.print("why the fuck man " + m);
                    newX = endX - value;
                    newY = endY - value * m;
                }
            } else {
                if (endY > aY) {
                    //System.out.print("wooh its working = " + m);
                    newX = endX;
                    newY = endY + value;
                } else {
                    //System.out.print("why the fuck man");
                    newX = endX;
                    newY = endY - value;
                }
            }

        } else {
            if (aX - endX != 0) {
                //if (endX > aX) {
                //System.out.print("wooh its working = " + m);
                newX = aX + value * (endX - aX);
                newY = aY + value * (endX - aX) * m;
                /*} else {
                    System.out.print("why the fuck man " + m);
                    newX = aX + value * (endX - aX);
                    newY = aY + value * (endX - aX) * m;
                }
            } */
            } else {
                // if (endY > aY) {
                //System.out.print("wooh its working = " + m);
                newX = aX;
                newY = aY + value * (endX - aX);
             /*   } else {
                    System.out.print("why the fuck man");

                    newX = endX;
                    newY = aY + value * (endX - aX);
                }*/
            }

        }
        /*
        if (endX - aX != 0) {
            if (aX > endX) {
                //System.out.print("wooh its working = " + m);
                newX = aX + value;
                newY = aY + value * m;
            } else {
                //System.out.print("why the fuck man");
                newX = aX - value;
                newY = aY - value * m;
            }
        } else {
            if (aY > endY) {
                //System.out.print("wooh its working = " + m);
                newX = aY+ value/m;
                newY = aY + value;
            } else {
                //System.out.print("why the fuck man");
                newX = aX- value/m;
                newY = aY - value;
            }
        }
*/
        Line ray;
        if (value > 1) {

            ray = new Line(endX, endY, newX, newY);
        } else {

            ray = new Line(aX, aY, newX, newY);
        }
        //System.out.println("Created Ray = " + ray);

        //In case Lines with a negative end are the Problem when we do not find an Intersection where there should be one
        /*
        if(newY < 0 || newX < 0)    {
            Point2D intPoint;
            Line xLine = new Line(0,0,value, 0);
            Line yLine = new Line(0,0, 0, value);
            if(lineIntersect(xLine, ray))   {
                System.out.println("Enter1");
                intPoint = FindIntersection(xLine, ray);
                ray.setEndX(intPoint.getEstX());
                ray.setEndY(intPoint.getEstY());
            }
            else if (lineIntersect(yLine, ray))  {
                System.out.println("Enter2");
                intPoint = FindIntersection(yLine, ray);
                System.out.println(intPoint);
                ray.setEndX(intPoint.getEstX());
                ray.setEndY(intPoint.getEstY());
            }
        }
        */

        return ray;

    }

    private static double signed2DTriArea(double ax, double ay, double bx, double by, double cx, double cy) {
        return (ax - cx) * (by - cy) - (ay - cy) * (bx - cx);
    }

    public static double GetLineYIntesept(Point2D p, double slope) {

        return p.getY() - slope * p.getX();
    }

    public static Point2D FindIntersection(Line line1, Line line2) {

        //System.out.println("Line1 = " + line1);
        //System.out.println("Line2 = " + line2);

        double l1x = (line1.getEndX() - line1.getStartX());
        double l2x = (line2.getEndX() - line2.getStartX());
        double slope2, slope1;
        if (l1x == 0) {
            slope1 = Double.MAX_VALUE;
            System.out.println("first fuck up");
            double xvalue = line1.getEndX();


        } else {
            slope1 = (line1.getEndY() - line1.getStartY()) / l1x;
        }


        if (l2x == 0) {
            slope2 = Double.MAX_VALUE;
            System.out.println("second fuck up");
        } else {
            slope2 = (line2.getEndY() - line2.getStartY()) / l2x;
        }


        Point2D line1Start, line2Start;
        line1Start = new Point2D(line1.getStartX(), line1.getStartY());
        line2Start = new Point2D(line2.getStartX(), line2.getStartY());


        double yinter1 = GetLineYIntesept(line1Start, slope1);
        double yinter2 = GetLineYIntesept(line2Start, slope2);

        if (slope1 == slope2 && yinter1 != yinter2) {
            //System.out.println("Slope clause");
            return null;
        }


        //System.out.println("Slope1 = "+ slope1 + "\tSlope2 = " + slope2);
        double x = (yinter2 - yinter1) / (slope1 - slope2);

        double y = slope1 * x + yinter1;
        //System.out.println("X = " + x);
        //System.out.println("Y = " + y);
        return new Point2D(x, y);
    }

    public static ArrayList removeFrom(ShadowNode check, ArrayList<ShadowNode> list) {
        double x1, x2, y1, y2;
        Point2D pointCheck, tmpPoint, printP1, printP2;
        ShadowNode tmp;

        pointCheck = check.getPosition();
        x1 = Math.round(pointCheck.getX());
        y1 = Math.round(pointCheck.getY());
        printP1 = new Point2D(x1, y1);

        if (list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
                tmp = list.get(i);
                tmpPoint = tmp.getPosition();
                x2 = Math.round(tmpPoint.getX());
                y2 = Math.round(tmpPoint.getY());
                printP2 = new Point2D(x2, y2);


                if (x1 == x2 && y1 == y2) {
                    System.out.println("For => " + printP1 + "\tChecking => " + printP2);
                    list.remove(i);
                    i--;
                }
            }
        }
        return list;
    }

    public static boolean arraycontains(ShadowNode check, ArrayList<ShadowNode> list) {
        double x1, x2, y1, y2;
        Point2D pointCheck, tmpPoint, printP1, printP2;
        ShadowNode tmp;
        boolean yesno = false;

        pointCheck = check.getPosition();
        x1 = Math.round(pointCheck.getX());
        y1 = Math.round(pointCheck.getY());
        printP1 = new Point2D(x1, y1);

        if (list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
                tmp = list.get(i);
                tmpPoint = tmp.getPosition();
                x2 = Math.round(tmpPoint.getX());
                y2 = Math.round(tmpPoint.getY());
                printP2 = new Point2D(x2, y2);


                if (x1 == x2 && y1 == y2) {
                    System.out.println("For => " + printP1 + "\tChecking => " + printP2);
                    yesno = true;
                    break;
                }
            }
        }
        return yesno;
    }

    public static double gradient(Line line) {

        double x1, x2, y1, y2;

        x1 = line.getStartX();
        y1 = line.getStartY();

        x2 = line.getEndX();
        y2 = line.getEndY();

        return (y2 - y1) / (x2 - x1);
    }

    public static boolean onLine(double pointX, double pointY, Line line) {
        Line2D.Double geomLine = new Line2D.Double(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
        return geomLine.ptLineDist(pointX, pointY) == 0.0;
    }

    public static boolean onLine(Point2D point, Line between) {


        double pX, pY, sX, sY, eX, eY;
        //System.out.print(point);
        pX = point.getX();
        pY = point.getY();

        sX = between.getStartX();
        sY = between.getStartY();


        eX = between.getEndX();
        eY = between.getEndY();

        //System.out.println("Overall Distance = " + distance(sX, sY, eX, eY));
        //System.out.println("Between Dist = " + ((distance(pX, pY, sX, sY) + distance(pX, pY, eX, eY))));

        if (Math.round((distance(pX, pY, sX, sY) + distance(pX, pY, eX, eY))) == Math.round(distance(sX, sY, eX, eY))) {
            return true;
        } else {
            return false;
        }

    }

    public static double distance(Point2D p1, Point2D p2) {
        double p1X, p2X, p1Y, p2Y;

        p1X = p1.getX();
        p1Y = p1.getY();

        p2X = p2.getX();
        p2Y = p2.getY();


        return distance(p1X, p1Y, p2X, p2Y);

    }

    public static double distance(double p1X, double p1Y, double p2X, double p2Y) {
        return Math.sqrt(Math.pow(p1X - p2X, 2) + Math.pow(p1Y - p2Y, 2));
    }


    public static Point2D getLineMiddle(Line line) {
        Point2D start, end;
        start = new Point2D(line.getStartX(), line.getStartY());
        end = new Point2D(line.getEndX(), line.getEndY());
        line = scaleRay(start, end, 0.5);
        end = new Point2D(line.getEndX(), line.getEndY());
        return end;
    }

    public static Point2D findIntersect2(Line line1, Line line2) {

        Point2D location;
        location = null;

        double x1, x2, x3, x4, x0;
        double y1, y2, y3, y4, y0;

        double a1, b1, a2, b2;
        // a1= 0;
        //   b1=0;
        //  a2=0;
        //  b2=0;

        x1 = line1.getStartX();
        x2 = line1.getEndX();

        y1 = line1.getStartY();
        y2 = line1.getEndY();


        x3 = line2.getStartX();
        x4 = line2.getEndX();

        y3 = line2.getStartY();
        y4 = line2.getEndY();

        if (x1 == x2 && x3 == x4) {
            //both lines are vertical
            if (x1 != x3) {
                //
                // +System.exit(1);
                return null;
            } else if (x1 == x3) {
                if (y1 <= y3 && y1 >= y4) {
                    location = new Point2D(x1, y1);
                    return location;
                } else if (y1 >= y3 && y1 <= y4) {
                    location = new Point2D(x1, y1);
                    return location;
                } else if ((y2 <= y3 && y2 >= y4) || (y2 >= y3 && y2 <= y4)) {
                    location = new Point2D(x1, y2);
                    return location;
                }
            } else {
                // System.exit(2);   return null;
            }

        } else if (x1 == x2) {
            //first line is vertical

            a2 = (y4 - y3) / (x4 - x3);
            b2 = y3 - a2 * x3;
            x0 = x1;
            y0 = b2 + a2 * x0;
            location = new Point2D(x0, y0);
            return location;
        } else if (x3 == x4) {
            a1 = (y2 - y1) / (x2 - x1);
            b1 = y1 - a1 * x1;

            x0 = x3;
            y0 = b1 + a1 * x0;
            location = new Point2D(x0, y0);
            return location;
        } else {
            //neither are vertical
            //check parallel now
            a1 = (y2 - y1) / (x2 - x1);
            b1 = y1 - a1 * x1;
            a2 = (y4 - y3) / (x4 - x3);
            b2 = y3 - a2 * x3;


            if (a1 == a2) {
                //they parallel now are they overlapping?
                if ((y1 <= y3 && y1 >= y4) || (y1 >= y3 && y1 <= y4)) {
                    location = new Point2D(x1, y1);
                    return location;
                } else if ((y2 <= y3 && y2 >= y4) ||
                        (y2 >= y3 && y2 <= y4)) {
                    location = new Point2D(x2, y2);
                    return location;

                } else {
                    // System.exit(3);
                    return null;
                }
            }// now we know they arent vertical or parallel
            else {
                x0 = -(b1 - b2) / (a1 - a2);
                y0 = b2 + a2 * x0;
                location = new Point2D(x0, y0);
                if (Math.min(x1, x2) <= x0 && x0 <= Math.max(x1, x2) &&
                        Math.min(x3, x4) <= x0 && x0 <= Math.max(x3, x4)) {
                    return location;
                } else {
                    // System.exit(4);
                    return null;
                }
            }

        }
        System.out.println("intersection not working");
        System.exit(5817);


        return location;
    }

    public static double angle(double dx1, double dy1, double dx2, double dy2) {
        double length1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        dx1 /= length1;
        dy1 /= length1;
        double length2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        dx2 /= length2;
        dy2 /= length2;
        return Math.toDegrees(Math.cos(dx1 * dx2 + dy1 * dy2));
    }


    public static Point2D getClosestIntersect(Line ray, ArrayList<Line> polygonEdges) {
        //System.out.println("Passed ray = " + ray);

        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for (Line inLine : polygonEdges) {
            if (lineIntersect(inLine, ray)) {
                intersectPoints.add(findIntersect2(inLine, ray));
            }

        }


        double min = Double.MAX_VALUE;
        for (int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            tmpLine = new Line(ray.getStartX(), ray.getStartY(), tmpPoint.getX(), tmpPoint.getY());
            dist = Math.sqrt(Math.pow((tmpLine.getEndX() - tmpLine.getStartX()), 2) + Math.pow((tmpLine.getEndY() - tmpLine.getStartY()), 2));

            if (dist < min) {
                minPos = i;
                min = dist;
            }

        }

        if (intersectPoints.size() > 0) {
            return intersectPoints.get(minPos);
        } else {
            //System.out.println("hello");
            return null;
        }
    }


}