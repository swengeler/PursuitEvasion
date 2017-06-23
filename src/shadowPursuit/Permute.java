package shadowPursuit;

import com.vividsolutions.jts.index.strtree.AbstractSTRtree;
import entities.Tree;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;

/**
 * Created by jonty on 20/06/2017.
 */


    public class Permute{

    public static void bubbleSort(ArrayList<WayPoint> array) {
        boolean swapped = true;
        int j = 0;
         WayPoint tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < array.size() - j; i++) {
                if (array.get(i).getConnected().size() < array.get(i + 1).getConnected().size()) {
                    tmp = array.get(i);
                    array.set(i,array.get(i+1));
                    array.set(i+1,tmp);

                    swapped = true;
                }
            }
        }
    }

        public static void main(String[] args){
            ArrayList<WayPoint> w= new ArrayList<>();

            WayPoint w1 = new WayPoint(new Point2D(80.18465276371738, 266.9690220692448));
            w1.addConnection(new WayPoint(new Point2D(492.78206106870215,289.57709923664123)));
            w1.addConnection(new WayPoint(new Point2D(600.0156565656569,241.24646464646446)));
            w1.addConnection(new WayPoint(new Point2D(104.29215615862104,174.0524290771046)));

            w.add(w1);


            WayPoint w2 = new WayPoint(new Point2D(760.539964186879, 354.8687937489831));
            w2.addConnection(new WayPoint(new Point2D(897.0379911245807, 311.72810910271676)));
            w2.addConnection(new WayPoint(new Point2D(554.0852540484962, 496.2108645360296)));
            //w2.addConnection(new WayPoint(new Point2D(528.5701233439927, 587.5714938328003)));
           // w2.addConnection(new WayPoint(new Point2D(583.6174636174644, 584.8191268191267)));

            w.add(w2);

            TreeNode root = new TreeNode(w);


            root.setWayPoints(minsort(root.getWayPoints()));
            System.out.print(root);
            root.addChildren();
        }

        public static ArrayList<WayPoint> minsort(ArrayList<WayPoint> w) {

            int minCon = Integer.MAX_VALUE;
            int pos = 0;

            for(int i = 0; i < w.size() - 1; i++)   {
                System.out.println("ENTERED");
                minCon = Integer.MAX_VALUE;
                pos = i;
                for(int j = i+1; j < w.size(); j++) {
                    if(w.get(j).getConnected().size() < minCon)    {
                        System.out.println("ENTERED 2");
                        minCon = w.get(j).getConnected().size();
                        pos = j;
                    }
                }

                WayPoint temp = w.get(i);
                System.out.println("Before\nPos => " + pos + w.get(pos).getCoordinate());
                System.out.println("i => " + i + "\tValue at" + w.get(i).getConnected() + "\n");
                w.set(i,w.get(pos));
                w.set(pos, temp);
                System.out.println("After\nPos => " + pos + "\tValue at" + w.get(pos).getCoordinate());
                System.out.println("i => " + i + "\tValue at" + w.get(i).getConnected() + "\n");

            }
            return w;
        }

}

