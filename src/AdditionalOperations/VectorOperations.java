package AdditionalOperations;


import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;
import java.util.ArrayList;

/**
 * Created by robin on 22.03.2017.
 */
public class VectorOperations {

    public static ArrayList<Position> polyToPoints(Polygon poly)    {

        //Turn polygon into points
        double xPos, yPos;

        ObservableList<Double> vertices = poly.getPoints();
        ArrayList<Position> points = new ArrayList<>();

        for(int i = 0; i < vertices.size() - 1; i+=2)  {
            xPos = vertices.get(i);
            yPos = vertices.get(i+1);

            points.add(new Position(xPos, yPos));
        }

        return  points;
    }


    public static ArrayList<Vector2D> PolyToVec(Polygon poly) {


        ArrayList<Position> points = polyToPoints(poly);


        //Turn points to Vectors
        ArrayList<Vector2D> vecs = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i += 2) {
            vecs.add(new Vector2D(points.get(i), points.get(i + 1)));
        }
        //vecs.add(points.get(points.size()-1), points.get(0));

        return null;

    }

}
