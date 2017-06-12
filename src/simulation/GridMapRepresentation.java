/*
package simulation;

import conversion.GridConversion;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public class GridMapRepresentation extends MapRepresentation {

    private int[][] mapArray;

    public GridMapRepresentation(Polygon borderPolygon, ArrayList<Polygon> obstaclePolygons) {
        super(borderPolygon, obstaclePolygons);
        ArrayList<Polygon> merge = new ArrayList<>();
        merge.add(borderPolygon);
        merge.addAll(obstaclePolygons);
        mapArray = GridConversion.convert(merge, 2);
    }

}
*/
