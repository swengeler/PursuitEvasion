package ui;

import control.Controller;
import conversion.GridConversion;
import javafx.animation.StrokeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import simulation.*;

import java.io.*;
import java.util.*;

public class Main extends Application {

    private enum ProgramState {
        MAP_EDITING, AGENT_PLACING, SIMULATION
    }

    private static final double CELL_SIZE = 5;

    private Stage stage;

    private HBox outerLayout;
    private VBox menu;
    private ZoomablePane pane;

    private Line indicatorLine;
    public static ArrayList<MapPolygon> mapPolygons;
    private MapPolygon currentMapPolygon;

    private ArrayList<Shape> covers;

    private ArrayList<Circle> pursuers;
    private ArrayList<Circle> evaders;
    private ArrayList<VisualAgent> visualAgents;

    private BooleanProperty addPoints;

    private ProgramState currentState;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Overwrite current map?");
        alert.setHeaderText("Loading a saved map will overwrite the one you are editing right now.");
        alert.setContentText("Are you ok with this?");

        stage = primaryStage;

        covers = new ArrayList<>();
        currentState = ProgramState.MAP_EDITING;

        // zoomable drawing pane
        pane = new ZoomablePane();
        /*pane.setOnKeyTyped(e -> {
            System.out.println("df");
            if (e.isControlDown()) {
                if (e.isShiftDown() && e.getCode() == KeyCode.S) {
                    saveMapAndAgents();
                } else if (e.getCode() == KeyCode.S) {
                    saveMapOnly();
                } else if (e.getCode() == KeyCode.O) {
                    loadMap();
                }
            }
        });*/

        // top-level container, partitions window into drawing pane and menu
        outerLayout = new HBox();
        outerLayout.setPrefSize(1200, 800);

        // sidebar menu, currently with dummy buttons
        menu = new VBox();
        menu.setStyle("-fx-background-color: #ffffff");
        menu.setMinWidth(190);
        menu.setPrefSize(190, 600);
        menu.setMaxWidth(190);

        // zoomable drawing pane
        pane = new ZoomablePane();

        // separator between pane and menu
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setStyle("-fx-background-color: #ffffff");
        separator.setMinWidth(10);
        separator.setPrefWidth(10);
        separator.setMaxWidth(10);

        // adding elements to the top-level container
        outerLayout.getChildren().addAll(pane, separator, menu);
        menu.toFront();
        HBox.setHgrow(pane, Priority.ALWAYS);
        HBox.setHgrow(menu, Priority.NEVER);

        // line indicating where a line will be drawn when clicked
        indicatorLine = new Line();
        indicatorLine.setVisible(false);
        indicatorLine.setStroke(Color.FORESTGREEN);
        indicatorLine.setStrokeWidth(3.0);
        indicatorLine.setStrokeLineCap(StrokeLineCap.ROUND);
        pane.getChildren().add(indicatorLine);

        currentMapPolygon = new MapPolygon(pane);
        pane.getChildren().add(currentMapPolygon);
        mapPolygons = new ArrayList<>();
        mapPolygons.add(currentMapPolygon);
        visualAgents = new ArrayList<>();
        addListeners();

        Button clearMapButton = new Button("Clear map");
        clearMapButton.setOnAction(e -> clearMap());
        menu.getChildren().add(clearMapButton);

        Button placeAgentsButton = new Button("Start placing agents");
        placeAgentsButton.setOnAction(e -> {
            initPlaceAgents();
        });
        menu.getChildren().add(placeAgentsButton);

        Button saveMapButton = new Button("Save map");
        saveMapButton.setOnAction(e -> {
            if (mapPolygons.size() > 0 && mapPolygons.get(0).isClosed()) {
                saveMapOnly();
            }
        });
        menu.getChildren().add(saveMapButton);

        Button saveMapAndAgentsButton = new Button("Save map and agents");
        saveMapAndAgentsButton.setOnAction(e -> {
            if (mapPolygons.size() > 0 && mapPolygons.get(0).isClosed()) {
                saveMapAndAgents();
            }
        });
        menu.getChildren().add(saveMapAndAgentsButton);

