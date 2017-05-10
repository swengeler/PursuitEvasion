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
        environment = new Polygon(521.0,485.0,605.0,492.0,627.0,365.0,692.0,400.0,705.0,497.0,670.0,598.0,673.0,632.0,775.0,680.0,855.0,671.0,870.0,620.0,862.0,538.0,835.0,494.0,805.0,426.0,800.0,363.0,791.0,327.0,757.0,284.0,690.0,250.0,520.0,249.0,447.0,276.0,404.0,309.0,342.0,374.0,291.0,435.0,279.0,511.0,298.0,575.0,323.0,639.0,379.0,690.0,436.0,676.0,450.0,625.0,446.0,548.0,436.0,492.0,441.0,415.0,448.0,370.0,470.0,360.0,522.0,380.0,534.0,418.0);

        Polygon obstacle1 = new Polygon(340.0 ,472.0 ,393.0 ,535.0 ,390.0 ,618.0);
        Polygon obstacle2 = new Polygon(766.0 ,509.0 ,747.0 ,596.0 ,804.0 ,612.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        obst.add(obstacle1);
        obst.add(obstacle2);

        Point2D agentPos = new Point2D(567, 440);
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
