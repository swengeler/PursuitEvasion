package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

import java.util.ArrayList;

/**
 * Created by jonty on 16/06/2017.
 */
public class WayPoint {

    Point2D coordinate;
    ArrayList<Line> rays,jontyRays;
    String retString;
    int Checkingdoublelink;


    ArrayList<WayPoint> connected;

    public WayPoint(Point2D coordinate) {
            this.coordinate = coordinate;
            if(coordinate == null)  {
                System.exit(898);
            }

            connected = new ArrayList<>();
            rays = new ArrayList<>();
            connected.add(this);
            Checkingdoublelink=0;


        }
    public  WayPoint(Point2D coordinate, ArrayList<Line> lines){
            this.coordinate = coordinate;
            if(coordinate == null)  {
                System.exit(898);
            }

            connected = new ArrayList<>();
            rays = new ArrayList<>();
            jontyRays=new ArrayList<>();
            connected.add(this);

        for(int i=0; i<lines.size(); i++){
            jontyRays.add(lines.get(i));
        }
        Checkingdoublelink=0;




    }

    public void addConnection(WayPoint toConnect)   {
        int count=0;

        if(!connected.contains(toConnect)) {
             Checkingdoublelink++;
             connected.add(toConnect);
             createString();

        }

    }



    public double getX()    {
        return  coordinate.getX();
    }

    public double getY()    {
        return  coordinate.getY();
    }

    public Point2D getCoord()   {return coordinate;}

    public void printWayPoint()    {
        System.out.println("Waypoint => " + this.coordinate + "\t Connected to: " + (connected.size() - 1));
        for(WayPoint wayP : connected)  {
            System.out.println(wayP.getCoord());
        }
        System.out.println("\n");
    }

    public void printRightWayPoint()    {
        System.out.println("Waypoint => " + this.coordinate + "\t Connected to: " + (connected.size() - 1));
        for(WayPoint wayP : connected)  {
            System.out.println(wayP.getCoord());
        }
        System.out.println("\n");
    }


    public void createString() {
        StringBuilder stringB = new StringBuilder();
        stringB.append("WayPoint = " + this.coordinate);
        stringB.append("\nConnected to");
        for(WayPoint wayP : connected)  {
            stringB.append("\n" + wayP.getCoord());
        }
        stringB.append("\n---------------");

        retString = stringB.toString();
    }

    public String toString()    {
        if(connected.size() > 1)
            return retString;
        else
            return new String("X: " + getX() + "\tY: " + getY());
    }

    public Point2D getCoordinate()  {
        return coordinate;
    }


    public ArrayList<WayPoint> getConnected() {
        return connected;
    }


}
