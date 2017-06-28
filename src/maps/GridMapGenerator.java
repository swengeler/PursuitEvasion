package maps;

import experiments.MapGenerator;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import java.util.ArrayList;

public class GridMapGenerator extends MapGenerator {

    private final double[] DEFAULT_COORDS = {
            0.0, 0.0,
            0.0, 400.0,
            100.0, 400.0,
            100.0, 600.0,
            0.0, 600.0,
            0.0, 1000.0,
            100.0, 1000.0,
            100.0, 750.0,
            200.0, 750.0,
            200.0, 250.0,
            100.0, 250.0,
            100.0, 0.0
    };

    private final double xScale = 1.0;
    private final double yScale = 1.0;
    private final boolean xStretch = true;
    private final boolean yStretch = true;

    private final double scale = 0.25;

    private int xDimension = 2;
    private int yDimension = 1;

    private Pane pane;

    public GridMapGenerator() {
        super();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        pane = new Pane();
        Scene scene = new Scene(pane, 200, 200);
        stage = primaryStage;
        stage.setScene(scene);
        stage.show();
        /*stage = primaryStage;
        for (int i = 1; i <= 4; i++) {
            for (int j = i; j <= 4; j++) {
                xDimension = i;
                yDimension = j;
                mapName = "grid_" + xDimension + "_" + yDimension + "_";
                System.out.println(xDimension + ", " + yDimension);
                generateMap();
                for (Polygon p : mapPolygons) {
                    for (int x = 0; x < p.getPoints().size() - 2; x += 2) {
                        if (p.getPoints().get(x).equals(p.getPoints().get(x + 2)) && p.getPoints().get(x + 1).equals(p.getPoints().get(x + 3))) {
                            p.getPoints().remove(x + 2);
                            p.getPoints().remove(x + 2);
                            x -= 2;
                        }
                    }
                }
                saveMap();
                mapPolygons.clear();
            }
        }*/
        xDimension = 2;
        yDimension = 2;
        mapName = "grid_" + xDimension + "_" + yDimension + "_";
        System.out.println(xDimension + ", " + yDimension);
        generateMap();
        for (Polygon p : mapPolygons) {
            for (int x = 0; x < p.getPoints().size() - 2; x += 2) {
                if (p.getPoints().get(x).equals(p.getPoints().get(x + 2)) && p.getPoints().get(x + 1).equals(p.getPoints().get(x + 3))) {
                    p.getPoints().remove(x + 2);
                    p.getPoints().remove(x + 2);
                    x -= 2;
                }
            }
        }
        saveMap();
        mapPolygons.clear();
        System.exit(0);
    }

