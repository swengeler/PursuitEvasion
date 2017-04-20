package AdditionalOperations;


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

            }
        }




        return false;

    }

    public static double dotProduct(pointVector v1, pointVector v2)  {
        return (v1.getX() * v2.getX()) + (v1.getY() * v2.getY());
    }




}
