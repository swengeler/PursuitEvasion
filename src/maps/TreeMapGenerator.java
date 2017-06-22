package maps;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import javafx.application.Application;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;

public class TreeMapGenerator extends Application {

    /*private final double[] DEFAULT_COORDS = {
            0.0, 0.0,
            0.0, 200.0,
            100.0, 200.0,
            100.0, 300.0,
            0.0, 300.0,
            0.0, 500.0,
            100.0, 500.0,
            100.0, 400.0,
            200.0, 400.0,
            200.0, 100.0,
            100.0, 100.0,
            100.0, 0.0
    };*/

    private final double[] DEFAULT_COORDS = {
            0.0, 0.0,
            0.0, 400.0,
            100.0, 400.0,
            100.0, 600.0,
            0.0, 600.0,
            0.0, 1000.0,
            100.0, 1000.0,
            100.0, 800.0,
            200.0, 800.0,
            200.0, 200.0,
            100.0, 200.0,
            100.0, 0.0
    };

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        saveMap();
    }

    private void saveMap() {
        Polygon p = new Polygon(DEFAULT_COORDS);
        Coordinate[] coordinates = new Coordinate[DEFAULT_COORDS.length / 2 + 1];
        for (int i = 0; i < DEFAULT_COORDS.length; i += 2) {
            coordinates[i / 2] = new Coordinate(DEFAULT_COORDS[i], DEFAULT_COORDS[i + 1]);
        }
        coordinates[coordinates.length - 1] = new Coordinate(DEFAULT_COORDS[0], DEFAULT_COORDS[1]);
        GeometryFactory factory = new GeometryFactory();
        com.vividsolutions.jts.geom.Polygon polygon = new com.vividsolutions.jts.geom.Polygon(new LinearRing(new CoordinateArraySequence(coordinates), factory), null, factory);
        com.vividsolutions.jts.geom.Polygon otherPolygon = new com.vividsolutions.jts.geom.Polygon(new LinearRing(new CoordinateArraySequence(coordinates), factory), null, factory);

        LinearRing ring = new LinearRing(new CoordinateArraySequence(new Coordinate[]{new Coordinate(0.0, 0.0), new Coordinate(0.0, 1.0), new Coordinate(1.0, 1.0), new Coordinate(1.0, 0.0), new Coordinate(0.0, 0.0)}), factory);

        com.vividsolutions.jts.geom.Polygon rectangle = new com.vividsolutions.jts.geom.Polygon(ring, null, factory);


        AffineTransformation transformation = new AffineTransformation();
        transformation.scale(2.0, 1.0);
        transformation.rotate(Math.PI / 2);
        /*transformation.translate(11.0, 0.0);*/
        Geometry test = transformation.transform(polygon);

        /*AffineTransformation otherTransformation = new AffineTransformation();
        otherTransformation.translate(0.0, 1.0);
        Geometry otherTest = otherTransformation.transform(otherPolygon);

        test = test.union(otherTest).union(rectangle);*/

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the current map");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Map data only file", "*.mdo"));
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            // write map to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(selectedFile))) {
                /*for (int i = 0; i < mapPolygons.size(); i++) {
                    for (int j = 0; j < mapPolygons.get(i).getPoints().size(); j++) {
                        out.print(mapPolygons.get(i).getPoints().get(j) + " ");
                    }
                    out.println();
                }*/
                /*for (int j = 0; j < p.getPoints().size(); j++) {
                    out.print((p.getPoints().get(j) * 100) + " ");
                }*/
                for (int i = 0; i < test.getCoordinates().length; i++) {
                    out.print(test.getCoordinates()[i].x + " ");
                    out.print(test.getCoordinates()[i].y + " ");
                }
                out.println();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
