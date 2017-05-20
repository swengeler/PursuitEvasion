package additionalOperations;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import shadowPursuit.ShadowNode;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by simon on 4/24/17.
 */
public class GeometryOperations {


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

        if (poly.getPoints().size() != 0) {

            ObservableList<Double> vertices = poly.getPoints();
            ArrayList<Point2D> points = new ArrayList<>();

            for (int i = 0; i < vertices.size() - 1; i += 2) {
                xPos = vertices.get(i);
                yPos = vertices.get(i + 1);

                points.add(new Point2D(xPos, yPos));
            }


            return points;
        } else
            return null;
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


    public static Line scaleRay(Point2D agentPos, ShadowNode t2Point, double value) {
        return scaleRay(agentPos, t2Point.getPosition(), value);
    }

    public static Line scaleRay(Point2D agentPos, Point2D t2Point, double value) {
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

        if (aX - endX != 0) {
            if (endX > aX) {
                System.out.print("wooh its working = " + m);
                newX = endX + value;
                newY = endY + value * m;
            } else {
                System.out.print("why the fuck man");
                newX = endX - value;
                newY = endY - value * m;
            }
        } else {
            if (endY > aY) {
                System.out.print("wooh its working = " + m);
                newX = endX;
                newY = endY + value;
            } else {
                System.out.print("why the fuck man");
                newX = endX;
                newY = endY - value;
            }
        }


        Line ray = new Line(endX, endY, newX, newY);
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
                ray.setEndX(intPoint.getX());
                ray.setEndY(intPoint.getY());
            }
            else if (lineIntersect(yLine, ray))  {
                System.out.println("Enter2");
                intPoint = FindIntersection(yLine, ray);
                System.out.println(intPoint);
                ray.setEndX(intPoint.getX());
                ray.setEndY(intPoint.getY());
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

        if (l1x == 0) {
            l1x = 0.000000001;
        }

        if (l2x == 0) {
            l2x = 0.000000001;
        }


        double slope1 = (line1.getEndY() - line1.getStartY()) / l1x;
        double slope2 = (line2.getEndY() - line2.getStartY()) / l2x;


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


    public static Point2D pointIntersect2(Line line1, Line line2) {

        double px, py, qx, qy, ry, rx, sx, sy, ty, tx;

        px = line1.getStartX();
        py = line1.getStartY();

        rx = line1.getEndX() - px;
        ry = line1.getEndY() - py;

        qx = line2.getStartX();
        qy = line2.getStartY();

        sx = line2.getEndX() - qx;
        sy = line2.getEndY() - qy;

        ty = (qy - py) * sy / (ry * sy);
        tx = (qx - px) * sx / (rx * sx);


/*
        t = (q − p) × s / (r × s)
        q= beginning of 1
        p= begining of 2
        P+r= end of 2
        q+s= end of 1

        p+tr= intersection.




        s = (-s1y * (x1 - x3)  + s1x * (y1 - y3)) / (-s2x * s1y + s1x * s2y);
        t = (-s1x * (y1 - y3) - s2y * (x1 - x3)) / (-s2x * s1y + s1x * s2y);
 */
        double xInt, yInt;
        xInt = px + (tx * sx);
        yInt = py + (ty * sy);

        return new Point2D(xInt, yInt);
    }

    public static double gradient(Line line) {

        double x1, x2, y1, y2;

        x1 = line.getStartX();
        y1 = line.getStartY();

        x2 = line.getEndX();
        y2 = line.getEndY();

        return (y2 - y1) / (x2 - x1);
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
        } else
            return false;

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


}
