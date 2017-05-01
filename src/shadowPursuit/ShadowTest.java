package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import simulation.Agent;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.getX1Points;


/**
 * Created by Robins on 26.04.2017.
 */
public class ShadowTest {

    Polygon poly;

    public ShadowTest() {
        poly = new Polygon(204, 216, 351, 215, 390, 316, 439, 214, 544, 213, 544, 368, 196, 371);

        Point2D agentPos = new Point2D(500, 230);
        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agentPos);

        ArrayList<Polygon> obstacles = new ArrayList<>();

        //ToDO someting goes wrong here
        ArrayList<Point2D> type1 = getX1Points(poly, obstacles, agents);
        for(Point2D point : type1)  {
            System.out.println(point);
        }

        /*
        ArrayList<Point2D> points = polyToPoints(poly);

        for(Point2D p: points)  {
            System.out.print(p + "\t");
        }*/

    }

    public static void main(String[] args0) {
        new ShadowTest();
    }



}
