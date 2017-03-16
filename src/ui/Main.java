package ui;

import control.Controller;
import conversion.GridConversion;
import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import simulation.RevealedMap;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

public class Main extends Application {

    enum ProgramState {
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
        addListeners();

        Button clearMapButton = new Button("Clear map");
        clearMapButton.setOnAction(e -> clearMap());
        menu.getChildren().add(clearMapButton);

        Button placeAgentsButton = new Button("Start placing agents");
        placeAgentsButton.setOnAction(e -> {
            if (currentState == ProgramState.MAP_EDITING && mapPolygons.size() > 0 && mapPolygons.get(0).isClosed()) {
                currentState = ProgramState.AGENT_PLACING;
                for (int i = 0; i < pane.getChildren().size(); i++) {
                    if (pane.getChildren().get(i) instanceof Anchor) {
                        pane.getChildren().remove(i);
                        i--;
                    }
                }

                Shape outerCover = new Rectangle(0, 0, pane.getWidth(), pane.getHeight());
                outerCover = Shape.subtract(outerCover, mapPolygons.get(0));
                outerCover.setFill(Color.WHITE);
                pane.getChildren().add(outerCover);

                covers.add(outerCover);
                for (int i = 1; i < mapPolygons.size(); i++) {
                    mapPolygons.get(i).setFill(Color.WHITE);
                    covers.add(mapPolygons.get(i));
                }
            }
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
        menu.getChildren().add(theBestTestButton);
        Button simulationButton = new Button("Better simulation");
        simulationButton.setOnAction(e -> {
            Controller.betterTest(mapPolygons, pursuers, evaders);
        });
        Button convertButton = new Button("Print Grid");
        convertButton.setOnAction(e -> {
            GridConversion.convert(mapPolygons, pursuers, evaders, CELL_SIZE);
        });
        menu.getChildren().add(convertButton);
        addPoints = new SimpleBooleanProperty(false);
        CheckBox b = new CheckBox("To draw or\nnot to draw");
        addPoints.bind(b.selectedProperty());
        menu.getChildren().add(b);

        pursuers = new ArrayList<>();
        evaders = new ArrayList<>();

        Scene scene = new Scene(outerLayout, 1200, 800);
        primaryStage.setTitle("Coded by Winston v5.76.002 build 41 alpha");

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

    private Shape polygonWithHoles;
    private Shape union;

    private void changePolygonWithHoles(Shape newSubtraction) {
        polygonWithHoles = Shape.subtract(polygonWithHoles, newSubtraction);
    }

    private void changeUnion(Shape prevUnion, Shape newAddition) {
        union = Shape.union(prevUnion, newAddition);
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
            if (currentState == ProgramState.MAP_EDITING) {
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

    private void clearMap() {
        pane.getChildren().clear();
        pane.getChildren().add(indicatorLine);
        mapPolygons.clear();
        currentMapPolygon = new MapPolygon(pane);
        pane.getChildren().add(currentMapPolygon);
        MapPolygon.getAllAnchors().clear();
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
                for (VisualAgent a : visualAgents) {
                    out.println(a.getCenterX() + " " + a.getCenterY() + " " + a.getFieldOfViewAngle() + " " + a.getFieldOfViewRange());
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
                    currentState = ProgramState.AGENT_PLACING;
                    // map and agent data
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
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
                                mapPolygons.add(currentMapPolygon);
                                StrokeTransition st = new StrokeTransition(new Duration(100), currentMapPolygon, Color.BLUE, Color.ORANGE);
                                st.play();
                                currentMapPolygon = new MapPolygon(pane);
                                pane.getChildren().add(currentMapPolygon);
                            }
                        }
                    } while ((line = in.readLine()) != null);

                    currentMapPolygon = new MapPolygon(pane);
                    pane.getChildren().add(currentMapPolygon);
                    mapPolygons.add(currentMapPolygon);
                    indicatorLine.setVisible(false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
