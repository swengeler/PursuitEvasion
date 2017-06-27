package shadowPursuit;


import javafx.geometry.Point2D;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;

/**
 * Created by jonty on 25/06/2017.
 */
public class PolyConverter {

    public static Polygon convert(ArrayList<Point2D> points)    {
        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];

        int i = 0;
        for(Point2D point: points)  {
            xPoints[i] = (int) point.getX();
            yPoints[i] = (int) point.getY();
            i++;
        }
        return new Polygon(xPoints,yPoints,points.size());
    }

    public static void main(String[] args){
        ArrayList<Point2D> p1= new ArrayList<>();
        ArrayList<Point2D> p2= new ArrayList<>();
        ArrayList<Point2D> p3= new ArrayList<>();


        p1.add(new Point2D(100,300));
        p1.add(new Point2D(100,0));
        p1.add(new Point2D(200,0));
        p1.add(new Point2D(200,300));

        p2.add(new Point2D(0,200));
        p2.add(new Point2D(0,100));
        p2.add(new Point2D(300,100));
        p2.add(new Point2D(300,200));

        p3.add(new Point2D(400,200));
        p3.add(new Point2D(400,100));
        p3.add(new Point2D(400,100));
        p3.add(new Point2D(400,200));


        System.out.println("must be false" + overlaps(p3,p2));

    }

    public static boolean overlaps(ArrayList<Point2D> p1 , ArrayList<Point2D> p2){
        boolean overlaps=false;
        Polygon poly1 = convert(p1);
        Polygon poly2 = convert(p2);

        /*
        System.out.println("Polygon1");
        for(int i = 0; i < poly1.xpoints.length; i++)   {
            System.out.println("x" + i + " => " + poly1.xpoints[i]);
            System.out.println("y" + i + " => " + poly1.xpoints[i]);
        }

        System.out.println("\nPolygon2");
        for(int i = 0; i < poly2.xpoints.length; i++)   {
            System.out.println("x" + i + " => " + poly2.xpoints[i]);
            System.out.println("y" + i + " => " + poly2.xpoints[i]);
        }
        System.out.println("\n--------\n");
        */

        Area area = new Area(poly1);
        area.intersect(new Area(poly2));
        return area.isEmpty();

    }




    public static double polygonArea(ArrayList<Point2D> points) {
        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];

        int c = 0;
        for(Point2D point: points)  {
            xPoints[c] = (int) point.getX();
            yPoints[c] = (int) point.getY();
            c++;
        }

        double area = 0;         // Accumulates area in the loop
        int j  = points.size()-1;  // The last vertex is the 'previous' one to the first

        for (int i=0; i<points.size(); i++) {
            area = area +  (xPoints[j]+xPoints[i]) * (yPoints[j]-yPoints[i]);
            j = i;  //j is previous vertex to i
        }
        return Math.abs(area/2);
    }

}
