package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

import java.util.ArrayList;

/**
 * Created by Robins on 27.06.2017.
 */
public class ShadowPoint {
    private final int type1 = 1;
    private final int type2 = 2;
    private final int type3 = 3;
    private final int type4 = 4;

    ArrayList<Line> rays;
    Point2D coordinate;

    public ShadowPoint(Point2D point, int value)    {
        coordinate = point;
        if(value == type1)  {

        }
        else if(value == type2)   {
            rays = new ArrayList<>();
        }
    }


    public void addRay(Line ray)    {
        rays.add(ray);
    }

    public ArrayList<Line> getRays()    {
        return rays;
    }

}