    protected void generateMap() {
        double horizontalWidth = DEFAULT_COORDS[11] * xScale;
        double horizontalHeight = DEFAULT_COORDS[16] * (yStretch ? yScale : 1.0);
        double verticalWidth = DEFAULT_COORDS[16] * (xStretch ? xScale : 1.0);
        double verticalHeight = DEFAULT_COORDS[11] * yScale;

        ArrayList<Double> points = new ArrayList<>(40);
        /*javafx.scene.shape.Polygon p = new javafx.scene.shape.Polygon();
        p.getPoints().addAll(
                0.0, 0.0, 0.0, 250.0, 100.0, 250.0, 100.0, 750.0, 0.0, 750.0,
                0.0, 1000.0, 350.0, 1000.0, 350.0, 1100.0, 650.0, 1100.0, 650.0, 1000.0,
                1000.0, 1000.0, 1000.0, 650.0, 1100.0, 650.0, 1100.0, 350.0, 1000.0, 350.0,
                1000.0, 0.0, 750.0, 0.0, 750.0, 100.0, 250.0, 100.0, 250.0, 0.0
        );
        for (int i = 0; i < p.getPoints().size(); i++) {
            p.getPoints().set(i, p.getPoints().get(i) * 0.5);
        }
        pane.getChildren().add(p);*/

        javafx.scene.shape.Polygon outer = new javafx.scene.shape.Polygon();
        /*outer.getPoints().addAll(
                0.0, 0.0, 0.0, 450.0, 100.0, 450.0, 100.0, 750.0, 0.0, 750.0, 0.0, 1100.0
        );*/

        double[] innerPoints = new double[]{
                0.0, 0.0, 0.0, 250.0, 125.0, 250.0, 125.0, 750.0, 0.0, 750.0,
                0.0, 1000.0, 350.0, 1000.0, 350.0, 1125.0, 650.0, 1125.0, 650.0, 1000.0,
                1000.0, 1000.0, 1000.0, 650.0, 1125.0, 650.0, 1125.0, 350.0, 1000.0, 350.0,
                1000.0, 0.0, 750.0, 0.0, 750.0, 125.0, 250.0, 125.0, 250.0, 0.0
        };

        double[] outerLeft = new double[]{
                0.0, 0.0, 0.0, 450.0, 125.0, 450.0, 125.0, 750.0, 0.0, 750.0, 0.0, 1100.0
        };
        double[] outerBottom = new double[]{
                0.0, 0.0, 350.0, 0.0, 350.0, 125.0, 850.0, 125.0, 850.0, 0.0, 1100.0, 0.0
        };
        double[] outerRight = new double[]{
                0.0, 0.0, 0.0, 350.0, 125.0, 350.0, 125.0, 850.0, 0.0, 850.0, 0.0, 1100.0
        };
        double[] outerTop = new double[]{
                0.0, 0.0, 450.0, 0.0, 450.0, 125.0, 750.0, 125.0, 750.0, 0.0, 1100.0, 0.0
        };

        /*double[] outerLeft = new double[]{
                0.0, 0.0, 0.0, 450.0, 100.0, 450.0, 100.0, 750.0, 0.0, 750.0, 0.0, 1100.0
        };
        double[] outerBottom = new double[]{
                0.0, 0.0, 350.0, 0.0, 350.0, 100.0, 850.0, 100.0, 850.0, 0.0, 1100.0, 0.0
        };
        double[] outerRight = new double[]{
                0.0, 0.0, 0.0, 350.0, 100.0, 350.0, 100.0, 850.0, 0.0, 850.0, 0.0, 1100.0
        };
        double[] outerTop = new double[]{
                0.0, 0.0, 450.0, 0.0, 450.0, 100.0, 750.0, 100.0, 750.0, 0.0, 1100.0, 0.0
        };*/

        for (int i = 0; i < yDimension; i++) {
            outer.getPoints().addAll(
                    outerLeft[0], outerLeft[1] + i * 1100.0,
                    outerLeft[2], outerLeft[3] + i * 1100.0,
                    outerLeft[4], outerLeft[5] + i * 1100.0,
                    outerLeft[6], outerLeft[7] + i * 1100.0,
                    outerLeft[8], outerLeft[9] + i * 1100.0
            );
            if (i == yDimension - 1) {
                outer.getPoints().addAll(outerLeft[10], outerLeft[11] + i * 1100.0);
            }
        }
        for (int i = 0; i < xDimension; i++) {
            outer.getPoints().addAll(
                    outerBottom[0] + i * 1100.0, outerBottom[1] + yDimension * 1100.0 + 100.0,
                    outerBottom[2] + i * 1100.0, outerBottom[3] + yDimension * 1100.0 + 100.0,
                    outerBottom[4] + i * 1100.0, outerBottom[5] + yDimension * 1100.0 + 100.0,
                    outerBottom[6] + i * 1100.0, outerBottom[7] + yDimension * 1100.0 + 100.0,
                    outerBottom[8] + i * 1100.0, outerBottom[9] + yDimension * 1100.0 + 100.0
            );
            if (i == xDimension - 1) {
                outer.getPoints().addAll(outerBottom[10] + i * 1100.0, outerBottom[11] + yDimension * 1100.0 + 100.0);
            }
        }
        outer.getPoints().addAll(xDimension * 1100.0 + 100.0, yDimension * 1100.0 + 100.0);
        for (int i = yDimension - 1; i >= 0; i--) {
            outer.getPoints().addAll(
                    outerRight[10] + xDimension * 1100.0 + 100.0, outerRight[11] + i * 1100.0,
                    outerRight[8] + xDimension * 1100.0 + 100.0, outerRight[9] + i * 1100.0,
                    outerRight[6] + xDimension * 1100.0 + 100.0, outerRight[7] + i * 1100.0,
                    outerRight[4] + xDimension * 1100.0 + 100.0, outerRight[5] + i * 1100.0,
                    outerRight[2] + xDimension * 1100.0 + 100.0, outerRight[3] + i * 1100.0
            );
            if (i == 0) {
                outer.getPoints().addAll(outerRight[0] + xDimension * 1100.0 + 100.0, outerRight[1] + i * 1100.0);
            }
        }
        for (int i = xDimension - 1; i >= 0; i--) {
            outer.getPoints().addAll(
                    outerTop[10] + 1100.0 * i, outerTop[11],
                    outerTop[8] + 1100.0 * i, outerTop[9],
                    outerTop[6] + 1100.0 * i, outerTop[7],
                    outerTop[4] + 1100.0 * i, outerTop[5],
                    outerTop[2] + 1100.0 * i, outerTop[3]
            );
            if (i == 0) {
                outer.getPoints().addAll(outerTop[0] + 1100.0 * i, outerTop[1]);
            }
        }
        /*for (int i = 0; i < outer.getPoints().size(); i++) {
            outer.getPoints().set(i, outer.getPoints().get(i) * 0.5);
        }*/
        for (int l = 0; l < outer.getPoints().size(); l += 2) {
            outer.getPoints().set(l, outer.getPoints().get(l) * scale);
            outer.getPoints().set(l + 1, outer.getPoints().get(l + 1) * scale);
            Label label = new Label("" + l);
            label.setTranslateX(outer.getPoints().get(l) + 5);
            label.setTranslateY(outer.getPoints().get(l + 1) + 5);
            pane.getChildren().addAll(label, new Circle(outer.getPoints().get(l), outer.getPoints().get(l + 1), 4, Color.BLUE));
        }
        mapPolygons.add(outer);
        //pane.getChildren().add(outer);

        javafx.scene.shape.Polygon inner;
        for (int i = 0; i < xDimension; i++) {
            for (int j = 0; j < yDimension; j++) {
                inner = new javafx.scene.shape.Polygon();
                for (int k = 0; k < innerPoints.length; k += 2) {
                    inner.getPoints().addAll(innerPoints[k] + i * 1000.0 + (i + 1) * 100.0, innerPoints[k + 1] + j * 1000.0 + (j + 1) * 100.0);
                }
                /*for (int l = 0; l < inner.getPoints().size(); l++) {
                    inner.getPoints().set(l, inner.getPoints().get(l) * 0.5);
                }*/
                inner.getPoints().addAll(inner.getPoints().get(0), inner.getPoints().get(1));
                for (int l = 0; l < inner.getPoints().size(); l += 2) {
                    inner.getPoints().set(l, inner.getPoints().get(l) * scale);
                    inner.getPoints().set(l + 1, inner.getPoints().get(l + 1) * scale);
                }
                mapPolygons.add(inner);
                inner.setFill(Color.WHITE);
                //pane.getChildren().add(inner);
            }
        }

        for (javafx.scene.shape.Polygon p : mapPolygons) {
            p.getPoints().remove(p.getPoints().size() - 1);
            p.getPoints().remove(p.getPoints().size() - 1);
        }

        int c = 0;
        for (javafx.scene.shape.Polygon p : mapPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                for (int j = 0; j < p.getPoints().size(); j += 2) {
                    if (i != j && (double) p.getPoints().get(i) == (double) p.getPoints().get(j) && (double) p.getPoints().get(i + 1) == (double) p.getPoints().get(j + 1)) {
                        System.out.println("Duplicate point in polygon " + c + " (i = " + i + ", j = " + j + "): (" + p.getPoints().get(i) + "|" + p.getPoints().get(i + 1) + ")");
                    }
                }
            }
            c++;
        }

        for (int l = 0; l < mapPolygons.get(1).getPoints().size(); l += 2) {
            Label label = new Label("" + l);
            label.setTranslateX(mapPolygons.get(1).getPoints().get(l) + 5);
            label.setTranslateY(mapPolygons.get(1).getPoints().get(l + 1) + 5);
            pane.getChildren().addAll(label, new Circle(mapPolygons.get(1).getPoints().get(l), mapPolygons.get(1).getPoints().get(l + 1), 4, Color.GREEN));
        }

    }

    public static void main(String[] args) {
        launch(args);
    }

}
