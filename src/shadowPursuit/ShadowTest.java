package shadowPursuit;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;

import static AdditionalOperations.VectorOperations.polyToPoints;

/**
 * Created by Robins on 26.04.2017.
 */
public class ShadowTest {

    Polygon poly;

    public ShadowTest() {
        poly = new Polygon(704.0,
        179.0,
        415.0,
        172.0,
        380.0,
        230.0,
        275.0,
        300.0,
        226.0,
        197.0,
        131.0,
        301.0,
        156.0,
        498.0,
        378.0,
        491.0,
        410.0,
        339.0,
        678.0,
        453.0,
        785.0,
        345.0);
        ArrayList<Point2D> points = polyToPoints(poly);

        for(Point2D p: points)  {
            System.out.print(p + "\t");
        }

    }

    public static void main(String[] args0) {
        new ShadowTest();
    }



}
