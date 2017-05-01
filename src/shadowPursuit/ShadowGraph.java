package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.inPolygon;

/**
 * Created by Robins on 30.04.2017.
 */
public class ShadowGraph {


    ArrayList<ShadowNode> Nodes;

    public ShadowGraph()    {
        Nodes = new ArrayList<>();

    }


    public void addType1(Point2D type1)   {
        ShadowNode newNode = new ShadowNode(type1);
        if(Nodes.size() == 0)   {
            Nodes.add(newNode);
        }
        else    {

        }
    }

    public void addConnectedType1(ArrayList<Point2D> connectedType1){


        boolean alreadyAdded =false;
        //Check first if not already in lIst

        if(Nodes.size() != 0) {
            for (ShadowNode node : Nodes) {
                if (inPolygon(node.getPosition(), connectedType1)) {
                    //Means that this list also in the list of all nodes - so not interesting
                    alreadyAdded = true;
                    break;
                }
            }
            if(!alreadyAdded)   {
                addNodesToList(connectedType1);
            }

        }
        else    {
            addNodesToList(connectedType1);
        }
    }


    public void addNodesToList(ArrayList<Point2D> points)    {
        ShadowNode temp;
        for(int i = 0; i < points.size(); i++) {
            temp = new ShadowNode(points.get(i));
            if(i > 0)   {
                Nodes.get(i-1).addnextType1(temp);
            }
        }
    }

    public void printGraph()    {
        ShadowNode temp, start;
        for(int i = 0; i < Nodes.size(); i++) {
            start = Nodes.get(i);
            temp = Nodes.get(i);
            while (temp.next != null && temp.next != start)   {
                System.out.print(temp + "\t");
                temp = Nodes.get(++i);
            }
            System.out.println();
        }
    }


}
