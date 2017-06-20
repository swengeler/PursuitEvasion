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

    ArrayList<WayPoint> connected;

    public WayPoint(Point2D coordinate) {
            this.coordinate = coordinate;
            if(coordinate == null)  {
                System.exit(898);
            }

            connected = new ArrayList<>();
            rays = new ArrayList<>();


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

    }

    public void addConnection(WayPoint toConnect)   {
        if(!connected.contains(toConnect)) {
            connected.add(toConnect);
            toConnect.connected.add(this);
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

}
