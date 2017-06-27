package shadowPursuit;

import javafx.geometry.Point2D;
import shadowPursuit.TreeNode;
import shadowPursuit.WayPoint;

import java.util.*;

/**
 * Created by jonty on 20/06/2017.
 */


public class Permute {

    /*


    public static void main(String[] args) {
        ArrayList<WayPoint> w = new ArrayList<>();

        WayPoint w1 = new WayPoint(new Point2D(80.18465276371738, 266.9690220692448));
        w1.addConnection(new WayPoint(new Point2D(492.78206106870215, 289.57709923664123)));
        w1.addConnection(new WayPoint(new Point2D(600.0156565656569, 241.24646464646446)));
        w1.addConnection(new WayPoint(new Point2D(104.29215615862104, 174.0524290771046)));

        w.add(w1);

        WayPoint w2 = new WayPoint(new Point2D(104.29215615862104, 174.0524290771046));

        w.add(w2);


        WayPoint w3 = new WayPoint(new Point2D(528.5701233439927, 587.5714938328003));
        w3.addConnection(new WayPoint(new Point2D(583.6174636174644, 584.8191268191267)));
        w3.addConnection(new WayPoint(new Point2D(600.0156565656569, 241.24646464646446)));

        w.add(w3);

        WayPoint w4 = new WayPoint(new Point2D(760.539964186879, 354.8687937489831));
        w4.addConnection(new WayPoint(new Point2D(897.0379911245807, 311.72810910271676)));
        w4.addConnection(new WayPoint(new Point2D(554.0852540484962, 496.2108645360296)));
        //w2.addConnection(new WayPoint(new Point2D(528.5701233439927, 587.5714938328003)));
        // w2.addConnection(new WayPoint(new Point2D(583.6174636174644, 584.8191268191267)));

        w.add(w4);


        TreeNode root = new TreeNode(w);
        Collection<WayPoint> wayPoints = root.getWayPoints();
        Set<WayPoint> foo = new HashSet<WayPoint>(root.getWayPoints());

        List<List<WayPoint>> listy = new ArrayList<>();
        listy.add(root.getWayPoints().get(0).getConnected());
        listy.add(root.getWayPoints().get(1).getConnected());
        listy.add(root.getWayPoints().get(2).getConnected());
        listy.add(root.getWayPoints().get(3).getConnected());




        List<List<WayPoint>> result = cartesianProduct(listy);
        for(List<WayPoint> points: result)    {
            for(WayPoint wayP: points)    {
                System.out.print(wayP.getCoord() + "\n");
            }
            System.out.println("\n");
        }

        System.out.println("Size = " + result.size());

        //root.setWayPoints(minsort(root.getWayPoints()));
        //System.out.print(root);
        //root.addChildren();
    }

    protected static  List<List<WayPoint>> cartesianProduct(List<List<WayPoint>> lists) {
        List<List<WayPoint>> resultLists = new ArrayList<List<WayPoint>>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<WayPoint>());
            return resultLists;
        } else {
            List<WayPoint> firstList = lists.get(0);
            List<List<WayPoint>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (WayPoint condition : firstList) {
                for (List<WayPoint> remainingList : remainingLists) {
                    ArrayList<WayPoint> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }



    */



}