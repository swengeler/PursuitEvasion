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
        //TODO Problem1
        //environment = new Polygon(237.0,139.0,164.0,164.0,109.0,245.0,133.0,307.0,224.0,289.0,243.0,271.0,251.0,339.0,236.0,383.0,195.0,445.0,168.0,515.0,189.0,575.0,241.0,579.0,340.0,566.0,418.0,548.0,419.0,430.0,379.0,367.0,321.0,321.0,281.0,282.0,276.0,268.0,305.0,216.0,374.0,182.0,428.0,172.0,483.0,153.0,460.0,108.0,337.0,85.0,262.0,78.0);

        //TODO Problem2
        //environment = new Polygon(407.0,103.0,321.0,171.0,281.0,299.0,452.0,249.0,480.0,191.0,526.0,285.0,596.0,245.0,542.0,176.0,691.0,183.0,742.0,102.0,552.0,54.0);
        //Point2D agentPos = new Point2D(521, 206);


        environment = new Polygon(211.0,244.0,505.0,140.0,744.0,165.0,675.0,241.0,494.0,231.0,544.0,297.0,490.0,337.0,428.0,261.0,408.0,317.0,266.0,324.0);
        Point2D agentPos = new Point2D(470, 260);



        Polygon obstacle1 = null;//new Polygon(340.0 ,472.0 ,393.0 ,535.0 ,390.0 ,618.0);
        Polygon obstacle2 = null;//new Polygon(766.0 ,509.0 ,747.0 ,596.0 ,804.0 ,612.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        //obst.add(obstacle1);
        //obst.add(obstacle2);



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
