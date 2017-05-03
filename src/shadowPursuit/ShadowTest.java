package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

import static additionalOperations.GeometryOperations.polyToPoints;
import static shadowPursuit.shadowOperations.getX1Points;


/**
 * Created by Robins on 26.04.2017.
 */
public class ShadowTest {

    Polygon poly;
    private MapRepresentation map;
    private ArrayList<Point2D> agents;

    private Point2D type1, type2, type3, type4;
    private ShadowGraph shadows;







    public ShadowTest() {
        poly = new Polygon(682.0,281.0,657.0,281.0,650.0,300.0,642.0,285.0,639.0,264.0,656.0,252.0,675.0,236.0,694.0,240.0,713.0,242.0,718.0,227.0,713.0,214.0,697.0,202.0,676.0,199.0,658.0,203.0,645.0,209.0,615.0,227.0,601.0,241.0,598.0,267.0,604.0,298.0,613.0,318.0,627.0,328.0,652.0,332.0,671.0,324.0,677.0,303.0);

        Point2D agentPos = new Point2D(670, 291);
        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agentPos);

        ArrayList<Polygon> obstacles = new ArrayList<>();


        ArrayList<Point2D> type1 = getX1Points(poly, obstacles, agents);
        for(Point2D point : type1)  {
            System.out.println(point);
        }

        System.out.println();
        ArrayList<Point2D> points = polyToPoints(poly);

    }


    public void generateShadowGraph()   {

    }


    public static void main(String[] args0) {
        new ShadowTest();
    }



}
