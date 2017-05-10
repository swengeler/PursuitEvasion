package additionalOperations;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import shadowPursuit.ShadowNode;

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

        ObservableList<Double> vertices = poly.getPoints();
        ArrayList<Point2D> points = new ArrayList<>();

        for (int i = 0; i < vertices.size() - 1; i += 2) {
            xPos = vertices.get(i);
            yPos = vertices.get(i + 1);

            points.add(new Point2D(xPos, yPos));
        }

        return points;
    }





    /*
    *Shadowpursuit: will be used to categorize points of type 1
    * Type 1: blocked entierly from  view because of an obstacles
     */

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




    public static Line occRay(Point2D agentPos, ShadowNode t2Point) {
        return occRay(agentPos, t2Point.getPosition());
    }

    public static Line occRay(Point2D agentPos, Point2D  t2Point) {
        Line ray = new Line(agentPos.getX(), agentPos.getY(), t2Point.getX(), t2Point.getY());
        ray.setScaleX(1000);
        ray.setScaleY(1000);
        return ray;

    }

    private static double signed2DTriArea(double ax, double ay, double bx, double by, double cx, double cy) {
        return (ax - cx) * (by - cy) - (ay - cy) * (bx - cx);
    }

    public static Point2D pointIntersect(Line line1, Line line2) {


        double x1, x2, x3, x4;
        double y1, y2, y3, y4;

        x1 = line1.getStartX();
        y1 = line1.getStartY();

        x2 = line1.getEndX();
        y2 = line1.getEndY();

        x3 = line2.getStartX();
        y3 = line2.getStartY();

        x4 = line2.getEndX();
        y4 = line2.getEndY();

        double s1x, s1y, s2x, s2y, s, t;

        s1x = x2 - x1;
        s2x = x4 - x3;
        s1y = y2 - y1;
        s2y = y4 - y3;

        s = (-s1y * (x1 - x3) + s1x * (y1 - y3)) / (-s2x * s1y + s1x * s2y);
        t = (-s1x * (y1 - y3) - s2y * (x1 - x3)) / (-s2x * s1y + s1x * s2y);

        double xInt, yInt;
        xInt = x1 + (t * s1x);
        yInt = y1 + (t * s1y);

        return new Point2D(xInt, yInt);


    }




}
