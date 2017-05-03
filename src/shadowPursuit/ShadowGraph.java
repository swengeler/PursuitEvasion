package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Polygon;
import simulation.MapRepresentation;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.*;

/**
 * Created by Robins on 30.04.2017.
 */
public class ShadowGraph {


    ArrayList<ShadowNode> Nodes;

    private Polygon environment;
    private ArrayList<Polygon> obstacles;
    private ArrayList<Polygon> allPolygons;

    private ArrayList<Point2D> agents;


    public ShadowGraph(MapRepresentation map, ArrayList<Point2D> agents)    {
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();

        allPolygons = map.getAllPolygons();
        this.agents = agents;


    }


    public void generateType1() {
        ArrayList<Point2D> t1 = getX1Points(environment, obstacles, agents);

        ShadowNode temp;
        for(Point2D point : t1) {
            temp = new ShadowNode(point);
            Nodes.add(temp);
        }


    }


    public void generateT1Connections() {
        ShadowNode start, left, right;
        Polygon tempPoly;
        Point2D leftP, rightP;
        ArrayList<Point2D> tempPoints;

        for(int i = 0; i < Nodes.size(); i++)   {
            start = Nodes.get(i);

            //Get adjacent points
            if(start.prev == null || start.next != null) {
                tempPoints = getAdjacentPoints(start.getPosition(), allPolygons);
                if (tempPoints.size() == 0) {
                    System.exit(234567);
                } else {
                    leftP = tempPoints.get(0);
                    rightP = tempPoints.get(1);
                    for(ShadowNode node : Nodes)    {
                        if(node.getPosition() == leftP) {
                            start.prev = node;
                        }
                        else if(node.getPosition() == rightP)   {
                            start.next = node;
                        }
                        if(start.next != null && start.prev != null) {
                            break;
                        }
                    }

                }
            }
        }
    }




}