        Button loadButton = new Button("Load map");
        loadButton.setOnAction(e -> {
            // maybe show message if overwriting current map
            if (currentState != ProgramState.SIMULATION) {
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    loadMap();
                } else {
                    alert.close();
                }
            }
        });
        menu.getChildren().add(loadButton);

        Button theBestTestButton = new Button("The best simulation");
        theBestTestButton.setOnAction(e -> {
            //

            Polygon outer = new Polygon();


            for (int i = 1; i < mapPolygons.size(); i++) {
                mapPolygons.get(i).setFill(Color.WHITE);
                mapPolygons.get(i).toFront();
            }

            Controller.theBestTest(mapPolygons, visualAgents);
            RevealedMap.showMapDebug(pane);
        });
        //menu.getChildren().add(theBestTestButton);
        /*Button simulationButton = new Button("Better simulation");
        simulationButton.setOnAction(e -> {
            Controller.betterTest(mapPolygons, pursuers, evaders);
        });*/
        Button convertButton = new Button("Print Grid");
        convertButton.setOnAction(e -> {
            GridConversion.convert(mapPolygons, pursuers, evaders, CELL_SIZE);
        });
        menu.getChildren().add(convertButton);
        addPoints = new SimpleBooleanProperty(false);
        CheckBox b = new CheckBox("To draw or\nnot to draw");
        addPoints.bind(b.selectedProperty());
        b.setSelected(true);
        //menu.getChildren().add(b);

        Button startSimulationButton = new Button("Start simulation");
        Button pauseSimulationButton = new Button("Pause simulation");

        menu.getChildren().add(startSimulationButton);
        menu.getChildren().add(pauseSimulationButton);

        startSimulationButton.setOnAction(ae -> {
            Simulation sim = Controller.getSimulation();
            if (sim == null) {
                if (mapPolygons == null || visualAgents == null || mapPolygons.isEmpty() || visualAgents.isEmpty()) {
                    System.out.println("Not enough data to construct simulation!");
                } else {
                    Controller.theBestTest(mapPolygons, visualAgents);
                    RevealedMap.showMapDebug(pane);
                    currentState = ProgramState.SIMULATION;
                }
            } else {
                sim.unPause();
            }
        });

        pauseSimulationButton.setOnAction(ae -> {
            Simulation sim = Controller.getSimulation();
            if (sim == null) {
                System.out.println("Simulation not started yet!");
            } else {
                sim.pause();
            }
        });

        Button triangulationButton = new Button("Show triangulation");
        triangulationButton.setOnAction(e -> {
            if (mapPolygons == null || mapPolygons.isEmpty()) {
                System.out.println("Not enough data to construct simulation!");
            } else {
                try {
                    ArrayList<DEdge> constraintEdges = new ArrayList<>();
                    Polygon p;
                    for (MapPolygon mp : mapPolygons) {
                        p = mp.getPolygon();
                        if (p != null) {
                            for (int i = 0; i < p.getPoints().size(); i += 2) {
                                constraintEdges.add(new DEdge(new DPoint(p.getPoints().get(i), p.getPoints().get(i + 1), 0), new DPoint(p.getPoints().get((i + 2) % p.getPoints().size()), p.getPoints().get((i + 3) % p.getPoints().size()), 0)));
                            }
                        }
                    }
                    ConstrainedMesh mesh = new ConstrainedMesh();
                    mesh.setConstraintEdges(constraintEdges);
                    mesh.processDelaunay();
                    List<DTriangle> triangles = mesh.getTriangleList();
                    List<DTriangle> includedTriangles = new ArrayList<>();

                    for (DTriangle dt : triangles) {
                        // check if triangle in polygon
                        double centerX = dt.getBarycenter().getX();
                        double centerY = dt.getBarycenter().getY();
                        boolean inPolygon = true;
                        if (!mapPolygons.get(0).contains(centerX, centerY)) {
                            inPolygon = false;
                        }
                        for (int i = 1; inPolygon && i < mapPolygons.size() - 1; i++) {
                            if (mapPolygons.get(i).contains(centerX, centerY)) {
                                inPolygon = false;
                            }
                        }
                        if (inPolygon) {
                            //System.out.printf("(%f|%f), (%f|%f), (%f|%f)\n", dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                            p = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                            p.setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
                            p.setFill(Color.WHITE.deriveColor(1, 1, 1, 0.1));
                            p.setStrokeWidth(1);

                            Circle c = new Circle(dt.getBarycenter().getX(), dt.getBarycenter().getY(), 5);
                            c.setFill(Color.BLUE);

                            Label index = new Label(includedTriangles.size() + "");
                            index.setTranslateX(c.getCenterX() + 10);
                            index.setTranslateY(c.getCenterY() + 10);

                            pane.getChildren().addAll(p, c, index);
                            includedTriangles.add(dt);
                        }
                    }

                    ArrayList<DEdge> checkedEdges = new ArrayList<>();
                    for (DTriangle dt1 : includedTriangles) {
                        for (DEdge de1 : dt1.getEdges()) {
                            if (!checkedEdges.contains(de1)) {
                                DTriangle otherTriangle = null;
                                DEdge connectingEdge = null;
                                for (DTriangle dt2 : includedTriangles) {
                                    for (DEdge de2 : dt2.getEdges()) {
                                        if (dt2 != dt1 && de1 == de2) {
                                            otherTriangle = dt2;
                                            connectingEdge = de2;
                                            break;
                                        }
                                    }
                                    if (otherTriangle != null) {
                                        break;
                                    }
                                }
                                if (otherTriangle != null) {
                                    /*Line l = new Line(dt1.getBarycenter().getX(), dt1.getBarycenter().getY(), otherTriangle.getBarycenter().getX(), otherTriangle.getBarycenter().getY());
                                    l.setStroke(Color.RED);
                                    l.setStrokeWidth(2);
                                    pane.getChildren().add(l);*/
                                    Line l1 = new Line(dt1.getBarycenter().getX(), dt1.getBarycenter().getY(), connectingEdge.getBarycenter().getX(), connectingEdge.getBarycenter().getY());
                                    l1.setStroke(Color.RED);
                                    l1.setStrokeWidth(2);
                                    Line l2 = new Line(connectingEdge.getBarycenter().getX(), connectingEdge.getBarycenter().getY(), otherTriangle.getBarycenter().getX(), otherTriangle.getBarycenter().getY());
                                    l2.setStroke(Color.RED);
                                    l2.setStrokeWidth(2);
                                    pane.getChildren().addAll(l1, l2);
                                }
                                checkedEdges.add(de1);
                            }
                        }
                    }

                    /*// randomised walkthrough test
                    SimplyConnectedTree tree = new SimplyConnectedTree((ArrayList<DTriangle>) includedTriangles);
                    //tree.printAdjacencyMatrix();
                    tree.getRandomTraversal(tree.getLeaf());
                    tree.getRandomTraversal(tree.getLeaf());
                    tree.getRandomTraversal(tree.getLeaf());
                    tree.getRandomTraversal(tree.getLeaf());*/
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(triangulationButton);

        Slider slider = new Slider(0, 150, 100);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(40);
        slider.setMaxWidth(180);
        menu.getChildren().add(slider);

        slider.valueProperty().addListener((ov, oldValue, newValue) -> {
            Controller.getSimulation().setTimeStep((int) (double) newValue);
        });

        pursuers = new ArrayList<>();
        evaders = new ArrayList<>();

        Scene scene = new Scene(outerLayout, 1200, 800);
        primaryStage.setTitle("Coded by Winston v5.76.002 build 42 alpha");

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/rrr_icon.png")));
        //pane.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/like-3.png"))));

        primaryStage.setScene(scene);
        primaryStage.show();

        /*Polygon arcTestPolygon = new Polygon();
        arcTestPolygon.getPoints().addAll(
                20.0, 30.0,
                30.0, 90.0,
                200.0, 150.0,
                200.0, 80.0
        );
        arcTestPolygon.setFill(Color.LIGHTGREEN);
        pane.getChildren().add(arcTestPolygon);

        Arc arc = new Arc();
        arc.setCenterX(100.0f);
        arc.setCenterY(80.0f);
        arc.setRadiusX(100.0f);
        arc.setRadiusY(100.0f);
        arc.setStartAngle(0.0f);
        arc.setLength(60.0f);
        arc.setType(ArcType.ROUND);
        arc.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.3));
        arc.setStroke(Color.BLACK);
        arc.setStrokeWidth(1);
        pane.getChildren().add(arc);

        Polygon testPolygon = new Polygon();
        testPolygon.getPoints().addAll(
                20.0, 30.0,
                0.0, 0.0,
                0.0, pane.getHeight(),
                pane.getWidth(), pane.getHeight(),
                pane.getWidth(), 0.0,
                0.0, 0.0,
                20.0, 30.0,
                200.0, 80.0,
                200.0, 150.0,
                30.0, 90.0
        );
        testPolygon.setFill(Color.WHITE);
        //pane.getChildren().add(testPolygon);

        Button arcTestButton = new Button("Arc test");
        arcTestButton.setOnAction(e -> {
            arc.setStartAngle(arc.getStartAngle() + 30.0f);
        });
        menu.getChildren().add(arcTestButton);

        VisualAgent visualAgent = new VisualAgent();
        visualAgent.setTranslateX(300);
        visualAgent.setTranslateY(300);
        pane.getChildren().add(visualAgent);

        Timeline timeLine = new Timeline();
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.setAutoReverse(true);
        KeyValue kv = new KeyValue(arc.startAngleProperty(), 360.0f);
        KeyFrame kf = new KeyFrame(Duration.millis(4000), kv);
        timeLine.getKeyFrames().add(kf);
        timeLine.play();

        Timeline timeLine2 = new Timeline();
        timeLine2.setCycleCount(Timeline.INDEFINITE);
        timeLine2.setAutoReverse(true);
        KeyValue kv2 = new KeyValue(visualAgent.turnAngleProperty(), 360.0);
        KeyFrame kf2 = new KeyFrame(Duration.millis(2000), kv2);
        timeLine2.getKeyFrames().add(kf2);
        timeLine2.play();

        union = new Rectangle(250, 250, 100, 100);
        //union = Shape.union(union, visualAgent.getFieldOfView());
        union.setFill(Color.GREEN);
        union.toFront();

        System.out.println(union.getBoundsInParent().getMinX() + " " + union.getBoundsInParent().getMaxX() + " - " + union.getBoundsInParent().getMinY() + " " + union.getBoundsInParent().getMaxY());
        pane.getChildren().add(union);

        Button unionTestButton = new Button("Union test");
        unionTestButton.setOnAction(e -> {
            pane.getChildren().remove(union);
            changeUnion(union, visualAgent.getFieldOfView());
            pane.getChildren().add(union);
            //union.toBack();
            union.setFill(Color.BLACK);
            System.out.println("Hello");
        });
        menu.getChildren().add(unionTestButton);*/

    }

    private void initPlaceAgents() {
        if (currentState == ProgramState.MAP_EDITING && mapPolygons.size() > 0 && mapPolygons.get(0).isClosed()) {
            currentState = ProgramState.AGENT_PLACING;
            for (int i = 0; i < pane.getChildren().size(); i++) {
                if (pane.getChildren().get(i) instanceof Anchor) {
                    pane.getChildren().remove(i);
                    i--;
                }
            }

            Shape outerCover = new Rectangle(0, 0, 1920, 1080);
            outerCover = Shape.subtract(outerCover, mapPolygons.get(0));
            outerCover.setFill(Color.LIGHTGREY);
            pane.getChildren().add(outerCover);

            covers.add(outerCover);
            for (int i = 1; i < mapPolygons.size(); i++) {
                mapPolygons.get(i).setFill(Color.LIGHTGREY);
                covers.add(mapPolygons.get(i));
            }
        }
    }

    private void clearMap() {
        currentState = ProgramState.MAP_EDITING;
        pane.getChildren().clear();
        pane.getChildren().add(indicatorLine);
        mapPolygons.clear();
        currentMapPolygon = new MapPolygon(pane);
        mapPolygons.add(currentMapPolygon);
        pane.getChildren().add(currentMapPolygon);
        MapPolygon.getAllAnchors().clear();
        visualAgents.clear();
        pursuers.clear();
        evaders.clear();
        Controller.setSimulation(null);
    }

    private void saveMapOnly() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the current map");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Map data only file", "*.mdo"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            // write map to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(selectedFile))) {
                for (int i = 0; i < mapPolygons.size() - 1; i++) {
                    for (int j = 0; j < mapPolygons.get(i).getPoints().size(); j++) {
                        out.print(mapPolygons.get(i).getPoints().get(j) + " ");
                    }
                    out.println();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void saveMapAndAgents() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the current map");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map and agent data file", "*.maa"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(selectedFile))) {
                // write map to file
                out.println("map");
                for (int i = 0; i < mapPolygons.size() - 1; i++) {
                    for (int j = 0; j < mapPolygons.get(i).getPoints().size(); j++) {
                        out.print(mapPolygons.get(i).getPoints().get(j) + " ");
                    }
                    out.println();
                }
                out.println("agents");
                AgentSettings s;
                for (VisualAgent a : visualAgents) {
                    s = a.getSettings();
                    out.print(s.getXPos() + " " + s.getYPos() + " ");
                    out.print(s.getSpeed() + " " + s.getTurnSpeed() + " ");
                    out.print(s.getTurnAngle() + " " + s.getFieldOfViewAngle() + " ");
                    out.print(s.getFieldOfViewRange() + " ");
                    out.print(s.isPursuing() + " ");
                    out.println(s.getMovePolicy());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void loadMap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load a map");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            clearMap();
            try (BufferedReader in = new BufferedReader(new FileReader(selectedFile))) {
                // read in the map and file
                String line = in.readLine();
                if (line.contains("map")) {
                    //currentState = ProgramState.AGENT_PLACING;
                    // map data
                    System.out.println(line);
                    String[] coords;
                    double[] coordsDouble;
                    while ((line = in.readLine()) != null && !line.contains("agents")) {
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coords.length; i += 2) {
                            // adding the polygons
                            Anchor a = null;
                            boolean connectedToOld = false;

                            for (Anchor oldAnchor : MapPolygon.getAllAnchors()) {
                                if (oldAnchor.getCenterX() == coordsDouble[i] && oldAnchor.getCenterY() == coordsDouble[i + 1]) {
                                    a = oldAnchor;
                                    connectedToOld = true;
                                    break;
                                }
                            }

                            if (!connectedToOld) {
                                DoubleProperty xProperty = new SimpleDoubleProperty(coordsDouble[i]);
                                DoubleProperty yProperty = new SimpleDoubleProperty(coordsDouble[i + 1]);
                                a = new Anchor(Color.GOLD, xProperty, yProperty);
                            }

                            currentMapPolygon.addAnchor(a);

                            if (connectedToOld && a.getCenterX() == currentMapPolygon.getPoints().get(0) && a.getCenterY() == currentMapPolygon.getPoints().get(1)) {
                                // connected to first point to close the polygon
                                StrokeTransition st = new StrokeTransition(new Duration(100), currentMapPolygon, Color.BLUE, Color.ORANGE);
                                st.play();
                                currentMapPolygon = new MapPolygon(pane);
                                mapPolygons.add(currentMapPolygon);
                                pane.getChildren().add(currentMapPolygon);
                            }
                        }
                    }
                    initPlaceAgents();
                    // read agent data
                    String[] settings;
                    while ((line = in.readLine()) != null) {
                        settings = line.split(" ");
                        if (settings.length != 9) {
                            System.out.println("Failed to load agents.");
                            return;
                        }
                        AgentSettings agentSettings = new AgentSettings();
                        agentSettings.setXPos(Double.parseDouble(settings[0]));
                        agentSettings.setYPos(Double.parseDouble(settings[1]));
                        agentSettings.setSpeed(Double.parseDouble(settings[2]));
                        agentSettings.setTurnSpeed(Double.parseDouble(settings[3]));
                        agentSettings.setTurnAngle(Double.parseDouble(settings[4]));
                        agentSettings.setFieldOfViewAngle(Double.parseDouble(settings[5]));
                        agentSettings.setFieldOfViewRange(Double.parseDouble(settings[6]));
                        agentSettings.setPursuing(Boolean.parseBoolean(settings[7]));
                        agentSettings.setMovePolicy(settings[8]);

                        VisualAgent visualAgent = new VisualAgent();
                        visualAgent.adoptSettings(agentSettings);
                        visualAgents.add(visualAgent);
                        //System.out.println("Pursuer loaded: " + visualAgent.getSettings().getXPos() + " " + visualAgent.getSettings().getYPos());
                        pane.getChildren().add(visualAgent);
                    }
                    for (MapPolygon p : mapPolygons) {
                        p.toFront();
                    }
                    for (Shape c : covers) {
                        c.toFront();
                    }
                } else {
                    currentState = ProgramState.MAP_EDITING;
                    // map data only
                    String[] coords;
                    double[] coordsDouble;
                    do {
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coords.length; i += 2) {
                            // adding the polygons
                            Anchor a = null;
                            boolean connectedToOld = false;

                            for (Anchor oldAnchor : MapPolygon.getAllAnchors()) {
                                if (oldAnchor.getCenterX() == coordsDouble[i] && oldAnchor.getCenterY() == coordsDouble[i + 1]) {
                                    a = oldAnchor;
                                    connectedToOld = true;
                                    break;
                                }
                            }

                            if (!connectedToOld) {
                                DoubleProperty xProperty = new SimpleDoubleProperty(coordsDouble[i]);
                                DoubleProperty yProperty = new SimpleDoubleProperty(coordsDouble[i + 1]);
                                a = new Anchor(Color.GOLD, xProperty, yProperty);
                            }

                            currentMapPolygon.addAnchor(a);

                            if (connectedToOld && a.getCenterX() == currentMapPolygon.getPoints().get(0) && a.getCenterY() == currentMapPolygon.getPoints().get(1)) {
                                // connected to first point to close the polygon
                                StrokeTransition st = new StrokeTransition(new Duration(100), currentMapPolygon, Color.BLUE, Color.ORANGE);
                                st.play();
                                currentMapPolygon = new MapPolygon(pane);
                                mapPolygons.add(currentMapPolygon);
                                pane.getChildren().add(currentMapPolygon);
                            }
                        }
                    } while ((line = in.readLine()) != null);

                    indicatorLine.setVisible(false);
                }
                /*for (MapPolygon m : mapPolygons) {
                    System.out.println("\nPolygon: ");
                    for (int i = 0; i < m.getPoints().size(); i += 2) {
                        System.out.println(m.getPoints().get(i) + " " + m.getPoints().get(i + 1));
                    }
                }*/
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addListeners() {
        PaneEvents paneEvents = new PaneEvents(pane);
        outerLayout.addEventFilter(MouseEvent.MOUSE_PRESSED, paneEvents.getOnMousePressedEventHandler());
        outerLayout.addEventFilter(MouseEvent.MOUSE_DRAGGED, paneEvents.getOnMouseDraggedEventHandler());
        outerLayout.addEventFilter(ScrollEvent.ANY, paneEvents.getOnScrollEventHandler());

        pane.widthProperty().addListener((ov, oldValue, newValue) -> {
            if ((double) oldValue != (double) newValue) {
                for (Anchor anchor : MapPolygon.getAllAnchors()) {
                    if (anchor.getCenterX() > (double) newValue) {
                        anchor.setCenterX((double) newValue);
                    }
                }
            }
        });
        pane.heightProperty().addListener((ov, oldValue, newValue) -> {
            if ((double) oldValue != (double) newValue) {
                for (Anchor anchor : MapPolygon.getAllAnchors()) {
                    if (anchor.getCenterY() > (double) newValue) {
                        anchor.setCenterY((double) newValue);
                    }
                }
            }
        });
        pane.setOnMousePressed(e -> {
            if (currentState == ProgramState.SIMULATION) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                if (currentState == ProgramState.MAP_EDITING && !e.isControlDown() && addPoints.get()) {
                    if (!e.isPrimaryButtonDown() || (mapPolygons.size() > 1 && !mapPolygons.get(0).contains(e.getX(), e.getY()))) {
                        return;
                    }

                    Anchor a = null;
                    boolean connectedToOld = false;

                    for (Anchor oldAnchor : MapPolygon.getAllAnchors()) {
                        if (Math.pow(oldAnchor.getCenterX() - e.getX(), 2) + Math.pow(oldAnchor.getCenterY() - e.getY(), 2) < Math.pow(oldAnchor.getRadius(), 2)) {
                            a = oldAnchor;
                            connectedToOld = true;
                            if (!indicatorLine.isVisible()) {
                                return;
                            }
                            break;
                        }
                    }

                    if (!connectedToOld) {
                        if (indicatorLine.isVisible()) {
                            for (MapPolygon mp : mapPolygons) {
                                if (mp.lineIntersects(indicatorLine)) {
                                    return;
                                }
                            }
                        } else {
                            for (int i = 1; i < mapPolygons.size(); i++) {
                                if (mapPolygons.get(i).contains(e.getX(), e.getY())) {
                                    return;
                                }
                            }
                        }
                        DoubleProperty xProperty = new SimpleDoubleProperty(e.getX());
                        DoubleProperty yProperty = new SimpleDoubleProperty(e.getY());
                        a = new Anchor(Color.GOLD, xProperty, yProperty);
                    } else if (a.getCenterX() == currentMapPolygon.getPoints().get(0) && a.getCenterY() == currentMapPolygon.getPoints().get(1)) {
                        for (int i = 1; i < mapPolygons.size() - 1; i++) {
                            if (mapPolygons.get(i) != currentMapPolygon && currentMapPolygon.contains(mapPolygons.get(i).getPoints().get(0), mapPolygons.get(i).getPoints().get(1))) {
                                return;
                            }
                        }
                    }

                    currentMapPolygon.addAnchor(a);

                    if (!indicatorLine.isVisible()) {
                        indicatorLine.setStartX(e.getX());
                        indicatorLine.setStartY(e.getY());
                        indicatorLine.setEndX(e.getX());
                        indicatorLine.setEndY(e.getY());
                        indicatorLine.setVisible(true);
                    } else {
                        if (connectedToOld && a.getCenterX() == currentMapPolygon.getPoints().get(0) && a.getCenterY() == currentMapPolygon.getPoints().get(1)) {
                            for (int i = 1; i < mapPolygons.size(); i++) {
                                if (mapPolygons.get(i) != currentMapPolygon && currentMapPolygon.contains(mapPolygons.get(i).getPoints().get(0), mapPolygons.get(i).getPoints().get(1))) {
                                    return;
                                }
                            }

                            System.out.println("Polygon created (with " + ((currentMapPolygon.getPoints().size() / 2) - 1) + " points)");
                            System.out.println(mapPolygons.size());

                            // connected to first point to close the polygon
                            Polygon robinsPolygon = currentMapPolygon.getPolygon();

                            StrokeTransition st = new StrokeTransition(new Duration(500), currentMapPolygon, Color.BLUE, Color.ORANGE);
                            st.play();
                            currentMapPolygon = new MapPolygon(pane);
                            pane.getChildren().add(currentMapPolygon);
                            mapPolygons.add(currentMapPolygon);
                            indicatorLine.setVisible(false);
                        } else {
                            // connected to other anchor or new anchor
                            indicatorLine.setStartX(e.getX());
                            indicatorLine.setStartY(e.getY());
                            indicatorLine.setEndX(e.getX());
                            indicatorLine.setEndY(e.getY());
                        }
                    }
                } else if (currentState == ProgramState.AGENT_PLACING) {
                    if (mapPolygons.get(0).isClosed()) {
                        boolean placedInHole = false;
                        for (int i = 1; i < mapPolygons.size(); i++) {
                            if (mapPolygons.get(i).contains(e.getX(), e.getY())) {
                                placedInHole = true;
                            }
                        }
                        if (!placedInHole && e.isPrimaryButtonDown() && mapPolygons.get(0).contains(e.getX(), e.getY())) {
                            VisualAgent visualAgent = new VisualAgent(e.getX(), e.getY());
                            if (visualAgents == null) {
                                visualAgents = new ArrayList<>();
                            }
                            visualAgents.add(visualAgent);
                            pane.getChildren().add(visualAgent);

                            // covering areas beyond the agent's vision
                            for (Shape s : covers) {
                                s.toFront();
                            }
                            mapPolygons.get(0).toFront();
                        }
                    }
                }
            } else if (e.getButton() == MouseButton.SECONDARY) {
                /*
                TODO:
                Constrain textboxes (only doubles)
                Better model..
                Do not create all this over and over again
                 */

                if (visualAgents == null) {
                    return;
                }

                /*for (IntegerProperty i = new SimpleIntegerProperty(0); i.get() < visualAgents.size(); i.set(i.get() + 1)) {

                }*/

                for (int i = 0; i < visualAgents.size(); i++) {
                    VisualAgent va = visualAgents.get(i);
                    Circle c = va.getAgentBody();
                    if (c.contains(e.getX(), e.getY())) {
                        //contains agent
                        ContextMenu contextMenu = new ContextMenu();
                        MenuItem editItem = new MenuItem("Edit");
                        MenuItem deleteItem = new MenuItem("Delete");
                        contextMenu.getItems().addAll(editItem, deleteItem);

                        editItem.setOnAction(ae -> {
                            Dialog<AgentSettings> dialog = new Dialog<>();
                            dialog.setTitle("Edit agent settings");
                            ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image(this.getClass().getResource("/rrr_icon.png").toString()));

                            ButtonType applyType = new ButtonType("Set", ButtonBar.ButtonData.APPLY);
                            dialog.getDialogPane().getButtonTypes().addAll(applyType, ButtonType.CANCEL);

                            GridPane grid = new GridPane();
                            grid.setHgap(10);
                            grid.setVgap(10);
                            grid.setPadding(new Insets(10, 10, 10, 10));

                            TextField xpos = new TextField();
                            xpos.setText(String.valueOf(va.getSettings().getXPos()));
                            TextField ypos = new TextField();
                            ypos.setText(String.valueOf(va.getSettings().getYPos()));
                            TextField speed = new TextField();
                            speed.setText(String.valueOf(va.getSettings().getSpeed()));
                            TextField turnSpeed = new TextField();
                            turnSpeed.setText(String.valueOf(va.getSettings().getTurnSpeed()));
                            TextField fovAngle = new TextField();
                            fovAngle.setText(String.valueOf(va.getSettings().getFieldOfViewAngle()));
                            TextField fovRange = new TextField();
                            fovRange.setText(String.valueOf(va.getSettings().getFieldOfViewRange()));
                            ComboBox<String> agentType = new ComboBox<>();
                            agentType.getItems().addAll("Pursuer", "Evader");
                            agentType.setValue(va.getSettings().isPursuing() ? "Pursuer" : "Evader");
                            ComboBox<String> agentPolicy = new ComboBox<>();
                            agentPolicy.getItems().addAll("Random policy", "Straight line policy", "Evader policy", "Dummy policy");
                            agentPolicy.setValue("Random policy");

                            grid.add(new Label("X:"), 0, 0);
                            grid.add(xpos, 1, 0);
                            grid.add(new Label("Y:"), 0, 1);
                            grid.add(ypos, 1, 1);
                            grid.add(new Label("Speed:"), 0, 2);
                            grid.add(speed, 1, 2);
                            grid.add(new Label("Turn Speed:"), 0, 3);
                            grid.add(turnSpeed, 1, 3);
                            grid.add(new Label("FOV Angle:"), 0, 4);
                            grid.add(fovAngle, 1, 4);
                            grid.add(new Label("FOV Range:"), 0, 5);
                            grid.add(fovRange, 1, 5);
                            grid.add(new Label("Type:"), 0, 6);
                            grid.add(agentType, 1, 6);
                            grid.add(new Label("Policy:"), 0, 7);
                            grid.add(agentPolicy, 1, 7);

                            Node applyButton = dialog.getDialogPane().lookupButton(applyType);

                            dialog.getDialogPane().setContent(grid);

                            Platform.runLater(() -> xpos.requestFocus());

                            dialog.setResultConverter(b -> {
                                if (b == applyType) {
                                    if (xpos.getText().isEmpty() || ypos.getText().isEmpty() || speed.getText().isEmpty() || turnSpeed.getText().isEmpty() ||
                                            fovAngle.getText().isEmpty() || fovRange.getText().isEmpty()) {
                                        return null;
                                    }
                                    AgentSettings s = new AgentSettings();
                                    s.setSpeed(Double.valueOf(speed.getText()));
                                    s.setTurnSpeed(Double.valueOf(turnSpeed.getText()));
                                    s.setXPos(Double.valueOf(xpos.getText()));
                                    s.setYPos(Double.valueOf(ypos.getText()));
                                    s.setFieldOfViewAngle(Double.valueOf(fovAngle.getText()));
                                    s.setFieldOfViewRange(Double.valueOf(fovRange.getText()));
                                    s.setPursuing(agentType.getValue().equals("Pursuer"));
                                    if (agentPolicy.getValue().equals("Random policy")) {
                                        s.setMovePolicy("random_policy");
                                    } else if (agentPolicy.getValue().equals("Straight line policy")) {
                                        s.setMovePolicy("straight_line_policy");
                                    } else if (agentPolicy.getValue().equals("Evader policy")) {
                                        s.setMovePolicy("evader_policy");
                                    } else if (agentPolicy.getValue().equals("Dummy policy")) {
                                        s.setMovePolicy("dummy_policy");
                                    }

                                    return s;
                                }
                                return null;
                            });

                            Optional<AgentSettings> result = dialog.showAndWait();

                            result.ifPresent(res -> {
                                va.adoptSettings(res);
                            });
                        });

                        deleteItem.setOnAction(ae -> {
                            pane.getChildren().remove(va);
                            visualAgents.remove(va);
                        });

                        contextMenu.show(c, e.getScreenX(), e.getScreenY());
                    }
                }
            }
        });
        pane.setOnMouseMoved(e -> {
            if (indicatorLine.isVisible()) {
                indicatorLine.setEndX(e.getX());
                indicatorLine.setEndY(e.getY());
                boolean intersection = false;
                for (MapPolygon mp : mapPolygons) {
                    if (mp.lineIntersects(indicatorLine)) {
                        indicatorLine.setStroke(Color.RED);
                        intersection = true;
                        break;
                    }
                }
                if (!intersection) {
                    indicatorLine.setStroke(Color.FORESTGREEN);
                }
            }
        });
        pane.setOnMouseDragged(e -> {
            if (indicatorLine.isVisible()) {
                indicatorLine.setEndX(e.getX());
                indicatorLine.setEndY(e.getY());
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
