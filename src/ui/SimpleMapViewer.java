package ui;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class SimpleMapViewer extends Application {

    private static File fileToOpen;

    private Stage stage;
    private ScrollPane pane;
    private Group graphics;

    private ArrayList<Polygon> polygons;
    private double minX, minY, maxX, maxY;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        graphics = new Group();
        pane = new ScrollPane();
        pane.setContent(graphics);
        pane.setFitToWidth(true);
        pane.setPannable(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadMap();
        displayMap();

        Scene scene = new Scene(pane, 1600, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simple map viewer");
        primaryStage.show();
    }

    private void loadMap() {
        if (fileToOpen == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load a map");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
            fileToOpen = fileChooser.showOpenDialog(stage);
        }
        if (fileToOpen != null) {
            polygons = new ArrayList<>();
            Polygon tempPolygon;
            try (BufferedReader in = new BufferedReader(new FileReader(fileToOpen))) {
                // read in the map and file
                String line = in.readLine();
                if (line.contains("map")) {
                    minX = Double.MAX_VALUE;
                    minY = Double.MAX_VALUE;
                    maxX = -Double.MAX_VALUE;
                    maxY = -Double.MAX_VALUE;

                    String[] coords;
                    double[] coordsDouble;
                    while ((line = in.readLine()) != null && !line.contains("agents")) {
                        tempPolygon = new Polygon();
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coordsDouble.length; i += 2) {
                            tempPolygon.getPoints().addAll(coordsDouble[i], coordsDouble[i + 1]);
                            if (coordsDouble[i] > maxX) {
                                maxX = coordsDouble[i];
                            } else if (coordsDouble[i] < minX) {
                                minX = coordsDouble[i];
                            }
                            if (coordsDouble[i + 1] > maxY) {
                                maxY = coordsDouble[i + 1];
                            } else if (coordsDouble[i + 1] < minY) {
                                minY = coordsDouble[i + 1];
                            }
                        }
                        polygons.add(tempPolygon);
                    }

                    /*if (maxX > pane.getWidth()) {
                        double difference = maxX - pane.getWidth();
                        stage.setWidth(stage.getWidth() + difference);
                        stage.centerOnScreen();
                    }
                    if (maxY > pane.getHeight()) {
                        double difference = maxY - pane.getHeight();
                        stage.setHeight(stage.getHeight() + difference);
                        stage.centerOnScreen();
                    }*/
                } else {
                    // map data only
                    minX = Double.MAX_VALUE;
                    minY = Double.MAX_VALUE;
                    maxX = -Double.MAX_VALUE;
                    maxY = -Double.MAX_VALUE;

                    System.out.println(line);
                    String[] coords;
                    double[] coordsDouble;
                    boolean firstLoop = true;
                    while (firstLoop || ((line = in.readLine()) != null && !line.isEmpty())) {
                        firstLoop = false;
                        tempPolygon = new Polygon();
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coordsDouble.length; i += 2) {
                            tempPolygon.getPoints().addAll(coordsDouble[i], coordsDouble[i + 1]);
                            System.out.println("x: " + coordsDouble[i] + ", y: " + coordsDouble[i + 1]);
                            if (coordsDouble[i] > maxX) {
                                maxX = coordsDouble[i];
                            } else if (coordsDouble[i] < minX) {
                                minX = coordsDouble[i];
                            }
                            if (coordsDouble[i + 1] > maxY) {
                                maxY = coordsDouble[i + 1];
                            } else if (coordsDouble[i + 1] < minY) {
                                minY = coordsDouble[i + 1];
                            }
                        }
                        polygons.add(tempPolygon);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.exit(1);
        }
    }

    private void displayMap() {
        if (fileToOpen != null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double screenWidth = screenSize.getWidth();
            double screenHeight = screenSize.getHeight();

            pane.setStyle("-fx-background:#708090;");

            System.out.println(maxX + " " + maxY);

            Polygon p;
            for (int i = 0; i < polygons.size(); i++) {
                p = polygons.get(i);
                if (minX != 0.0) {
                    p.setTranslateX(-minX + 10);
                }
                if (minY != 0.0) {
                    p.setTranslateY(-minY + 10);
                }
                if (i == 0) {
                    p.setStroke(Color.BLACK);
                    p.setFill(Color.WHITE);
                } else {
                    p.setStroke(Color.BLACK);
                    p.setFill(Color.SLATEGREY);
                }
                graphics.getChildren().add(p);
            }

            if (maxX - minX + 50 < screenWidth) {
                stage.setWidth(maxX - minX + 50);
            }
            if (maxY - minY + 50 < screenHeight) {
                stage.setHeight(maxY - minY + 50);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            fileToOpen = new File(args[0]);
        }
        launch(args);
    }

}
