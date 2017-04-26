package AdditionalOperations;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by simon on 4/24/17.
 */
public class GeometryOperations {

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

    //TODO @Rob continue here
    public static boolean lineIntersectInPoly(ArrayList<Point2D> points, Line l) {



        return false;
    }


    public static boolean lineIntersect(Line l1, Line l2) {
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

    private static double signed2DTriArea(double ax, double ay, double bx, double by, double cx, double cy) {
        return (ax - cx) * (by - cy) - (ay - cy) * (bx - cx);
    }

    //TODO @Rob continue here
    public ArrayList<Point2D> visiblePoints(ArrayList<Point2D> polyPoints, Point2D position)   {
        ArrayList<Point2D> visPoints = new ArrayList<>();
        Line temp;


        for(Point2D point : polyPoints) {

        }
        return null;
    }


}
