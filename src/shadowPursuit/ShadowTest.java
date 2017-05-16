package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import simulation.MapRepresentation;

import java.util.ArrayList;

import static additionalOperations.GeometryOperations.polyToPoints;
import static shadowPursuit.shadowOperations.getAdjacentPoints;
import static shadowPursuit.shadowOperations.getX1Points;


/**
 * Created by Robins on 26.04.2017.
 */
public class ShadowTest {

    Polygon environment;
    private MapRepresentation map;
    private ArrayList<Point2D> agents;

    private Point2D type1, type2, type3, type4;
    private ShadowGraph shadows;


    public ShadowTest() {
        environment = new Polygon(162.0,105.0,914.0,91.0,958.0,754.0,135.0,750.0,748.0,558.0,121.0,718.0,735.0,511.0,94.0,657.0,727.0,459.0,105.0,558.0);

        Polygon obstacle1 = null;//new Polygon(340.0 ,472.0 ,393.0 ,535.0 ,390.0 ,618.0);
        Polygon obstacle2 = null;//new Polygon(766.0 ,509.0 ,747.0 ,596.0 ,804.0 ,612.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        //obst.add(obstacle1);
        //obst.add(obstacle2);

        Point2D agentPos = new Point2D(397, 301);
        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agentPos);



        ArrayList<Polygon> polys = new ArrayList<>();
        polys.add(environment);
        polys.addAll(obst);


        System.out.println("Invisible");
        ArrayList<Point2D> type1 = getX1Points(environment, obst, agents);

        for (Point2D point : type1)  {
            //System.out.println(point);
        }

        System.out.println("\n");
        /*
        System.out.println("\nvisible");
        ArrayList<Point2D> env = polyToPoints(environment);
        for(Point2D point : env)  {
            if(!type1.contains(point))    {
                //System.out.println(point);
                ArrayList<Point2D> connected = getAdjacentPoints(point, polys);
                //System.out.println(connected.size());
                System.out.print("\nFor = " + point + "\n" + connected.get(0) + " \t " + connected.get(1) + "\n");
            }


        }
        */

        //ArrayList<Point2D> connected = getAdjacentPoints(type1.get(0), polys);
        //System.out.println("\nFor = " + type1.get(0) + "\n" + connected.get(0) + " \t " + connected.get(1));



        shadows = new ShadowGraph(environment, obst, agents);



    }


    public void generateShadowGraph() {

    }


    public static void main(String[] args0) {
        new ShadowTest();
    }


}
