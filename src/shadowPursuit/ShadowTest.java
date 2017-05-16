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
        environment = new Polygon(352.0,214.0,
                301.0,259.0,
                268.0,318.0,
                281.0,378.0,
                303.0,447.0,
                360.0,462.0,
                421.0,480.0,
                410.0,556.0,
                367.0,613.0,
                367.0,656.0,
                439.0,676.0,
                503.0,670.0,
                603.0,641.0,
                740.0,601.0,
                795.0,556.0,
                725.0,458.0,
                636.0,408.0,
                633.0,327.0,
                726.0,245.0,
                705.0,174.0,
                616.0,134.0,
                552.0,131.0,
                448.0,162.0);

        Polygon obstacle1 = null;//new Polygon(340.0 ,472.0 ,393.0 ,535.0 ,390.0 ,618.0);
        Polygon obstacle2 = null;//new Polygon(766.0 ,509.0 ,747.0 ,596.0 ,804.0 ,612.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        //obst.add(obstacle1);
        //obst.add(obstacle2);

        Point2D agentPos = new Point2D(566, 542);
        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agentPos);



        ArrayList<Polygon> polys = new ArrayList<>();
        polys.add(environment);
        polys.addAll(obst);


        System.out.println("Invisible");
        ArrayList<Point2D> type1 = getX1Points(environment, obst, agents);

        for (Point2D point : type1) {
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
