package shadowPursuit;

import javafx.geometry.Point2D;
import java.util.ArrayList;

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

    public void addConnectedType1(){

    }


}
