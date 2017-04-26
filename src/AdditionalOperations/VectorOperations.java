package AdditionalOperations;


import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;


/**
 * Created by robin on 22.03.2017.
 */
public class VectorOperations {




    public boolean pointIntersect(ArrayList<pointVector> vectors, Point2D AgentPos, Point2D dest) {

        pointVector tarVector = new pointVector(AgentPos, dest);

        for (pointVector vec : vectors) {

        }
        return false;
    }

    public static Point2D pointIntersect(pointVector vec1, pointVector vec2) {


        double x1, x2, x3, x4;
        double y1, y2, y3, y4;

        x1 = vec1.getOrigin().getX();
        y1 = vec1.getOrigin().getY();

        x2 = vec1.getDestination().getX();
        y2 = vec1.getDestination().getY();

        x3 = vec2.getOrigin().getX();
        y3 = vec2.getOrigin().getY();

        x4 = vec2.getDestination().getX();
        y4 = vec2.getDestination().getY();

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

    public static void main(String[] args0) {
        Point2D p, q, r, s;

        p = new Point2D(1, 1);
        q = new Point2D(5, 1);

        r = new Point2D(4.5, 5.5);
        s = new Point2D(-3.5, 3.5);

        pointVector v1, v2;

        v1 = new pointVector(p, r);
        v2 = new pointVector(q, s);

        System.out.println("intersect at = " + pointIntersect(v1, v2));
    }

    public static double dotProduct(pointVector v1, pointVector v2) {
        return (v1.getX() * v2.getX()) + (v1.getY() * v2.getY());
    }

    public static double crossProduct(Point2D p1, Point2D p2) {
        pointVector v1 = new pointVector(p1.getX(), p1.getY());
        pointVector v2 = new pointVector(p2.getX(), p2.getY());
        return crossProduct(v1, v2);
    }

    public static double crossProduct(pointVector v1, pointVector v2) {
        double a1, a2, b1, b2;

        a1 = v1.getX();
        a2 = v1.getY();

        b1 = v2.getX();
        b2 = v2.getY();

        return a1 * b2 - b1 * a2;
    }


}
