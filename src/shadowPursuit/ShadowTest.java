package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.getX1Points;
import static shadowPursuit.shadowOperations.polyToPoints;


/**
 * Created by Robins on 26.04.2017.
 */
public class ShadowTest {

    Polygon environment;
    private MapRepresentation map;
    private ArrayList<Point2D> agents;

    private Point2D type1, type2, type3, type4;
    private ShadowGraph shadows;
    private PursuitTree pursuitTree;


    public ShadowTest() {
        //TODO Problem1
        //environment = new Polygon(237.0,139.0,164.0,164.0,109.0,245.0,133.0,307.0,224.0,289.0,243.0,271.0,251.0,339.0,236.0,383.0,195.0,445.0,168.0,515.0,189.0,575.0,241.0,579.0,340.0,566.0,418.0,548.0,419.0,430.0,379.0,367.0,321.0,321.0,281.0,282.0,276.0,268.0,305.0,216.0,374.0,182.0,428.0,172.0,483.0,153.0,460.0,108.0,337.0,85.0,262.0,78.0);

        //TODO Problem2
        //environment = new Polygon(407.0,103.0,321.0,171.0,281.0,299.0,452.0,249.0,480.0,191.0,526.0,285.0,596.0,245.0,542.0,176.0,691.0,183.0,742.0,102.0,552.0,54.0);
        //Point2D agentPos = new Point2D(521, 206);

        //TODO Problem4
        //environment = new Polygon(482.0,348.0,482.0,382.0,440.0,408.0,415.0,394.0,398.0,351.0,357.0,373.0,314.0,442.0,346.0,486.0,418.0,514.0,465.0,513.0,479.0,505.0,520.0,447.0,523.0,422.0,542.0,455.0,547.0,484.0,628.0,577.0,642.0,556.0,692.0,475.0,747.0,443.0,769.0,388.0,743.0,329.0,665.0,329.0,639.0,377.0,614.0,392.0,556.0,387.0,534.0,387.0,533.0,355.0,535.0,275.0,613.0,295.0,654.0,244.0,643.0,188.0,450.0,102.0,409.0,128.0,363.0,212.0,287.0,229.0,281.0,258.0,330.0,293.0,370.0,292.0,406.0,293.0,432.0,319.0);
        //Point2D agentPos = new Point2D(505, 363);

        //TODO Problem5
        //environment = new Polygon(526.0,281.0,527.0,304.0,510.0,313.0,475.0,303.0,461.0,282.0,418.0,280.0,410.0,322.0,497.0,383.0,557.0,385.0,605.0,383.0,621.0,345.0,664.0,308.0,633.0,262.0,601.0,274.0,567.0,300.0,566.0,279.0,606.0,256.0,663.0,258.0,669.0,277.0,697.0,230.0,598.0,175.0,520.0,162.0,481.0,182.0,359.0,160.0,356.0,178.0,385.0,233.0,482.0,287.0,466.0,223.0,508.0,227.0);
        //Point2D agentPos = new Point2D(545, 287);

        //environment = new Polygon(473.0,266.0,534.0,282.0,536.0,316.0,511.0,336.0,489.0,298.0,451.0,303.0,457.0,355.0,514.0,370.0,573.0,383.0,611.0,366.0,626.0,304.0,632.0,245.0,618.0,232.0,596.0,274.0,590.0,309.0,570.0,305.0,568.0,285.0,576.0,243.0,594.0,225.0,631.0,213.0,665.0,273.0,681.0,250.0,681.0,168.0,657.0,132.0,534.0,172.0,399.0,186.0,357.0,232.0,354.0,280.0,372.0,344.0,406.0,359.0,426.0,318.0);
        //Point2D agentPos = new Point2D(556, 303);


        //environment = new Polygon(177.0,97.0,262.0,98.0,262.0,366.0,410.0,367.0,414.0,94.0,526.0,92.0,528.0,721.0,409.0,724.0,408.0,451.0,257.0,447.0,256.0,724.0,170.0,722.0);

        //Point2D agent1Pos = new Point2D(442, 343);
        //Point2D agent2Pos = new Point2D(441, 465);

        environment = new Polygon(726.0,186.0,906.0,184.0,904.0,212.0,793.0,210.0,795.0,308.0,118.0,313.0,120.0,276.0,297.0,270.0,299.0,166.0,141.0,172.0,141.0,130.0,334.0,130.0,331.0,268.0,376.0,263.0,376.0,108.0,280.0,108.0,279.0,68.0,479.0,69.0,478.0,124.0,399.0,122.0,402.0,172.0,480.0,176.0,476.0,205.0,406.0,203.0,408.0,262.0,524.0,256.0,526.0,161.0,448.0,155.0,455.0,135.0,534.0,138.0,533.0,43.0,444.0,41.0,441.0,16.0,602.0,18.0,600.0,258.0,681.0,254.0,688.0,135.0,916.0,138.0,917.0,158.0,730.0,159.0);

        Point2D agent1Pos = new Point2D(716.1833256563797, 252.26255181943804);
        Point2D agent2Pos = new Point2D(532.8638919767213, 30.06973778853353);
        Point2D agent3Pos = new Point2D(894, 280);
        Point2D agent4Pos = new Point2D(798, 220);



        Polygon obstacle1 = new Polygon(132.0,73.0,317.0,70.0,321.0,345.0,543.0,338.0,544.0,61.0,937.0,53.0,944.0,680.0,852.0,681.0,845.0,262.0,703.0,266.0,721.0,681.0,542.0,685.0,543.0,395.0,319.0,402.0,318.0,696.0,111.0,697.0);//new Polygon(340.0 ,472.0 ,393.0 ,535.0 ,390.0 ,618.0);
        Polygon obstacle2 = null;//new Polygon(766.0 ,509.0 ,747.0 ,596.0 ,804.0 ,612.0);

        ArrayList<Polygon> obst = new ArrayList<>();
        // obst.add(obstacle1);
        //obst.add(obstacle2);


        ArrayList<Point2D> agents = new ArrayList<>();
        agents.add(agent1Pos);
        agents.add(agent2Pos);
        //agents.add(agent3Pos);
        //agents.add(agent4Pos);


        ArrayList<Polygon> polys = new ArrayList<>();
        polys.add(environment);
        //polys.addAll(obst);


        //System.out.println("Invisible");
        /*ArrayList<Point2D> type1 = getX1Points(environment, obst, agents);

        for (Point2D point : type1) {
            System.out.println(point);
        }*/

        //System.out.println("\n");
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

        for(Point2D point : polyToPoints(environment))  {
            System.out.println(point);
        }
        System.out.println("------");

        shadows = new ShadowGraph(environment, obst, agents);
        //shadows.printNodes();
        shadows.printShadows();

    }


    public void generateShadowGraph() {

    }


    public static void main(String[] args0) {
        new ShadowTest();
    }


}