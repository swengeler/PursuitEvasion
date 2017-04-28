package additionalOperations;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.Agent;

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



    public static ArrayList<Point2D> getX1Points(Polygon environment, ArrayList<Polygon> obstacles, ArrayList<Agent> agents)  {


        //First you add all points


        ArrayList<Point2D> all = polyToPoints(environment);
        ArrayList<Point2D> vis = new ArrayList<>();
         for(Polygon poly : obstacles)   {
            all.addAll(polyToPoints(poly));
        }

        /* Possible cases for a type1 vertex:
        *   1. environment Vertex is blocked by environment itself
        *   2. environment Vertex is blocked by an obstacle
        *   3. Obstacle Vertex is blocked by enironment
        *   4. Obstacle vertex is blocked by obstacle
         */

        Line temp;
        double agentX, agentY, x2, y2;
        int i = 0;
        boolean visib = true;

        while(i < all.size())   {
            x2 = all.get(i).getX();
            y2 = all.get(i).getY();



            for(Agent agent : agents)   {
                agentX = agent.getXPos();
                agentY = agent.getYPos();

                temp = new Line(agentX, agentY, x2, y2);

                visib = true;
                //Not blocked by environment
                if(!lineIntersectInPoly(environment, temp)) {

                    for(int j = 0; j < obstacles.size(); j++)   {
                        //And if just one obstacle blocks ths view for the controlledAgents this controlledAgents cannnot see the point
                        if(lineIntersectInPoly(obstacles.get(j), temp)) {
                            visib = false;
                            break;
                        }
                    }
                }
            }
            if(visib == false)   {
                all.remove(i);
            }
            else {
                i++;
            }

        }


        return null;
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
        double x1,x2;
        double y1,y2;

        x1 = points.get(0).getX();
        y1 = points.get(0).getY();


        for(int i = 1; i < points.size(); i++)  {

            x2 = points.get(i).getX();
            y2 = points.get(i).getY();

            temp = new Line(x1, y1, x2, y2);

            if(lineIntersect(current, temp)) {
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

        if(lineIntersect(current, temp))
            return true;


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
