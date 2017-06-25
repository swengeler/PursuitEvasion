package ui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import entities.utils.PathVertex;
import experiments.IndexPair;
import experiments.SquareGuardInfo;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

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

    private SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph;
    private ArrayList<Geometry> visibilityPolygons;
    private ArrayList<SquareGuardInfo> squareGuardInfo;
    private ArrayList<ArrayList<Coordinate>> lineGuardOriginalPositions;
    private ArrayList<ArrayList<Coordinate>> triangleGuardOriginalPositions;

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

        load();
        //displayMap();

        Scene scene = new Scene(pane, 1600, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simple map viewer");
        primaryStage.show();
    }

    private void load() {
        if (fileToOpen == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load a map");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa", "*.spm", "*.sgi", "*.lgi", "*.tgi"));
            fileToOpen = fileChooser.showOpenDialog(stage);
        }
        if (fileToOpen != null) {
            if (fileToOpen.getName().contains(".spm")) {
                File parent = fileToOpen.getParentFile();
                File[] directoryContents = parent.listFiles();
                if (directoryContents != null) {
                    for (File f : directoryContents) {
                        if (f.getName().startsWith(fileToOpen.getName().substring(0, fileToOpen.getName().length() - 4)) && (f.getName().endsWith(".mdo") || f.getName().endsWith(".maa"))) {
                            // draw map first and the shortest path on top
                            loadMap(f);
                            displayMap();
                            loadShortestPathRoadMap(fileToOpen);
                            displayShortestPathRoadMap();
                            break;
                        }
                    }
                }
            } else if (fileToOpen.getName().contains(".sgi")) {
                File parent = fileToOpen.getParentFile();
                File[] directoryContents = parent.listFiles();
                if (directoryContents != null) {
                    for (File f : directoryContents) {
                        if (f.getName().startsWith(fileToOpen.getName().substring(0, fileToOpen.getName().length() - 4)) && (f.getName().endsWith(".mdo") || f.getName().endsWith(".maa"))) {
                            // draw map first and the shortest path on top
                            loadMap(f);
                            displayMap();
                            loadSquareGuardManagerInfo(fileToOpen);
                            displaySquareGuardManagerInfo();
                            break;
                        }
                    }
                }
            } else if (fileToOpen.getName().contains(".lgi")) {
                File parent = fileToOpen.getParentFile();
                File[] directoryContents = parent.listFiles();
                if (directoryContents != null) {
                    for (File f : directoryContents) {
                        if (f.getName().startsWith(fileToOpen.getName().substring(0, fileToOpen.getName().length() - 4)) && (f.getName().endsWith(".mdo") || f.getName().endsWith(".maa"))) {
                            // draw map first and the shortest path on top
                            loadMap(f);
                            displayMap();
                            loadLineGuardManagerInfo(fileToOpen);
                            displayLineGuardManagerInfo();
                            break;
                        }
                    }
                }
            } else if (fileToOpen.getName().contains(".tgi")) {
                File parent = fileToOpen.getParentFile();
                File[] directoryContents = parent.listFiles();
                if (directoryContents != null) {
                    for (File f : directoryContents) {
                        if (f.getName().startsWith(fileToOpen.getName().substring(0, fileToOpen.getName().length() - 4)) && (f.getName().endsWith(".mdo") || f.getName().endsWith(".maa"))) {
                            // draw map first and the shortest path on top
                            loadMap(f);
                            displayMap();
                            loadTriangleGuardManagerInfo(fileToOpen);
                            displayTriangleGuardManagerInfo();
                            break;
                        }
                    }
                }
            } else if (fileToOpen.getName().contains(".mdo") || fileToOpen.getName().contains(".maa")) {
                loadMap(fileToOpen);
                displayMap();
            }
        } else {
            System.exit(1);
        }
    }

    private void loadTriangleGuardManagerInfo(File fileToOpen) {
        if (fileToOpen != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                triangleGuardOriginalPositions = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayTriangleGuardManagerInfo() {
        if (triangleGuardOriginalPositions != null) {
            for (ArrayList<Coordinate> arr : triangleGuardOriginalPositions)  {
                for (Coordinate c : arr) {
                    graphics.getChildren().add(new Circle(c.x, c.y, 4, Color.INDIANRED));
                }
            }
        }
    }

    private void loadLineGuardManagerInfo(File fileToOpen) {
        if (fileToOpen != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                lineGuardOriginalPositions = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayLineGuardManagerInfo() {
        if (lineGuardOriginalPositions != null) {
            for (ArrayList<Coordinate> arr : lineGuardOriginalPositions) {
                for (Coordinate c : arr) {
                    graphics.getChildren().add(new Circle(c.x, c.y, 4, Color.LAWNGREEN));
                }
            }
        }
    }

    private void loadSquareGuardManagerInfo(File fileToOpen) {
        if (fileToOpen != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                squareGuardInfo = (ArrayList<SquareGuardInfo>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void displaySquareGuardManagerInfo() {
        if (squareGuardInfo != null) {
            for (SquareGuardInfo sgi : squareGuardInfo) {
                for (Coordinate c : sgi.originalPositions) {
                    graphics.getChildren().add(new Circle(c.x, c.y, 4, Color.CYAN));
                }
            }
        }
    }

    private void loadVisibilityPolygons(File fileToOpen) {
        if (fileToOpen != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                visibilityPolygons = (ArrayList<Geometry>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayVisibilityPolygons() {
        if (visibilityPolygons != null) {
            //Geometry g = visibilityPolygons.get(0);
            Polygon p;
            for (Geometry g : visibilityPolygons) {
                p = new Polygon();
                for (Coordinate c : g.getCoordinates()) {
                    p.getPoints().addAll(c.x, c.y);
                }
                p.setFill(Color.YELLOW.deriveColor(1.0, 1.0, 1.0, 0.1));
                p.setStroke(Color.BLACK);
                p.setStrokeWidth(0.5);
                graphics.getChildren().add(p);
            }
        }
    }

    private void loadShortestPathRoadMap(File fileToOpen) {
        if (fileToOpen != null) {
            try (BufferedReader in = new BufferedReader(new FileReader(fileToOpen))) {
                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                String line = in.readLine();
                String[] numbers;
                double[] coordinates;
                int[] indeces;
                while ((line = in.readLine()) != null && !line.contains("ip")) {
                    numbers = line.split(" ");
                    coordinates = new double[numbers.length];
                    for (int i = 0; i < numbers.length; i++) {
                        coordinates[i] = Double.parseDouble(numbers[i]);
                    }
                    pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                }

                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                while ((line = in.readLine()) != null) {
                    numbers = line.split(" ");
                    indeces = new int[numbers.length];
                    for (int i = 0; i < numbers.length; i++) {
                        indeces[i] = Integer.parseInt(numbers[i]);
                    }
                    indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                }

                for (PathVertex pv : pathVertices) {
                    graphics.getChildren().add(new Circle(pv.getRealX(), pv.getRealY(), 3, Color.BLUE));
                }
                Line l;
                for (IndexPair ip : indexPairs) {
                    l = new Line(pathVertices.get(ip.index1).getRealX(), pathVertices.get(ip.index1).getRealY(), pathVertices.get(ip.index2).getRealX(), pathVertices.get(ip.index2).getRealY());
                    l.setStrokeWidth(1.75);
                    l.setStroke(Color.BLUE);
                    graphics.getChildren().add(l);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayShortestPathRoadMap() {
        if (shortestPathGraph != null) {
            Line l;
            for (PathVertex pv1 : shortestPathGraph.vertexSet()) {
                graphics.getChildren().add(new Circle(pv1.getRealX(), pv1.getRealY(), 3, Color.BLUE));
                for (PathVertex pv2: shortestPathGraph.vertexSet()) {
                    if (!pv1.equals(pv2) && shortestPathGraph.containsEdge(pv1, pv2)) {
                        l = new Line(pv1.getRealX(), pv1.getRealY(), pv2.getRealX(), pv2.getRealY());
                        l.setStroke(Color.BLUE);
                        l.setStrokeWidth(2);
                        graphics.getChildren().add(l);
                    }
                }
            }
        }
    }

    private void loadMap(File fileToOpen) {
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
