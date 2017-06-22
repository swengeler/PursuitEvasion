package maps;

import com.vividsolutions.jts.geom.*;
import javafx.application.Application;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;

public class GridMapGenerator extends Application {

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

    private Stage stage;

    private GeometryFactory factory;
    private Geometry finalPolygon;

    private ArrayList<javafx.scene.shape.Polygon> mapPolygons;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        factory = new GeometryFactory(new PrecisionModel(1));
        finalPolygon = new Polygon(null, null, factory);
        mapPolygons = new ArrayList<>();
        generatePolygon();
        saveMap();
    }

    private void generatePolygon() {
        final double xScale = 1.0;
        final double yScale = 1.0;
        final boolean xStretch = true;
        final boolean yStretch = true;

        final int xDimension = 2;
        final int yDimension = 1;

        double horizontalWidth = DEFAULT_COORDS[11] * xScale;
        double horizontalHeight = DEFAULT_COORDS[16] * (yStretch ? yScale : 1.0);
        double verticalWidth = DEFAULT_COORDS[16] * (xStretch ? xScale : 1.0);
        double verticalHeight = DEFAULT_COORDS[11] * yScale;

        Pane pane = new Pane();
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
                0.0, 0.0, 0.0, 250.0, 100.0, 250.0, 100.0, 750.0, 0.0, 750.0,
                0.0, 1000.0, 350.0, 1000.0, 350.0, 1100.0, 650.0, 1100.0, 650.0, 1000.0,
                1000.0, 1000.0, 1000.0, 650.0, 1100.0, 650.0, 1100.0, 350.0, 1000.0, 350.0,
                1000.0, 0.0, 750.0, 0.0, 750.0, 100.0, 250.0, 100.0, 250.0, 0.0
        };

        double[] outerLeft = new double[]{
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
        };

        for (int i = 0; i < yDimension; i++) {
            outer.getPoints().addAll(
                    outerLeft[0], outerLeft[1] + i * 1100.0,
                    outerLeft[2], outerLeft[3] + i * 1100.0,
                    outerLeft[4], outerLeft[5] + i * 1100.0,
                    outerLeft[6], outerLeft[7] + i * 1100.0,
                    outerLeft[8], outerLeft[9] + i * 1100.0,
                    outerLeft[10], outerLeft[11] + i * 1100.0
            );
        }
        for (int i = 0; i < xDimension; i++) {
            outer.getPoints().addAll(
                    outerBottom[0] + i * 1100.0, outerBottom[1] + yDimension * 1100.0 + 100.0,
                    outerBottom[2] + i * 1100.0, outerBottom[3] + yDimension * 1100.0 + 100.0,
                    outerBottom[4] + i * 1100.0, outerBottom[5] + yDimension * 1100.0 + 100.0,
                    outerBottom[6] + i * 1100.0, outerBottom[7] + yDimension * 1100.0 + 100.0,
                    outerBottom[8] + i * 1100.0, outerBottom[9] + yDimension * 1100.0 + 100.0,
                    outerBottom[10] + i * 1100.0, outerBottom[11] + yDimension * 1100.0 + 100.0
            );
        }
        outer.getPoints().addAll(xDimension * 1100.0 + 100.0, yDimension * 1100.0 + 100.0);
        for (int i = yDimension - 1; i >= 0; i--) {
            outer.getPoints().addAll(
                    outerRight[10] + xDimension * 1100.0 + 100.0, outerRight[11] + i * 1100.0,
                    outerRight[8] + xDimension * 1100.0 + 100.0, outerRight[9] + i * 1100.0,
                    outerRight[6] + xDimension * 1100.0 + 100.0, outerRight[7] + i * 1100.0,
                    outerRight[4] + xDimension * 1100.0 + 100.0, outerRight[5] + i * 1100.0,
                    outerRight[2] + xDimension * 1100.0 + 100.0, outerRight[3] + i * 1100.0,
                    outerRight[0] + xDimension * 1100.0 + 100.0, outerRight[1] + i * 1100.0
            );
        }
        for (int i = xDimension - 1; i >= 0; i--) {
            outer.getPoints().addAll(
                    outerTop[10] + 1100.0 * i, outerTop[11],
                    outerTop[8] + 1100.0 * i, outerTop[9],
                    outerTop[6] + 1100.0 * i, outerTop[7],
                    outerTop[4] + 1100.0 * i, outerTop[5],
                    outerTop[2] + 1100.0 * i, outerTop[3],
                    outerTop[0] + 1100.0 * i, outerTop[1]
            );
        }
        /*for (int i = 0; i < outer.getPoints().size(); i++) {
            outer.getPoints().set(i, outer.getPoints().get(i) * 0.5);
        }*/
        for (int l = 0; l < outer.getPoints().size(); l += 2) {
            outer.getPoints().set(l, outer.getPoints().get(l) * 0.5);
            outer.getPoints().set(l + 1, outer.getPoints().get(l + 1) * 0.5);
        }
        mapPolygons.add(outer);
        pane.getChildren().add(outer);

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
                for (int l = 0; l < inner.getPoints().size(); l += 2) {
                    inner.getPoints().set(l, inner.getPoints().get(l) * 0.5);
                    inner.getPoints().set(l + 1, inner.getPoints().get(l + 1) * 0.5);
                }
                mapPolygons.add(inner);
                inner.setFill(Color.WHITE);
                pane.getChildren().add(inner);
            }
        }


        /*
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[DEFAULT_COORDS.length / 2 + 1];
        for (int i = 0; i < DEFAULT_COORDS.length; i += 2) {
            coordinates[i / 2] = new Coordinate(DEFAULT_COORDS[i], DEFAULT_COORDS[i + 1]);
        }
        coordinates[coordinates.length - 1] = new Coordinate(DEFAULT_COORDS[0], DEFAULT_COORDS[1]);

        Geometry vertical = new LinearRing(new CoordinateArraySequence(coordinates), factory);
        Geometry horizontal = new LinearRing(new CoordinateArraySequence(coordinates), factory);
        Geometry connector = new LinearRing(new CoordinateArraySequence(new Coordinate[]{new Coordinate(0.0, 0.0), new Coordinate(0.0, 200.0), new Coordinate(200.0, 200.0), new Coordinate(200.0, 0.0), new Coordinate(0.0, 0.0)}), factory);

        AffineTransformation horizontalTransform = new AffineTransformation();
        horizontalTransform.rotate(Math.PI / 2);
        horizontalTransform.translate(horizontalWidth, 0.0);
        horizontalTransform.scale(xScale, yStretch ? yScale : 1.0);
        horizontal = horizontalTransform.transform(horizontal);

        AffineTransformation verticalTransform = new AffineTransformation();
        verticalTransform.scale(xStretch ? xScale : 1.0, yScale);
        vertical = verticalTransform.transform(vertical);

        AffineTransformation connectorTransform = new AffineTransformation();
        connectorTransform.scale((xStretch ? xScale : 1.0) * 0.5, (yStretch ? yScale : 1.0) * 0.5);
        connector = connectorTransform.transform(connector);

        System.out.println("horizontalWidth: " + horizontalWidth);
        System.out.println("horizontalHeight: " + horizontalHeight);
        System.out.println("verticalWidth: " + verticalWidth);
        System.out.println("verticalHeight: " + verticalHeight);

        AffineTransformation translateTransform;
        Geometry tempHorizontal, tempVertical, tempConnector, tempGeometry;
        ArrayList<Geometry> geometries = new ArrayList<>();
        Geometry[] geometryArray = new Geometry[3];
        for (int i = 0; i < xDimension; i++) {
            for (int j = 0; j < yDimension; j++) {
                // do the horizontal ones first
                translateTransform = new AffineTransformation();
                translateTransform.translate((i + 1) * verticalWidth * 0.5 + i * horizontalWidth, j * horizontalHeight * 0.5 + j * verticalHeight);
                tempHorizontal = translateTransform.transform(horizontal);

                // then do the vertical ones
                translateTransform = new AffineTransformation();
                translateTransform.translate(i * horizontalWidth + i * verticalWidth * 0.5, (j + 1) * horizontalHeight * 0.5 + j * verticalHeight);
                tempVertical = translateTransform.transform(vertical);

                // and finally the connectors
                translateTransform = new AffineTransformation();
                translateTransform.translate(i * verticalWidth * 0.5 + i * horizontalWidth, j * horizontalHeight * 0.5 + j * verticalHeight);
                tempConnector = translateTransform.transform(connector);

                finalPolygon = tempConnector;
                finalPolygon = tempHorizontal.union(tempVertical).union(tempConnector);

                *//*geometries.add(tempHorizontal);
                geometries.add(tempVertical);
                geometries.add(tempConnector);*//*
                geometryArray[0] = tempHorizontal;
                geometryArray[1] = tempVertical;
                geometryArray[2] = tempConnector;
            }
        }
        //finalPolygon = TopologyPreservingSimplifier.simplify(finalPolygon, 0.1);
        //finalPolygon = (new GeometryPrecisionReducer(new PrecisionModel(1))).reduce(finalPolygon);
        //finalPolygon = horizontal.union(vertical);
        *//*GeometryCollection geometryCollection = new GeometryCollection(geometryArray, factory);
        finalPolygon = geometryCollection.getBoundary();*//*
        *//*for (Geometry g : geometries) {
            finalPolygon = finalPolygon.union(g);
        }*//*
        *//*translateTransform = new AffineTransformation();
        translateTransform.translate(1 * verticalWidth * 0.5 + 0 * horizontalWidth, 0 * horizontalHeight * 0.5 + 0 * verticalHeight);
        tempHorizontal = translateTransform.transform(horizontal);*//*
        //finalPolygon = finalPolygon.union(horizontal).union(vertical).union(connector);
        for (int i = 0; i < xDimension; i++) {

        }
        for (int i = 0; i < yDimension; i++) {

        }


        *//*Polygon polygon = new Polygon(new LinearRing(new CoordinateArraySequence(coordinates), factory), null, factory);
        Polygon otherPolygon = new Polygon(new LinearRing(new CoordinateArraySequence(coordinates), factory), null, factory);

        finalPolygon = polygon;

        LinearRing ring = new LinearRing(new CoordinateArraySequence(new Coordinate[]{new Coordinate(0.0, 0.0), new Coordinate(0.0, 1.0), new Coordinate(1.0, 1.0), new Coordinate(1.0, 0.0), new Coordinate(0.0, 0.0)}), factory);

        Polygon rectangle = new Polygon(ring, null, factory);


        AffineTransformation transformation = new AffineTransformation();
        transformation.scale(1.0, 2.0);
        transformation.rotate(Math.PI / 2);
        transformation.translate(11.0, 0.0);
        Geometry test = transformation.transform(polygon);

        AffineTransformation otherTransformation = new AffineTransformation();
        otherTransformation.scale(1.0, 2.0);
        otherTransformation.translate(0.0, 1.0);
        Geometry otherTest = otherTransformation.transform(otherPolygon);

        test = test.union(otherTest).union(rectangle);*//*

        int counter = 0;
        Label l;
        *//*for (Coordinate c : finalPolygon.getCoordinates()) {
            l = new Label("" + counter);
            l.setTranslateX(c.x + 5);
            l.setTranslateY(c.y + 5);
            pane.getChildren().addAll(new Circle(c.x, c.y, 4, Color.GREEN), l);
            System.out.println(counter + " - x: " + c.x + ", y: " + c.y);
            counter++;
        }*//*
        Scene scene = new Scene(pane, 1700, 900);
        stage.setScene(scene);
        stage.show();*/
    }

    private void saveMap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the current map");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Map data only file", "*.mdo"));
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            // write map to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(selectedFile))) {
                for (int i = 0; i < mapPolygons.size(); i++) {
                    for (int j = 0; j < mapPolygons.get(i).getPoints().size(); j++) {
                        out.print(mapPolygons.get(i).getPoints().get(j) + " ");
                    }
                    out.println();
                }
                /*for (int j = 0; j < p.getPoints().size(); j++) {
                    out.print((p.getPoints().get(j) * 100) + " ");
                }*/
                /*for (int i = 0; i < finalPolygon.getCoordinates().length; i++) {
                    out.print(finalPolygon.getCoordinates()[i].x + " ");
                    out.print(finalPolygon.getCoordinates()[i].y + " ");
                }*/
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
