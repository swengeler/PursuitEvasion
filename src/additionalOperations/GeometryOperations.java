package additionalOperations;

import Jama.Matrix;
import com.vividsolutions.jts.geom.*;
import entities.utils.PathVertex;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import shadowPursuit.ShadowNode;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class GeometryOperations {

    public static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(1E12));
    //public static final GeometryFactory factory = new GeometryFactory();
    public static final double PRECISION_EPSILON = 1E-10;

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

    public static ArrayList<PathVertex> polyToPathVertices(Polygon poly) {
        List<Double> vertices = poly.getPoints();
        ArrayList<PathVertex> points = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i += 2) {
            // store real point, estimated to be added later
            points.add(new PathVertex(-1.0, -1.0, vertices.get(i), vertices.get(i + 1)));
        }
        return points;
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
        //return lineIntersect(l1, l2.getStartX(), l2.getStartY(), l2.getEndX(), l2.getEndY());
        LineSegment ls1 = new LineSegment(l1.getStartX(), l1.getStartY(), l1.getEndX(), l1.getEndY());
        LineSegment ls2 = new LineSegment(l2.getStartX(), l2.getStartY(), l2.getEndX(), l2.getEndY());
        return ls1.intersection(ls2) != null;
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

    public static boolean lineIntersectSeparatingLines(double x1, double y1, double x2, double y2, ArrayList<Line> separatingLines) {
        LineSegment ls = new LineSegment(x1, y1, x2, y2);
        LineSegment temp;
        for (Line l : separatingLines) {
            temp = new LineSegment(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
            if (temp.intersection(ls) != null) {
                return true;
            }
        }
        return false;
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

    public static boolean inPolygon(Polygon polygon, double x, double y) {
        LineSegment temp;
        for (int i = 0; i < polygon.getPoints().size(); i += 2) {
            temp = new LineSegment(polygon.getPoints().get(i), polygon.getPoints().get(i + 1), polygon.getPoints().get((i + 2) % polygon.getPoints().size()), polygon.getPoints().get((i + 3) % polygon.getPoints().size()));
            if (temp.distance(new Coordinate(x, y)) == 0) {
                return true;
            }
        }
        return polygon.contains(x, y);
    }

    public static boolean inPolygonWithoutBorder(Polygon polygon, double startX, double startY, double endX, double endY) {
        boolean polygonContains = inPolygon(polygon, startX, startY, endX, endY);
        Line temp;
        for (int i = 0; i < polygon.getPoints().size(); i += 2) {
            temp = new Line(polygon.getPoints().get(i), polygon.getPoints().get(i + 1), polygon.getPoints().get((i + 2) % polygon.getPoints().size()), polygon.getPoints().get((i + 3) % polygon.getPoints().size()));
            if (temp.contains((startX + (endX - startX) / 2), (startY + (endY - startY) / 2))) {
                return false;
            }
        }
        return polygonContains;
    }

    public static boolean inPolygonWithoutBorder(Polygon polygon, double x, double y) {
        boolean polygonContains = polygon.contains(x, y);
        LineSegment temp;
        for (int i = 0; i < polygon.getPoints().size(); i += 2) {
            temp = new LineSegment(polygon.getPoints().get(i), polygon.getPoints().get(i + 1), polygon.getPoints().get((i + 2) % polygon.getPoints().size()), polygon.getPoints().get((i + 3) % polygon.getPoints().size()));
            if (temp.distance(new Coordinate(x, y)) != 0) {
                return false;
            }
        }
        return polygonContains;
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
        p1Y = p1.getX();

        p2X = p2.getX();
        p2Y = p2.getX();


        return distance(p1X, p1Y, p2X, p2Y);

    }

    public static double distance(double p1X, double p1Y, double p2X, double p2Y) {
        return Math.sqrt(Math.pow(p1X - p2X, 2) + Math.pow(p1Y - p2Y, 2));
    }

    public static boolean isClockwise(Polygon polygon) {
        // iterate over edges
        double sum = 0;
        for (int i = 0; i < polygon.getPoints().size(); i += 2) {
            sum += (-polygon.getPoints().get((i + 2) % polygon.getPoints().size()) - polygon.getPoints().get(i)) * (-polygon.getPoints().get((i + 3) % polygon.getPoints().size()) + polygon.getPoints().get(i + 1));
        }
        return sum > 0;
    }

    public static boolean leftTurnPredicateInverted(Coordinate c1, Coordinate c2, Coordinate c3) {
        return leftTurnPredicateInverted(c1.x, -c1.y, c2.x, -c2.y, c3.x, -c3.y);
    }

    public static boolean leftTurnPredicateInverted(double p1X, double p1Y, double p2X, double p2Y, double p3X, double p3Y) {
        return leftTurnPredicate(p1X, -p1Y, p2X, -p2Y, p3X, -p3Y);
    }

    public static boolean leftTurnPredicate(Point2D p1, Point2D p2, Point2D p3) {
        return leftTurnPredicate(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    public static boolean leftTurnPredicate(PathVertex p1, PathVertex p2, PathVertex p3) {
        return leftTurnPredicate(p1.getRealX(), p1.getRealY(), p2.getRealX(), p2.getRealY(), p3.getRealX(), p3.getRealY());
    }

    public static boolean leftTurnPredicate(double p1X, double p1Y, double p2X, double p2Y, double p3X, double p3Y) {
        double[][] array = {{1, p1X, p1Y}, {1, p2X, p2Y}, {1, p3X, p3Y}};
        return (new Matrix(array)).det() > 0;
    }

    public static double leftTurnValue(PathVertex p1, PathVertex p2, PathVertex p3) {
        return leftTurnValue(p1.getRealX(), p1.getRealY(), p2.getRealX(), p2.getRealY(), p3.getRealX(), p3.getRealY());
    }

    public static double leftTurnValue(double p1X, double p1Y, double p2X, double p2Y, double p3X, double p3Y) {
        double[][] array = {{1, p1X, p1Y}, {1, p2X, p2Y}, {1, p3X, p3Y}};
        return (new Matrix(array)).det();
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

    public static PointVector closestPoint(double x, double y, Line line) {
        PointVector point = new PointVector(x, y);
        PointVector pointFromLine = new PointVector(x - line.getStartX(), y - line.getStartY());
        PointVector gradient = new PointVector(line.getStartX() - line.getEndX(), line.getStartY() - line.getEndY());
        PointVector normal = new PointVector(-gradient.getY(), gradient.getX());
        normal = VectorOperations.multiply(normal, 1 / normal.length());
        double singedDistance = VectorOperations.dotProduct(pointFromLine, normal);
        PointVector closestPoint = VectorOperations.subtract(point, VectorOperations.multiply(normal, singedDistance));
        return closestPoint;
    }

    // from: https://rootllama.wordpress.com/2014/06/20/ray-line-segment-intersection-test-in-2d/
    public static Point2D rayLineSegIntersection(double rayStartX, double rayStartY, double rayDeltaX, double rayDeltaY, Line lineSegment) {
        /*PointVector lineSegmentEnd = new PointVector(lineSegment.getEndX(), lineSegment.getEndY());
        PointVector v1 = VectorOperations.subtract(new PointVector(rayStartX, rayStartY), lineSegmentEnd);
        PointVector v2 = VectorOperations.subtract(new PointVector(lineSegment.getStartX(), lineSegment.getStartY()), lineSegmentEnd);
        PointVector v3 = new PointVector(-rayDeltaY / length, rayDeltaX / length);

        double t1 = VectorOperations.crossProduct(v2, v1)*/

        double length = Math.sqrt(rayDeltaX * rayDeltaX + rayDeltaY * rayDeltaY);
        rayDeltaX /= length;
        rayDeltaY /= length;
        double rayEndX = rayStartX + 100000 * rayDeltaX;
        double rayEndY = rayStartY + 100000 * rayDeltaY;

        LineSegment ray = new LineSegment(rayStartX, rayStartY, rayEndX, rayEndY);
        LineSegment lineSeg = new LineSegment(lineSegment.getStartX(), lineSegment.getStartY(), lineSegment.getEndX(), lineSegment.getEndY());
        Coordinate intersectionPoint = ray.intersection(lineSeg);

        /*Line2D.Double ray = new Line2D.Double(rayStartX, rayStartY, rayEndX, rayEndY);
        Line2D.Double lineSeg = new Line2D.Double(lineSegment.getStartX(), lineSegment.getStartY(), lineSegment.getEndX(), lineSegment.getEndY());
        if (ray.intersectsLine(lineSeg))
            System.out.printf("(%.3f|%.3f) to (%.3f|%.3f) intersects (%.3f|%.3f) to (%.3f|%.3f)\n", ray.getX1(), ray.getY1(), ray.getX2(), ray.getY2(), lineSeg.getX1(), lineSeg.getY1(), lineSeg.getX2(), lineSeg.getY2());*/
        //return findIntersect2(new Line(rayStartX, rayStartY, rayEndX, rayEndY), lineSegment);
        return intersectionPoint == null ? null : new Point2D(intersectionPoint.x, intersectionPoint.y);
    }

    public static boolean lineTriangleIntersectWithPoints(Line line, DTriangle triangle) {
        for (DEdge de : triangle.getEdges()) {
            if (lineIntersect(line, de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY()) ||
                    onLine(de.getPointLeft().getX(), de.getPointLeft().getY(), line) || onLine(de.getPointRight().getX(), de.getPointRight().getY(), line)) {
                return true;
            }
        }
        return false;
    }

    public static boolean lineTriangleIntersectWithoutPoints(Line line, DTriangle triangle) {
        for (DEdge de : triangle.getEdges()) {
            if (lineIntersect(line, de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY()) &&
                    !(onLine(de.getPointLeft().getX(), de.getPointLeft().getY(), line) || onLine(de.getPointRight().getX(), de.getPointRight().getY(), line))) {
                return true;
            }
        }
        return false;
    }

    public static int lineTriangleIntersectNr(Line line, DTriangle triangle) {
        int result = 0;
        for (DEdge de : triangle.getEdges()) {
            if (lineIntersect(line, de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY()) ||
                    onLine(de.getPointLeft().getX(), de.getPointLeft().getY(), line) || onLine(de.getPointRight().getX(), de.getPointRight().getY(), line)) {
                result++;
            }
        }
        return result;
    }

}