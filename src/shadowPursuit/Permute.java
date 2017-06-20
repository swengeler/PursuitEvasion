package shadowPursuit;

import javafx.geometry.Point2D;

import java.util.ArrayList;

/**
 * Created by jonty on 20/06/2017.
 */


    public class Permute{
        static void permute(java.util.List<WayPoint> arr, int k){
            for(int i = k; i < arr.size(); i++){
                java.util.Collections.swap(arr, i, k);
                permute(arr, k+1);
                java.util.Collections.swap(arr, k, i);
            }
            if (k == arr.size() -1){

                //System.out.println(java.util.Arrays.toString(arr.));
            }
        }
        public static void main(String[] args){
            ArrayList<WayPoint> w= new ArrayList<>();
            Point2D a= new Point2D(1,2);
            Point2D b= new Point2D(2,2);
            Point2D c= new Point2D(3,2);
            Point2D d= new Point2D(4,2);


            w.add(new WayPoint(a));
            w.add(new WayPoint(b));
            w.add(new WayPoint(c));
            w.add(new WayPoint(d));

            Permute.permute(w, 0);
        }
    }
