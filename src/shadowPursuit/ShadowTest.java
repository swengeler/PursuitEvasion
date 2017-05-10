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
        environment = new Polygon(394.0,396.0,576.0,396.0,578.0,305.0,626.0,310.0,633.0,358.0,633.0,418.0,694.0,442.0,796.0,443.0,794.0,365.0,751.0,323.0,751.0,266.0,766.0,213.0,694.0,159.0,482.0,141.0,423.0,189.0,213.0,207.0,189.0,281.0,116.0,397.0,166.0,489.0,223.0,549.0,247.0,438.0,228.0,371.0,264.0,334.0,307.0,361.0);


        Polygon obstacle1 = new Polygon(663.0 ,352.0 ,664.0 ,385.0 ,764.0 ,395.0);
        Polygon obstacle2 = new Polygon(215.0 ,382.0 ,228.0 ,465.0 ,181.0 ,450.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        obst.add(obstacle1);
        obst.add(obstacle2);

        Point2D agentPos = new Point2D(474, 357);
        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agentPos);



        ArrayList<Polygon> polys = new ArrayList<>();
        polys.add(environment);
        polys.addAll(obst);


        System.out.println("Invisible");
        ArrayList<Point2D> type1 = getX1Points(environment, obst, agents);

        for (Point2D point : type1) {
            System.out.println(point);
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
