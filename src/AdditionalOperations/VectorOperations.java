package AdditionalOperations;


import com.sun.javafx.geom.Vec2d;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import java.lang.Object.*;

import java.util.ArrayList;


/**
 * Created by robin on 22.03.2017.
 */
public class VectorOperations {

    public static ArrayList<Point2D> polyToPoints(Polygon poly)    {

        //Turn polygon into points
        double xPos, yPos;

        ObservableList<Double> vertices = poly.getPoints();
        ArrayList<Point2D> points = new ArrayList<>();

        for(int i = 0; i < vertices.size() - 1; i+=2)  {
            xPos = vertices.get(i);
            yPos = vertices.get(i+1);

            points.add(new Point2D(xPos, yPos));
        }

        return  points;
    }




    public boolean pointIntersect(ArrayList<pointVector> vectors, Point2D AgentPos, Point2D dest) {

        pointVector tarVector = new pointVector(AgentPos, dest);

        for(pointVector vec : vectors) {

        }
        return false;
    }

    public boolean pointIntersect(pointVector vec1, pointVector vec2) {


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

        //Check if boundingBoxes intersect
        if (!(x1 <= x4
                && x2 >= x3
                && y1 <= y4
                && y2 >= y3))   {
            return false;
        }
        else    {
            //Check if parallel
            double d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);

            if(d == 0)
                return false; //Lines are parallel
            else {

                //Find intersection
                //q1 = q - p
                //t = (q − p) × s / (r × s)
                Point2D q1;
                double rs, t;
                rs = crossProduct(vec1.getDestination(), vec2.getDestination());
                q1 = new Point2D(x3-x1, y3 - y1);

                t = crossProduct(q1, vec1.getDestination()) / crossProduct(vec1.getDestination(), vec1.getDestination());


                //p1 = p-q
                //u =p1 × r / (r × s)
                Point2D p1;
                p1 = new Point2D(x1, y4-y2);
                double u = crossProduct(p1, vec1.getDestination()) / crossProduct(vec2.getDestination(), vec1.getDestination());




                //double t =

            }
        }




        return false;

    }

    public static double dotProduct(pointVector v1, pointVector v2)  {
        return (v1.getX() * v2.getX()) + (v1.getY() * v2.getY());
    }

    public static double crossProduct(Point2D p1, Point2D p2)    {
        pointVector v1 = new pointVector(p1.getX(), p1.getY());
        pointVector v2 = new pointVector(p2.getX(), p2.getY());
        return crossProduct(v1,v2);
    }

    public static double crossProduct(pointVector v1, pointVector v2)    {
        double a1,a2,b1,b2;

        a1 = v1.getX();
        a2 = v1.getY();

        b1 = v2.getX();
        b2 = v2.getY();

        return a1*b2 - b1*a2;
    }


}
