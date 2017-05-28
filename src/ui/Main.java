package ui;

import control.Controller;
import conversion.GridConversion;
import entities.*;
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
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import pathfinding.ShortestPathRoadMap;
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
    private VBox entityMenu;
    public static ZoomablePane pane;

    private Line indicatorLine;
    public static ArrayList<MapPolygon> mapPolygons;
    private MapPolygon currentMapPolygon;

    private ArrayList<Shape> covers;

    private ArrayList<Circle> pursuers;
    private ArrayList<Circle> evaders;
    private ArrayList<VisualAgent> visualAgents;
    private ArrayList<RadioButton> entitiesList;

    private BooleanProperty addPoints;

    private ProgramState currentState;

    // ************************************************************************************************************** //
    // Test stuff for entities
    // ************************************************************************************************************** //
    private Entity testEntity;
    private BooleanProperty useEntities = new SimpleBooleanProperty(false);
    private ArrayList<Entity> evadingEntities = new ArrayList<>();
    private MapRepresentation map;
    private AdaptedSimulation adaptedSimulation;
    private Entity activeEntity;
    private DCREntity testDCREntity;
    // ************************************************************************************************************** //
    // Test stuff for entities
    // ************************************************************************************************************** //

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ****************************************************************************************************** //
        // Setup of all the UI elements and internal data structures
        // ****************************************************************************************************** //

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Overwrite current map?");
        alert.setHeaderText("Loading a saved map will overwrite the one you are editing right now.");
        alert.setContentText("Are you ok with this?");

        stage = primaryStage;

        covers = new ArrayList<>();
        currentState = ProgramState.MAP_EDITING;

        // zoomable drawing pane
        pane = new ZoomablePane();

        // top-level container, partitions window into drawing pane and menu
        outerLayout = new HBox();
        outerLayout.setPrefSize(1400, 800);

        // entity menu
        entityMenu = new VBox();
        entityMenu.setStyle("-fx-background-color: #ffffff");
        entityMenu.setMinWidth(190);
        entityMenu.setPrefSize(190, 600);
        entityMenu.setMaxWidth(190);

        // sidebar menu, currently with dummy buttons
        menu = new VBox();
        menu.setStyle("-fx-background-color: #ffffff");
        menu.setMinWidth(190);
        menu.setPrefSize(190, 600);
        menu.setMaxWidth(190);

        // zoomable drawing pane
        pane = new ZoomablePane();

        // separator between menus
        Separator menuSeparator = new Separator(Orientation.VERTICAL);
        menuSeparator.setStyle("-fx-background-color: #ffffff");
        menuSeparator.setMinWidth(10);
        menuSeparator.setPrefWidth(10);
        menuSeparator.setMaxWidth(10);

        // separator between pane and menu
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setStyle("-fx-background-color: #ffffff");
        separator.setMinWidth(10);
        separator.setPrefWidth(10);
        separator.setMaxWidth(10);

        // adding elements to the top-level container
        outerLayout.getChildren().addAll(pane, separator, entityMenu, menuSeparator, menu);
        menu.toFront();
        HBox.setHgrow(pane, Priority.ALWAYS);
        HBox.setHgrow(entityMenu, Priority.NEVER);
        HBox.setHgrow(menu, Priority.NEVER);

        // line indicating where a line will be drawn when clicked
        indicatorLine = new Line();
        indicatorLine.setVisible(false);
        indicatorLine.setStroke(Color.FORESTGREEN);
        indicatorLine.setStrokeWidth(3.0);
        indicatorLine.setStrokeLineCap(StrokeLineCap.ROUND);
        pane.getChildren().add(indicatorLine);

        currentMapPolygon = new MapPolygon(pane);
        currentMapPolygon.setPickOnBounds(false);
        pane.getChildren().add(currentMapPolygon);
        mapPolygons = new ArrayList<>();
        mapPolygons.add(currentMapPolygon);
        visualAgents = new ArrayList<>();
        addListeners();

        pursuers = new ArrayList<>();
        evaders = new ArrayList<>();

        // entity menu stuff
        Label entityLabel = new Label("Entity menu");
        entityLabel.setFont(Font.font("Arial", 14));

        Separator entityLabelSeparator = new Separator(Orientation.HORIZONTAL);
        entityLabelSeparator.setStyle("-fx-background-color: #ffffff");
        entityLabelSeparator.setMinHeight(10);
        entityLabelSeparator.setPrefHeight(10);
        entityLabelSeparator.setMaxHeight(10);

        entityMenu.getChildren().addAll(entityLabel, entityLabelSeparator);

        ComboBox<String> entities = new ComboBox<>();
        entities.getItems().addAll("Random entity", "Straight line entity", "Flocking evader entity", "Hide evader entity", "Dummy entity");
        entities.setValue("Random entity");

        Button addEntity = new Button("Add");

        entityMenu.getChildren().addAll(entities, addEntity);

        Label entityLabel2 = new Label("Entities in use");
        entityLabel2.setFont(Font.font("Arial", 14));

        Separator entityLabel2Separator = new Separator(Orientation.HORIZONTAL);
        entityLabel2Separator.setStyle("-fx-background-color: #ffffff");
        entityLabel2Separator.setMinHeight(10);
        entityLabel2Separator.setPrefHeight(10);
        entityLabel2Separator.setMaxHeight(10);

        entityMenu.getChildren().addAll(entityLabel2, entityLabel2Separator);

        entitiesList = new ArrayList<>();

        ToggleGroup toggleGroup = new ToggleGroup();

        toggleGroup.selectedToggleProperty().addListener((ov, old_toggle, new_toggle) -> {

            if (old_toggle != null) {
                RadioButton old = (RadioButton) old_toggle;
                old.setStyle("-fx-text-fill: #e74c3c");
            }

            if (toggleGroup.getSelectedToggle() != null) {
                RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                selected.setStyle("-fx-text-fill: #2ecc71");
            }

        });

        addEntity.setOnAction(ae -> {
            boolean flag = false;
            RadioButton entButton = new RadioButton(entities.getValue());
            entButton.setStyle("-fx-text-fill: #2ecc71");

            for (RadioButton entButton2 : entitiesList) {
                if (entButton.getText().equals(entButton2.getText())) {
                    System.out.println("Entity already present!");
                    flag = true;
                }
            }

            if (!flag) {
                entButton.setToggleGroup(toggleGroup);
                entButton.setSelected(true);
                entitiesList.add(entButton);
                entityMenu.getChildren().add(entButton);
            }
        });

        // ****************************************************************************************************** //
        // The old/normal controls for the simulation; still using the old project structure
        // ****************************************************************************************************** //

        Label menuLabel1 = new Label("Old controls");
        menuLabel1.setFont(Font.font("Arial", 14));

        Separator menuSeparator1 = new Separator(Orientation.HORIZONTAL);
        menuSeparator1.setStyle("-fx-background-color: #ffffff");
        menuSeparator1.setMinHeight(10);
        menuSeparator1.setPrefHeight(10);
        menuSeparator1.setMaxHeight(10);
        menu.getChildren().addAll(menuLabel1, menuSeparator1);

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

        Slider slider = new Slider(0, 150, 100);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(40);
        slider.setMaxWidth(180);
        menu.getChildren().add(slider);

        slider.valueProperty().addListener((ov, oldValue, newValue) -> {
            //System.out.println("val = " + newValue);
            if (!useEntities.getValue()) {
                Controller.getSimulation().setTimeStep((int) (double) newValue);
            } else {
                adaptedSimulation.setTimeStep((int) (double) newValue);
            }
        });

        // ****************************************************************************************************** //
        // The ten million debugging buttons for dividing the environment for the randomised algorithm
        // ****************************************************************************************************** //

        Separator menuSeparator2 = new Separator(Orientation.HORIZONTAL);
        menuSeparator2.setStyle("-fx-background-color: #ffffff");
        menuSeparator2.setMinHeight(10);
        menuSeparator2.setPrefHeight(10);
        menuSeparator2.setMaxHeight(10);

        Label menuLabel2 = new Label("Old debugging stuff");
        menuLabel2.setFont(Font.font("Arial", 14));

        Separator menuSeparator3 = new Separator(Orientation.HORIZONTAL);
        menuSeparator3.setStyle("-fx-background-color: #ffffff");
        menuSeparator3.setMinHeight(10);
        menuSeparator3.setPrefHeight(10);
        menuSeparator3.setMaxHeight(10);
        menu.getChildren().addAll(menuSeparator2, menuLabel2, menuSeparator3);

        CheckBox useEntitiesCheckBox = new CheckBox("Use entities");
        useEntitiesCheckBox.selectedProperty().bindBidirectional(useEntities);
        menu.getChildren().add(useEntitiesCheckBox);

        Button prepareEntityTestButton = new Button("Prepare entity test");
        prepareEntityTestButton.setOnAction(e -> {
            map = new MapRepresentation(mapPolygons, null, evadingEntities);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(prepareEntityTestButton);

        Button runEntityTestButton = new Button("Run entity test");
        runEntityTestButton.setOnAction(e -> {
            if (evadingEntities.size() == 0) {
                System.exit(-5);
            }
            adaptedSimulation = new AdaptedSimulation(map);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(runEntityTestButton);

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
                        for (int i = 1; inPolygon && i < mapPolygons.size(); i++) {
                            if (mapPolygons.get(i).contains(centerX, centerY)) {
                                inPolygon = false;
                            }
                        }
                        if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                            inPolygon = false;
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
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(triangulationButton);

        Button simpleComponentButton = new Button("Show simply connected\ncomponents");
        simpleComponentButton.setOnAction(e -> {
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
                        if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                            inPolygon = false;
                        }
                        if (inPolygon) {
                            includedTriangles.add(dt);
                        }
                    }

                    ArrayList<ArrayList<DTriangle>> nodes = new ArrayList<>(includedTriangles.size());
                    ArrayList<DTriangle> temp;
                    for (DTriangle dt : includedTriangles) {
                        temp = new ArrayList<>();
                        temp.add(dt);
                        nodes.add(temp);
                    }
                    int[][] adjacencyMatrix = new int[nodes.size()][nodes.size()];

                    // checking for adjacency between nodes
                    ArrayList<DEdge> checkedEdges = new ArrayList<>();
                    DTriangle dt1, dt2;
                    DEdge de;
                    for (int i = 0; i < includedTriangles.size(); i++) {
                        dt1 = includedTriangles.get(i);
                        // go through the edges of each triangle
                        for (int j = 0; j < 3; j++) {
                            de = dt1.getEdge(j);
                            if (!checkedEdges.contains(de)) {
                                int neighbourIndex = -1;
                                for (int k = 0; neighbourIndex == -1 && k < includedTriangles.size(); k++) {
                                    dt2 = includedTriangles.get(k);
                                    if (k != i && dt2.isEdgeOf(de)) {
                                        // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                                        neighbourIndex = k;
                                    }
                                }
                                if (neighbourIndex != -1) {
                                    adjacencyMatrix[i][neighbourIndex] = 1;
                                    adjacencyMatrix[neighbourIndex][i] = 1;
                                }
                                checkedEdges.add(de);
                            }
                        }
                    }
                    // loop above fills the adjacency matrix

                    // 1. delete all vertices of degree 1
                    boolean degreeOneRemaining = true;
                    while (degreeOneRemaining) {
                        degreeOneRemaining = false;
                        for (int i = 0; i < nodes.size(); i++) {
                            int adjCount = 0;
                            int neighbourIndex = -1;
                            for (int j = 0; j < nodes.size(); j++) {
                                if (adjacencyMatrix[i][j] == 1) {
                                    adjCount++;
                                    neighbourIndex = j;
                                }
                            }
                            if (adjCount == 1) {
                                System.out.println("Delete " + i + " and add to " + neighbourIndex);
                                // add triangle to neighbour which is not deleted
                                nodes.get(neighbourIndex).addAll(nodes.get(i));
                                nodes.get(i).clear();
                                // "delete" this vertex
                                for (int j = 0; j < nodes.size(); j++) {
                                    adjacencyMatrix[i][j] = -1;
                                    adjacencyMatrix[j][i] = -1;
                                }
                                degreeOneRemaining = true;
                            }
                        }
                    }

                    int[] degreeMatrix = new int[nodes.size()];
                    int degreeCount;
                    for (int i = 0; i < nodes.size(); i++) {
                        degreeCount = 0;
                        for (int j = 0; j < nodes.size(); j++) {
                            if (adjacencyMatrix[i][j] == 1) {
                                degreeCount++;
                            }
                        }
                        degreeMatrix[i] = degreeCount;
                    }

                    // 2. merge all vertices of degree 2
                    boolean degreeTwoRemaining = true;
                    while (degreeTwoRemaining) {
                        degreeTwoRemaining = false;
                        for (int i = 0; i < nodes.size(); i++) {
                            if (degreeMatrix[i] == 3) {
                                continue;
                            }
                            int adjCount = 0;
                            int mergeNeighbourIndex = -1;
                            int otherNeighbourIndex = -1;
                            for (int j = 0; j < nodes.size(); j++) {
                                if (adjacencyMatrix[i][j] == 1) {
                                    adjCount++;
                                    if (adjCount == 1) {
                                        mergeNeighbourIndex = j;
                                    } else {
                                        otherNeighbourIndex = j;
                                    }
                                }
                            }
                            if (mergeNeighbourIndex != -1 && otherNeighbourIndex != -1) {
                                if (degreeMatrix[mergeNeighbourIndex] > 2 && degreeMatrix[otherNeighbourIndex] > 2) {
                                    continue;
                                } else if (degreeMatrix[mergeNeighbourIndex] > 2) {
                                    int store = mergeNeighbourIndex;
                                    mergeNeighbourIndex = otherNeighbourIndex;
                                    otherNeighbourIndex = store;
                                }
                            }
                            if (adjCount == 2 && degreeMatrix[mergeNeighbourIndex] == 2) {
                                System.out.println("Merge " + i + " into " + mergeNeighbourIndex);
                                // add triangles to neighbour which is not deleted
                                nodes.get(mergeNeighbourIndex).addAll(nodes.get(i));
                                nodes.get(i).clear();
                                // connect the merged and the other adjacent vertex
                                adjacencyMatrix[mergeNeighbourIndex][otherNeighbourIndex] = 1;
                                adjacencyMatrix[otherNeighbourIndex][mergeNeighbourIndex] = 1;
                                // "delete" this vertex
                                for (int j = 0; j < nodes.size(); j++) {
                                    adjacencyMatrix[i][j] = -1;
                                    adjacencyMatrix[j][i] = -1;
                                }
                                //degreeTwoRemaining = true;
                            }
                        }
                        /*for (int j = 0; j < nodes.size(); j++) {
                            int adjTwoCounter = 0;
                            for (int k = 0; k < nodes.size(); k++) {
                                if (adjacencyMatrix[j][k] == 1) {
                                    adjTwoCounter++;
                                }
                            }
                            if (adjTwoCounter == 2 ) {
                                degreeTwoRemaining = true;
                                break;
                            }
                        }
                        System.out.println("\nCheck");
                        for (int i = 0; i < nodes.size(); i++) {
                            for (int j = 0; j < nodes.size(); j++) {
                                System.out.print((adjacencyMatrix[i][j] < 0 ? "" : " ") + adjacencyMatrix[i][j] + " ");
                            }
                            System.out.println();
                        }
                        System.out.println();*/
                    }

                    // add a list of the original degrees of vertices and only concatenate those that had degree 2 originally

                    Polygon tempTriangle;
                    Color currentColor;
                    int c = 0;
                    for (ArrayList<DTriangle> node : nodes) {
                        currentColor = new Color(Math.random(), Math.random(), Math.random(), 0.7);
                        for (DTriangle dt : node) {
                            tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                            tempTriangle.setFill(currentColor);
                            tempTriangle.setStroke(Color.BLACK);
                            pane.getChildren().add(tempTriangle);
                        }
                    }

                    for (int i = 0; i < nodes.size(); i++) {
                        for (int j = 0; j < nodes.size(); j++) {
                            System.out.print((adjacencyMatrix[i][j] < 0 ? "" : " ") + adjacencyMatrix[i][j] + " ");
                        }
                        System.out.println();
                    }

                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(simpleComponentButton);

        Button spanningTreeButton = new Button("Show spanning tree");
        spanningTreeButton.setOnAction(e -> {
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
                        if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                            inPolygon = false;
                        }
                        if (inPolygon) {
                            includedTriangles.add(dt);
                        }
                    }

                    ArrayList<DTriangle> nodes = new ArrayList<>(includedTriangles.size());
                    nodes.addAll(includedTriangles);
                    int[][] originalAdjacencyMatrix = new int[nodes.size()][nodes.size()];

                    // checking for adjacency between nodes
                    ArrayList<DEdge> checkedEdges = new ArrayList<>();
                    DTriangle dt1, dt2;
                    DEdge de;
                    for (int i = 0; i < includedTriangles.size(); i++) {
                        dt1 = includedTriangles.get(i);
                        // go through the edges of each triangle
                        for (int j = 0; j < 3; j++) {
                            de = dt1.getEdge(j);
                            if (!checkedEdges.contains(de)) {
                                int neighbourIndex = -1;
                                for (int k = 0; neighbourIndex == -1 && k < includedTriangles.size(); k++) {
                                    dt2 = includedTriangles.get(k);
                                    if (k != i && dt2.isEdgeOf(de)) {
                                        // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                                        neighbourIndex = k;
                                    }
                                }
                                if (neighbourIndex != -1) {
                                    originalAdjacencyMatrix[i][neighbourIndex] = 1;
                                    originalAdjacencyMatrix[neighbourIndex][i] = 1;
                                }
                                checkedEdges.add(de);
                            }
                        }
                    }

                    //for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                    int nodeIndex = 0;
                    int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
                    boolean[] visitedNodes = new boolean[nodes.size()];
                    int[] parentNodes = new int[nodes.size()];

                    ArrayList<Integer> nextLayer;
                    ArrayList<Integer> currentLayer = new ArrayList<>();
                    currentLayer.add(nodeIndex);
                    parentNodes[nodeIndex] = -1;
                    boolean unexploredLeft = true;

                    ArrayList<Line> tree = new ArrayList<>();
                    Line temp;
                    while (unexploredLeft) {
                        nextLayer = new ArrayList<>();
                        for (int i : currentLayer) {
                            visitedNodes[i] = true;
                            for (int j = 0; j < nodes.size(); j++) {
                                if (originalAdjacencyMatrix[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                                    spanningTreeAdjacencyMatrix[i][j] = 1;
                                    spanningTreeAdjacencyMatrix[j][i] = 1;
                                    nextLayer.add(j);
                                    parentNodes[j] = i;
                                    visitedNodes[j] = true;

                                    temp = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                    temp.setStroke(Color.RED);
                                    temp.setStrokeWidth(4);
                                    tree.add(temp);
                                }
                            }
                        }
                        currentLayer = nextLayer;
                        if (nextLayer.size() == 0) {
                            unexploredLeft = false;
                        }
                    }

                    ArrayList<DTriangle> separatingTriangles = new ArrayList<>();
                    ArrayList<Integer> separatingIndeces = new ArrayList<>();
                    for (int i = 0; i < nodes.size(); i++) {
                        boolean difference = false;
                        int degree = 0;
                        int adjacentIndex = -1;
                        for (int j = 0; j < nodes.size(); j++) {
                            if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                                degree++;
                            }
                            if (originalAdjacencyMatrix[i][j] == 1 && spanningTreeAdjacencyMatrix[i][j] != 1) {
                                difference = true;
                                adjacentIndex = j;
                            }
                        }
                        // that means it's a leaf node in the spanning tree but was previously connected to some other node
                        if (difference && !separatingTriangles.contains(nodes.get(adjacentIndex))) {
                            separatingTriangles.add(nodes.get(i));
                            separatingIndeces.add(i);
                        }
                    }

                    boolean properTree = true;
                    for (int i = 0; properTree && i < separatingIndeces.size(); i++) {
                        int adjacencyCount = 0;
                        for (int j = 0; j < nodes.size(); j++) {
                            if (spanningTreeAdjacencyMatrix[separatingIndeces.get(i)][j] == 1) {
                                adjacencyCount++;
                            }
                        }
                        if (adjacencyCount > 1) {
                            properTree = false;
                        }
                    }

                    Rectangle frame = new Rectangle(0, 0, pane.getWidth(), pane.getHeight());
                    frame.setStroke(Color.RED);
                    frame.setFill(Color.TRANSPARENT);
                    frame.setStrokeWidth(10);
                    if (properTree) {
                        frame.setStroke(Color.FORESTGREEN);
                    }
                    pane.getChildren().add(frame);

                    // breadth-first search:
                    // go through list of nodes in current layer
                    // go through their children (not parents!) and check if they have been visited
                    // if no: add them to a list of nodes which are in the next layer
                    // if yes: mark current node as "special" leaf
                    // continue with next layer

                    ArrayList<Polygon> showTriangles = new ArrayList<>(nodes.size());
                    Polygon tempTriangle;
                    Color currentColor;
                    for (DTriangle dt : nodes) {
                        if (separatingTriangles.contains(dt)) {
                            currentColor = Color.LIGHTBLUE.deriveColor(1, 1, 1, 0.7);
                        } else {
                            currentColor = Color.LAWNGREEN.deriveColor(1, 1, 1, 0.7);
                        }
                        tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                        tempTriangle.setFill(currentColor);
                        tempTriangle.setStroke(Color.BLACK);
                        showTriangles.add(tempTriangle);
                        //pane.getChildren().add(tempTriangle);
                    }
                    pane.getChildren().addAll(showTriangles);
                    pane.getChildren().addAll(tree);

                    Circle rootIndicator = new Circle(nodes.get(nodeIndex).getBarycenter().getX(), nodes.get(nodeIndex).getBarycenter().getY(), 5, Color.BLACK);
                    pane.getChildren().add(rootIndicator);

                        /*try {
                            //File file = new File("E:\\Simon\\Desktop\\Screenshots\\" + (int) (Math.random() * 1000) + "_screenshot.png");
                            File file = new File("E:\\Simon\\Desktop\\Screenshots\\" + nodeIndex + "_screenshot.png");
                            // Pad the capture area
                            WritableImage writableImage = new WritableImage((int) pane.getWidth() + 20, (int) pane.getHeight() + 20);
                            pane.snapshot(null, writableImage);
                            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                            // Write the snapshot to the chosen file
                            ImageIO.write(renderedImage, "png", file);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        pane.getChildren().remove(frame);
                        pane.getChildren().removeAll(showTriangles);
                        pane.getChildren().removeAll(tree);
                        pane.getChildren().remove(rootIndicator);
                        System.out.println("Screenshot with node " + nodeIndex + " as root taken.");*/
                    //}
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(spanningTreeButton);

        Button loopBreakingButton = new Button("Show loop breaking");
        loopBreakingButton.setOnAction(e -> {
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
                    List<DTriangle> holeTriangles = new ArrayList<>();

                    for (DTriangle dt : triangles) {
                        // check if triangle in polygon
                        double centerX = dt.getBarycenter().getX();
                        double centerY = dt.getBarycenter().getY();
                        boolean inPolygon = true;
                        boolean inHole = false;
                        if (!mapPolygons.get(0).contains(centerX, centerY)) {
                            inPolygon = false;
                        }
                        for (int i = 1; inPolygon && i < mapPolygons.size() - 1; i++) {
                            if (mapPolygons.get(i).contains(centerX, centerY)) {
                                inPolygon = false;
                                inHole = true;
                            }
                        }
                        if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                            inPolygon = false;
                        }
                        if (inPolygon) {
                            includedTriangles.add(dt);
                        }
                        if (inHole) {
                            holeTriangles.add(dt);
                        }
                    }

                    ArrayList<DTriangle> nodes = new ArrayList<>(includedTriangles.size());
                    nodes.addAll(includedTriangles);
                    int[][] originalAdjacencyMatrix = new int[nodes.size()][nodes.size()];

                    // checking for adjacency between nodes
                    ArrayList<DEdge> checkedEdges = new ArrayList<>();
                    DTriangle dt1, dt2;
                    DEdge de;
                    for (int i = 0; i < includedTriangles.size(); i++) {
                        dt1 = includedTriangles.get(i);
                        // go through the edges of each triangle
                        for (int j = 0; j < 3; j++) {
                            de = dt1.getEdge(j);
                            if (!checkedEdges.contains(de)) {
                                int neighbourIndex = -1;
                                for (int k = 0; neighbourIndex == -1 && k < includedTriangles.size(); k++) {
                                    dt2 = includedTriangles.get(k);
                                    if (k != i && dt2.isEdgeOf(de)) {
                                        // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                                        neighbourIndex = k;
                                    }
                                }
                                if (neighbourIndex != -1) {
                                    originalAdjacencyMatrix[i][neighbourIndex] = 1;
                                    originalAdjacencyMatrix[neighbourIndex][i] = 1;
                                }
                                checkedEdges.add(de);
                            }
                        }
                    }

                    int[] degreeMatrix = new int[nodes.size()];
                    int degreeCount;
                    for (int i = 0; i < nodes.size(); i++) {
                        degreeCount = 0;
                        for (int j = 0; j < nodes.size(); j++) {
                            if (originalAdjacencyMatrix[i][j] == 1) {
                                degreeCount++;
                            }
                        }
                        degreeMatrix[i] = degreeCount;
                    }

                    // 1. group hole triangles by hole
                    // 2. choose degree-2 triangle adjacent to the hole (can be according to some criterion)
                    // 3. ???
                    // 4. profit

                    /*
                    for each triangle, find all the triangles that are connected to it from holeTriangles
                    then do the same for the triangles added in that iteration
                    start with the first hole/triangle
                    find all its immediate neighbours from holeTriangles (and remove them from that list)
                    find their immediate neighbours (and so forth) until no new triangles are added to the hole
                     */

                    // checking for adjacency of triangles in the holes
                    checkedEdges.clear();
                    ArrayList<ArrayList<DTriangle>> holes = new ArrayList<>();
                    ArrayList<DTriangle> temp;
                    for (int i = 0; i < holeTriangles.size(); i++) {
                        dt1 = holeTriangles.get(i);
                        temp = new ArrayList<>();
                        temp.add(dt1);
                        holeTriangles.remove(dt1);

                        boolean addedToHole = true;
                        while (addedToHole) {
                            addedToHole = false;
                            // go through the remaining triangles and see if they are adjacent to the ones already in the hole
                            for (int j = 0; j < temp.size(); j++) {
                                for (int k = 0; k < holeTriangles.size(); k++) {
                                    if (temp.get(j) != holeTriangles.get(k) && (holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(0)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(1)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(2)))) {
                                        temp.add(holeTriangles.get(k));
                                        holeTriangles.remove(k);
                                        addedToHole = true;
                                        k--;
                                    }
                                }
                            }
                        }
                        holes.add(temp);
                        i--;
                    }

                    System.out.println("Nr. holes: " + holes.size());
                    for (ArrayList<DTriangle> hole : holes) {
                        System.out.println("Hole with " + hole.size() + (hole.size() > 1 ? " triangles" : " triangle"));
                    }

                    Polygon tempTriangle;
                    Color currentColor;
                    for (ArrayList<DTriangle> hole : holes) {
                        //currentColor = new Color(Math.random(), Math.random(), Math.random(), 0.7);
                        currentColor = Color.rgb(255, 251, 150, 0.4);
                        for (DTriangle dt : hole) {
                            tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                            tempTriangle.setFill(currentColor);
                            tempTriangle.setStroke(Color.GREY);
                            pane.getChildren().add(tempTriangle);
                        }
                    }

                    // separating triangles are those adjacent to a hole and (for now) degree 2
                    ArrayList<DTriangle> separatingTriangles = new ArrayList<>();
                    for (ArrayList<DTriangle> hole : holes) {
                        boolean triangleFound = false;
                        // go through triangles in the hole
                        for (int i = 0; !triangleFound && i < hole.size(); i++) {
                            dt1 = hole.get(i);
                            // go through triangles outside the hole
                            for (int j = 0; !triangleFound && j < nodes.size(); j++) {
                                dt2 = nodes.get(j);
                                // if the triangle has degree two and is adjacent to the holeTriangle, make it a separating triangle
                                if (degreeMatrix[j] == 2 && (dt2.isEdgeOf(dt1.getEdge(0)) || dt2.isEdgeOf(dt1.getEdge(1)) || dt2.isEdgeOf(dt1.getEdge(2)))) {
                                    int vertexCount = 0;
                                    for (DTriangle holeTriangle : hole) {
                                        for (int k = 0; k < dt2.getPoints().size(); k++) {
                                            if (holeTriangle.getPoints().contains(dt2.getPoint(k))) {
                                                vertexCount++;
                                            }
                                        }
                                    }
                                    int what = 0;
                                    for (int z = 0; z < separatingTriangles.size(); z++) {
                                        for (int k = 0; k < dt2.getPoints().size(); k++) {
                                            if (separatingTriangles.get(z).getPoints().contains(dt2.getPoint(k))) {
                                                what++;
                                            }
                                        }
                                    }
                                    System.out.println("vertexCount = " + vertexCount);
                                    if (vertexCount <= 4 && what < 2) {
                                        separatingTriangles.add(dt2);
                                        triangleFound = true;
                                    }
                                }
                            }
                        }
                    }
                    // if they form a loop, then change one?
                    // run spanning tree on the generated graph and see whether branches "meet"
                    // if so, break that loop either by adding a separating triangle or by changing a separating triangle
                    // also, could just use this from the start?

                    //int[][] spanningTreeAdjacencyMatrix = originalAdjacencyMatrix.clone();
                    int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
                    for (int i = 0; i < originalAdjacencyMatrix.length; i++) {
                        for (int j = 0; j < originalAdjacencyMatrix[0].length; j++) {
                            spanningTreeAdjacencyMatrix[i][j] = originalAdjacencyMatrix[i][j];
                        }
                    }
                    for (DTriangle dt : separatingTriangles) {
                        for (int i = 0; i < nodes.size(); i++) {
                            spanningTreeAdjacencyMatrix[nodes.indexOf(dt)][i] = 0;
                            spanningTreeAdjacencyMatrix[i][nodes.indexOf(dt)] = 0;
                        }
                    }

                    boolean unexploredLeft = true;
                    ArrayList<Integer> currentLayer = new ArrayList<>();
                    currentLayer.add(0);
                    ArrayList<Integer> nextLayer;

                    boolean[] visitedNodes = new boolean[nodes.size()];
                    int[] parentNodes = new int[nodes.size()];
                    parentNodes[0] = -1;

                    ArrayList<Line> tree = new ArrayList<>();
                    Line tempLine;
                    while (unexploredLeft) {
                        nextLayer = new ArrayList<>();
                        for (int i : currentLayer) {
                            visitedNodes[i] = true;
                            for (int j = 0; j < nodes.size(); j++) {
                                if (spanningTreeAdjacencyMatrix[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                                    nextLayer.add(j);
                                    parentNodes[j] = i;
                                    visitedNodes[j] = true;

                                    tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                    tempLine.setStroke(Color.RED);
                                    tempLine.setStrokeWidth(4);
                                    tree.add(tempLine);
                                }
                            }
                        }
                        currentLayer = nextLayer;
                        if (nextLayer.size() == 0) {
                            unexploredLeft = false;
                        }
                    }

                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        int c = 0;
                        for (int j = 0; j < spanningTreeAdjacencyMatrix[0].length; j++) {
                            if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                                c++;
                            }
                        }
                        System.out.println("Node " + i + " has " + c + " neighbours");
                    }

                    ArrayList<DTriangle> componentNodes = new ArrayList<>();
                    for (DTriangle dt : nodes) {
                        if (!separatingTriangles.contains(dt)) {
                            componentNodes.add(dt);
                        }
                    }
                    ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = new ArrayList<>();
                    visitedNodes = new boolean[nodes.size()];
                    int[] componentParentNodes = new int[nodes.size()];
                    for (int i = 0; i < componentNodes.size(); i++) {
                        temp = new ArrayList<>();
                        temp.add(componentNodes.get(i));

                        unexploredLeft = true;
                        currentLayer = new ArrayList<>();
                        /*System.out.println("i: " + i);
                        System.out.println("componentNodes.size(): " + componentNodes.size());*/
                        currentLayer.add(nodes.indexOf(componentNodes.get(i)));
                        componentNodes.remove(i);
                        while (unexploredLeft) {
                            nextLayer = new ArrayList<>();
                            for (int j : currentLayer) {
                                visitedNodes[j] = true;
                                for (int k = 0; k < componentNodes.size(); k++) {
                                    if (spanningTreeAdjacencyMatrix[j][nodes.indexOf(componentNodes.get(k))] == 1 && nodes.indexOf(componentNodes.get(k)) != componentParentNodes[j] && !visitedNodes[nodes.indexOf(componentNodes.get(k))]) {
                                        nextLayer.add(nodes.indexOf(componentNodes.get(k)));
                                        componentParentNodes[nodes.indexOf(componentNodes.get(k))] = j;
                                        visitedNodes[nodes.indexOf(componentNodes.get(k))] = true;
                                        temp.add(componentNodes.get(k));
                                        componentNodes.remove(k);
                                        k--;
                                    }
                                }
                            }
                            currentLayer = nextLayer;
                            if (nextLayer.size() == 0) {
                                unexploredLeft = false;
                            }
                        }
                        simplyConnectedComponents.add(temp);
                        i--;
                    }

                    System.out.println("holes.size(): " + holes.size() + "\nseparatingTriangles.size(): " + separatingTriangles.size() + "\nsimplyConnectedComponents.size(): " + simplyConnectedComponents.size());
                    if (simplyConnectedComponents.size() == 2) {
                        simplyConnectedComponents.sort((o1, o2) -> o1.size() > o2.size() ? -1 : (o1.size() == o2.size() ? 0 : 1));
                        //System.out.println("simplyConnectedComponents.size(): " + simplyConnectedComponents.size());

                        // now cut through any possible loops:
                        // want a triangle adjacent to one of the enclosing triangles for each cut-off region
                        // (and on the outside) that is also part of the loop -> how can you identify that?
                        // 1 could check whether everything is still reachable from the adjacent nodes of the one you want to make a separator
                        // 2 2.1 search for nodes in the tree which are not
                        //       a) separating triangles
                        //       b) parent and child
                        //       but are still adjacent in the separating tree graph
                        //   2.2 then trace their paths back to the last common ancestor
                        //       -> take path from one back to the root and store nodes, then take path from the other until you meet one of the nodes
                        //       -> thereby automatically iterate over all of the nodes on the loop
                        //   2.3 find a hole that has a triangle on the loop adjacent to it, then move the current adjacent
                        //       separating triangle to that new-found triangle (can use some other criterion for selection too)

                        /*for (int i = 0; i < adjacencyMatrix.length; i++) {
                            System.out.print(i + " | ");
                            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                                System.out.print(adjacencyMatrix[i][j] + " ");
                            }
                            System.out.println();
                        }*/

                        // finding pairs of adjacent nodes that are not parent and child (and thus form a loop)
                        ArrayList<int[]> adjacentPairs = new ArrayList<>();
                        for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                            for (int j = 0; j < i; j++) {
                                if (spanningTreeAdjacencyMatrix[i][j] == 1 && !(parentNodes[i] == j || parentNodes[j] == i)) {
                                    adjacentPairs.add(new int[]{i, j});
                                    System.out.println(i + " and " + j + " adjacent but not parent and child");
                                }
                            }
                        }

                        // trace the paths of the pair back to the last common ancestor
                        ArrayList<ArrayList<Integer>> loops = new ArrayList<>();
                        ArrayList<Integer>[] tempIndeces;
                        for (int[] adjacentPair : adjacentPairs) {
                            tempIndeces = new ArrayList[2];
                            tempIndeces[0] = new ArrayList<>();
                            tempIndeces[0].add(adjacentPair[0]);
                            int currentParent = parentNodes[adjacentPair[0]];
                            while (currentParent != -1) {
                                tempIndeces[0].add(currentParent);
                                currentParent = parentNodes[currentParent];
                            }

                            tempIndeces[1] = new ArrayList<>();
                            tempIndeces[1].add(adjacentPair[1]);
                            currentParent = parentNodes[adjacentPair[1]];
                            while (!tempIndeces[0].contains(currentParent)) {
                                tempIndeces[1].add(currentParent);
                                currentParent = parentNodes[currentParent];
                            }

                            int commonAncestor = currentParent;
                            int currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                            while (currentIndex != commonAncestor) {
                                tempIndeces[0].remove(tempIndeces[0].size() - 1);
                                currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                            }

                            for (int j = tempIndeces[1].size() - 1; j >= 0; j--) {
                                tempIndeces[0].add(tempIndeces[1].get(j));
                            }
                            loops.add(tempIndeces[0]);
                        }

                        for (int i = 0; i < loops.size(); i++) {
                            if (loops.get(i).size() <= 3) {
                                loops.remove(i);
                                i--;
                            }
                        }

                        DTriangle dt3;
                        if (loops.size() == 1) {
                            // find a hole adjacent to both the loop and the disconnected component
                            // then use its separating triangle to break the loop and open up the disconnected component
                            ArrayList<DTriangle> currentConnectedComponent = simplyConnectedComponents.get(1);
                            ArrayList<Integer> loopBreakingCandidates = new ArrayList<>();
                            ArrayList<DTriangle> currentHole;
                            for (int z = 0; z < holes.size(); z++) {
                                currentHole = holes.get(z);
                                // see whether the hole is adjacent to the connected components
                                // i.e. (its separating triangle) could be used to "break up" the barrier enclosing that component
                                boolean disconnectedAdjacencyFound = false;
                                boolean loopAdjacencyFound = false;
                                for (int i = 0; (!disconnectedAdjacencyFound || !loopAdjacencyFound) && i < currentHole.size(); i++) {
                                    dt1 = currentHole.get(i);
                                    for (int j = 0; !disconnectedAdjacencyFound && j < currentConnectedComponent.size(); j++) {
                                        dt2 = currentConnectedComponent.get(j);
                                        for (int k = 0; !disconnectedAdjacencyFound && k < dt1.getPoints().size(); k++) {
                                            if (dt2.isOnAnEdge(dt1.getPoint(k))) {
                                                disconnectedAdjacencyFound = true;
                                            }
                                        }
                                    }
                                    for (int j = 0; !loopAdjacencyFound && j < loops.get(0).size(); j++) {
                                        dt3 = nodes.get(loops.get(0).get(j));
                                        for (int k = 0; !loopAdjacencyFound && k < dt1.getEdges().length; k++) {
                                            if (dt3.isEdgeOf(dt1.getEdge(k))) {
                                                loopAdjacencyFound = true;
                                            }
                                        }
                                    }
                                }
                                if (disconnectedAdjacencyFound && loopAdjacencyFound) {
                                    loopBreakingCandidates.add(z);
                                }
                            }
                            System.out.println("loopBreakingCandidates.size(): " + loopBreakingCandidates.size());

                            if (loopBreakingCandidates.size() != 0) {
                                // change separating triangle
                                // find a triangle adjacent to the loop
                                DTriangle newSeparatingTriangle = null;
                                DTriangle oldSeparatingTriangle = separatingTriangles.get(loopBreakingCandidates.get(0));
                                boolean newSeparatingTriangleFound = false;
                                for (int i = 0; !newSeparatingTriangleFound && i < holes.get(loopBreakingCandidates.get(0)).size(); i++) {
                                    dt1 = holes.get(loopBreakingCandidates.get(0)).get(i);
                                    for (int j = 0; !newSeparatingTriangleFound && j < loops.get(0).size(); j++) {
                                        dt2 = nodes.get(loops.get(0).get(j));
                                        for (int k = 0; !newSeparatingTriangleFound && k < dt1.getEdges().length; k++) {
                                            if (dt2.isEdgeOf(dt1.getEdge(k))) {
                                                newSeparatingTriangle = dt2;
                                                newSeparatingTriangleFound = true;
                                            }
                                        }
                                    }
                                }
                                if (newSeparatingTriangleFound) {
                                    separatingTriangles.set(loopBreakingCandidates.get(0), newSeparatingTriangle);
                                    // updating the adjacency matrix
                                    for (int i = 0; i < nodes.size(); i++) {
                                        spanningTreeAdjacencyMatrix[nodes.indexOf(newSeparatingTriangle)][i] = 0;
                                        spanningTreeAdjacencyMatrix[i][nodes.indexOf(newSeparatingTriangle)] = 0;
                                    }
                                } else {
                                    System.out.println("No new separating triangle found.");
                                }

                                // updating the adjacency matrix
                                for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                                    if (originalAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] == 1 || originalAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] == 1) {
                                        spanningTreeAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] = 1;
                                        spanningTreeAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] = 1;
                                        System.out.println("Happens");
                                    }
                                }

                                // visuals, aye
                                for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                                    for (int j = 0; j < i; j++) {
                                        if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                                            tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                            tempLine.setStroke(Color.RED);
                                            tempLine.setStrokeWidth(4);
                                            tree.add(tempLine);
                                        }
                                    }
                                }
                            }
                        } else {
                            System.out.println("There are " + (loops.size() > 1 ? "multiple" : "no") + "loops.");
                        }
                    } else {
                        System.out.println("There are " + (simplyConnectedComponents.size() == 1 ? "no more" : simplyConnectedComponents.size()) + " simply-connected components.");
                    }

                    ArrayList<Polygon> showTriangles = new ArrayList<>(nodes.size());
                    for (DTriangle dt : nodes) {
                        if (separatingTriangles.contains(dt)) {
                            currentColor = Color.MEDIUMVIOLETRED.deriveColor(1, 1, 1, 0.7);
                        } else {
                            currentColor = Color.LAWNGREEN.deriveColor(1, 1, 1, 0.7);
                        }
                        tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                        tempTriangle.setStroke(Color.BLACK);
                        tempTriangle.setFill(currentColor);
                        showTriangles.add(tempTriangle);
                        //pane.getChildren().add(tempTriangle);
                    }
                    pane.getChildren().addAll(showTriangles);
                    pane.getChildren().addAll(tree);
                    pane.getChildren().add(new Circle(nodes.get(0).getBarycenter().getX(), nodes.get(0).getBarycenter().getY(), 5, Color.BLACK));
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(loopBreakingButton);

        Button theButtonToEndAllButtons = new Button("The button to\nend all buttons");
        theButtonToEndAllButtons.setOnAction(e -> {
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
                    List<DTriangle> holeTriangles = new ArrayList<>();

                    ArrayList<Node> toFrontNodes = new ArrayList<>();

                    for (DTriangle dt : triangles) {
                        // check if triangle in polygon
                        double centerX = dt.getBarycenter().getX();
                        double centerY = dt.getBarycenter().getY();
                        boolean inPolygon = true;
                        boolean inHole = false;
                        if (!mapPolygons.get(0).contains(centerX, centerY)) {
                            inPolygon = false;
                        }
                        for (int i = 1; inPolygon && i < mapPolygons.size() - 1; i++) {
                            if (mapPolygons.get(i).contains(centerX, centerY)) {
                                inPolygon = false;
                                inHole = true;
                            }
                        }
                        if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                            inPolygon = false;
                        }
                        if (inPolygon) {
                            Circle c = new Circle(dt.getBarycenter().getX(), dt.getBarycenter().getY(), 4);
                            c.setFill(Color.BLUE);

                            Label index = new Label(includedTriangles.size() + "");
                            index.setTranslateX(c.getCenterX() + 5);
                            index.setTranslateY(c.getCenterY() + 5);

                            toFrontNodes.add(c);
                            toFrontNodes.add(index);

                            includedTriangles.add(dt);
                        }
                        if (inHole) {
                            holeTriangles.add(dt);
                        }
                    }

                    ArrayList<DTriangle> nodes = new ArrayList<>(includedTriangles.size());
                    nodes.addAll(includedTriangles);
                    int[][] originalAdjacencyMatrix = new int[nodes.size()][nodes.size()];

                    // checking for adjacency between nodes
                    ArrayList<DEdge> checkedEdges = new ArrayList<>();
                    DTriangle dt1, dt2;
                    DEdge de;
                    for (int i = 0; i < includedTriangles.size(); i++) {
                        dt1 = includedTriangles.get(i);
                        // go through the edges of each triangle
                        for (int j = 0; j < 3; j++) {
                            de = dt1.getEdge(j);
                            if (!checkedEdges.contains(de)) {
                                int neighbourIndex = -1;
                                for (int k = 0; neighbourIndex == -1 && k < includedTriangles.size(); k++) {
                                    dt2 = includedTriangles.get(k);
                                    if (k != i && dt2.isEdgeOf(de)) {
                                        // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                                        neighbourIndex = k;
                                    }
                                }
                                if (neighbourIndex != -1) {
                                    originalAdjacencyMatrix[i][neighbourIndex] = 1;
                                    originalAdjacencyMatrix[neighbourIndex][i] = 1;
                                }
                                checkedEdges.add(de);
                            }
                        }
                    }

                    ArrayList<ArrayList<DTriangle>> holes = new ArrayList<>();
                    ArrayList<DTriangle> tempTriangle;
                    for (int i = 0; i < holeTriangles.size(); i++) {
                        dt1 = holeTriangles.get(i);
                        tempTriangle = new ArrayList<>();
                        tempTriangle.add(dt1);
                        holeTriangles.remove(dt1);

                        boolean addedToHole = true;
                        while (addedToHole) {
                            addedToHole = false;
                            // go through the remaining triangles and see if they are adjacent to the ones already in the hole
                            for (int j = 0; j < tempTriangle.size(); j++) {
                                for (int k = 0; k < holeTriangles.size(); k++) {
                                    if (tempTriangle.get(j) != holeTriangles.get(k) && (holeTriangles.get(k).isEdgeOf(tempTriangle.get(j).getEdge(0)) || holeTriangles.get(k).isEdgeOf(tempTriangle.get(j).getEdge(1)) || holeTriangles.get(k).isEdgeOf(tempTriangle.get(j).getEdge(2)))) {
                                        tempTriangle.add(holeTriangles.get(k));
                                        holeTriangles.remove(k);
                                        addedToHole = true;
                                        k--;
                                    }
                                }
                            }
                        }
                        holes.add(tempTriangle);
                        i--;
                    }

                    //for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                    int nodeIndex = 0;
                    int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
                    boolean[] visitedNodes = new boolean[nodes.size()];
                    int[] parentNodes = new int[nodes.size()];

                    ArrayList<Integer> nextLayer;
                    ArrayList<Integer> currentLayer = new ArrayList<>();
                    currentLayer.add(nodeIndex);
                    parentNodes[nodeIndex] = -1;
                    boolean unexploredLeft = true;

                    ArrayList<Line> tree = new ArrayList<>();
                    Line tempLine;
                    while (unexploredLeft) {
                        nextLayer = new ArrayList<>();
                        for (int i : currentLayer) {
                            visitedNodes[i] = true;
                            for (int j = 0; j < nodes.size(); j++) {
                                if (originalAdjacencyMatrix[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                                    spanningTreeAdjacencyMatrix[i][j] = 1;
                                    spanningTreeAdjacencyMatrix[j][i] = 1;
                                    nextLayer.add(j);
                                    parentNodes[j] = i;
                                    visitedNodes[j] = true;

                                    tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                    tempLine.setStroke(Color.RED);
                                    tempLine.setStrokeWidth(4);
                                    tree.add(tempLine);
                                }
                            }
                        }
                        currentLayer = nextLayer;
                        if (nextLayer.size() == 0) {
                            unexploredLeft = false;
                        }
                    }

                    // finding pairs of adjacent nodes that are not parent and child (and thus form a loop)
                    ArrayList<int[]> adjacentPairs = new ArrayList<>();
                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        for (int j = 0; j < i; j++) {
                            if (originalAdjacencyMatrix[i][j] == 1 && !(parentNodes[i] == j || parentNodes[j] == i)) {
                                adjacentPairs.add(new int[]{i, j});
                            }
                        }
                    }

                    // trace the paths of the pair back to the last common ancestor
                    // TODO: need better checks for "tightest" loops (neighbouring branches can also form loops)
                    ArrayList<ArrayList<Integer>> loops = new ArrayList<>();
                    ArrayList<Integer>[] tempIndeces;
                    for (int[] adjacentPair : adjacentPairs) {
                        tempIndeces = new ArrayList[2];
                        tempIndeces[0] = new ArrayList<>();
                        tempIndeces[0].add(adjacentPair[0]);
                        int currentParent = parentNodes[adjacentPair[0]];
                        while (currentParent != -1) {
                            tempIndeces[0].add(currentParent);
                            currentParent = parentNodes[currentParent];
                        }

                        tempIndeces[1] = new ArrayList<>();
                        tempIndeces[1].add(adjacentPair[1]);
                        currentParent = parentNodes[adjacentPair[1]];
                        ArrayList<Integer> currentNeighbours = new ArrayList<>();
                        for (int i = 0; i < originalAdjacencyMatrix.length; i++) {
                            if (originalAdjacencyMatrix[currentParent][i] == 1 && i != adjacentPair[1]) {
                                currentNeighbours.add(i);
                            }
                        }
                        int currentChild;
                        while (!tempIndeces[0].contains(currentParent) /*&& !tempIndeces[0].contains(neighbours(currentParent))*/) {
                            tempIndeces[1].add(currentParent);
                            currentChild = currentParent;
                            currentParent = parentNodes[currentParent];
                            currentNeighbours = new ArrayList<>();
                            for (int i = 0; i < originalAdjacencyMatrix.length; i++) {
                                if (originalAdjacencyMatrix[currentParent][i] == 1 && i != currentChild) {
                                    currentNeighbours.add(i);
                                }
                            }
                        }


                        int commonAncestor = currentParent;
                        int currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                        while (currentIndex != commonAncestor) {
                            tempIndeces[0].remove(tempIndeces[0].size() - 1);
                            currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                        }

                        for (int j = tempIndeces[1].size() - 1; j >= 0; j--) {
                            tempIndeces[0].add(tempIndeces[1].get(j));
                        }
                        loops.add(tempIndeces[0]);
                    }

                    //System.out.println("loops.size(): " + loops.size());

                    for (int i = 0; i < loops.size(); i++) {
                        System.out.print("Loop " + (i + 1) + ": ");
                        for (int j = 0; j < loops.get(i).size(); j++) {
                            System.out.print(loops.get(i).get(j) + " ");
                        }
                        System.out.println();
                    }


                    ArrayList<DTriangle> separatingTriangles = new ArrayList<>();
                    for (ArrayList<Integer> loop : loops) {
                        boolean separatingTriangleFound = false;
                        DTriangle currentSeparatingTriangle = null;
                        for (int i = 0; !separatingTriangleFound && i < holes.size(); i++) {
                            for (int j = 0; !separatingTriangleFound && j < loop.size(); j++) {
                                dt1 = nodes.get(loop.get(j));
                                boolean adjacentFound = false;
                                for (int z = 0; !adjacentFound && z < separatingTriangles.size(); z++) {
                                    for (int y = 0; y < dt1.getEdges().length; y++) {
                                        if (separatingTriangles.get(z).isEdgeOf(dt1.getEdge(y)) || separatingTriangles.get(z).getPoints().contains(dt1.getPoint(y))) {
                                            adjacentFound = true;
                                        }
                                    }
                                }
                                if (adjacentFound || separatingTriangles.contains(dt1)) {
                                    //continue;
                                }
                                for (int k = 0; !separatingTriangleFound && k < holes.get(i).size(); k++) {
                                    for (int l = 0; !separatingTriangleFound && l < dt1.getEdges().length; l++) {
                                        if (holes.get(i).get(k).isEdgeOf(dt1.getEdge(l))) {
                                            // TODO: Also check for adjacency with existing separating triangles (avoid redundancy)
                                            separatingTriangleFound = true;
                                            currentSeparatingTriangle = dt1;
                                            ArrayList<DTriangle> tmp = holes.remove(i);
                                            holes.add(tmp);
                                            i--;
                                        }
                                    }
                                }
                            }
                        }
                        if (currentSeparatingTriangle != null) {
                            separatingTriangles.add(currentSeparatingTriangle);
                        }
                    }

                    ArrayList<Line> loopLines = new ArrayList<>();
                    Color currentColor;
                    for (ArrayList<Integer> loop : loops) {
                        currentColor = new Color(Math.random(), Math.random(), Math.random(), 1);
                        for (int i = 0; i < loop.size(); i++) {
                            tempLine = new Line(nodes.get(loop.get(i)).getBarycenter().getX(), nodes.get(loop.get(i)).getBarycenter().getY(), nodes.get(loop.get((i + 1) % loop.size())).getBarycenter().getX(), nodes.get(loop.get((i + 1) % loop.size())).getBarycenter().getY());
                            tempLine.setStroke(currentColor);
                            tempLine.setStrokeWidth(5);
                            loopLines.add(tempLine);
                        }
                    }

                    ArrayList<Polygon> showTriangles = new ArrayList<>(nodes.size());
                    Polygon tempTrianglePolygon;
                    for (DTriangle dt : nodes) {
                        if (separatingTriangles.contains(dt)) {
                            currentColor = Color.LIGHTBLUE.deriveColor(1, 1, 1, 0.7);
                        } else {
                            currentColor = Color.LAWNGREEN.deriveColor(1, 1, 1, 0.7);
                        }
                        tempTrianglePolygon = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                        tempTrianglePolygon.setFill(currentColor);
                        tempTrianglePolygon.setStroke(Color.BLACK);
                        showTriangles.add(tempTrianglePolygon);
                    }
                    pane.getChildren().addAll(showTriangles);
                    pane.getChildren().addAll(tree);
                    pane.getChildren().addAll(loopLines);
                    pane.getChildren().addAll(toFrontNodes);

                    Circle rootIndicator = new Circle(nodes.get(nodeIndex).getBarycenter().getX(), nodes.get(nodeIndex).getBarycenter().getY(), 5, Color.BLACK);
                    pane.getChildren().add(rootIndicator);

                        /*try {
                            //File file = new File("E:\\Simon\\Desktop\\Screenshots\\" + (int) (Math.random() * 1000) + "_screenshot.png");
                            File file = new File("E:\\Simon\\Desktop\\Screenshots\\" + nodeIndex + "_screenshot.png");
                            // Pad the capture area
                            WritableImage writableImage = new WritableImage((int) pane.getWidth() + 20, (int) pane.getHeight() + 20);
                            pane.snapshot(null, writableImage);
                            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                            // Write the snapshot to the chosen file
                            ImageIO.write(renderedImage, "png", file);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        pane.getChildren().remove(frame);
                        pane.getChildren().removeAll(showTriangles);
                        pane.getChildren().removeAll(tree);
                        pane.getChildren().remove(rootIndicator);
                        System.out.println("Screenshot with node " + nodeIndex + " as root taken.");*/
                    //}
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(theButtonToEndAllButtons);

        Button shortestPathMapButton = new Button("Shortest path map");
        shortestPathMapButton.setOnAction(e -> {
            if (mapPolygons == null || mapPolygons.isEmpty()) {
                System.out.println("Not enough data to construct simulation!");
            } else {
                map = new MapRepresentation(mapPolygons);
                ArrayList<DTriangle> excluded = new ArrayList<>();
                try {
                    excluded.add(new DTriangle(
                            new DPoint(969.0, 759.0, 0.0),
                            new DPoint(41.0, 750.0, 0.0),
                            new DPoint(506.0, 624.0, 0.0)
                    ));
                    /*excluded.add(new DTriangle(
                            new DPoint(120.0, 443.0, 0.0),
                            new DPoint(41.0, 750.0, 0.0),
                            new DPoint(506.0, 624.0, 0.0)
                    ));*/
                } catch (DelaunayError delaunayError) {
                    delaunayError.printStackTrace();
                }
                ShortestPathRoadMap sprm = new ShortestPathRoadMap(map, excluded);
            }
        });
        menu.getChildren().add(shortestPathMapButton);

        // ****************************************************************************************************** //
        // New controls to debug the new project structure
        // ****************************************************************************************************** //

        Separator menuSeparator4 = new Separator(Orientation.HORIZONTAL);
        menuSeparator4.setStyle("-fx-background-color: #ffffff");
        menuSeparator4.setMinHeight(10);
        menuSeparator4.setPrefHeight(10);
        menuSeparator4.setMaxHeight(10);

        Label menuLabel3 = new Label("New model debugging");
        menuLabel3.setFont(Font.font("Arial", 14));

        Separator menuSeparator5 = new Separator(Orientation.HORIZONTAL);
        menuSeparator5.setStyle("-fx-background-color: #ffffff");
        menuSeparator5.setMinHeight(10);
        menuSeparator5.setPrefHeight(10);
        menuSeparator5.setMaxHeight(10);
        menu.getChildren().addAll(menuSeparator4, menuLabel3, menuSeparator5);

        Button startIntroducingEntitiesButton = new Button("Start introducing entities");
        startIntroducingEntitiesButton.setOnAction(e -> {
            if (mapPolygons == null || mapPolygons.isEmpty()) {
                System.out.println("Not enough data to construct simulation!");
            } else {
                initPlaceAgents();
                map = new MapRepresentation(mapPolygons);
            }
        });
        menu.getChildren().add(startIntroducingEntitiesButton);

        Button placeDCREntityButton = new Button("Place random entity (evading)");
        placeDCREntityButton.setOnAction(e -> {
            if (map == null) {
                map = new MapRepresentation(mapPolygons);
            }

            VisualAgent va = new VisualAgent(500, 500);
            va.getAgentBody().setFill(Color.LAWNGREEN);
            pane.getChildren().add(va);
            RandomEntity randomEntity = new RandomEntity(map);
            randomEntity.setAgent(new Agent(va.getSettings()));
            map.getEvadingEntities().add(randomEntity);

            useEntities.set(true);
            testDCREntity = new DCREntity(map);
            map.getPursuingEntities().add(testDCREntity);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(placeDCREntityButton);

        Button placeRandomEntity = new Button("Place DCR entity");
        placeRandomEntity.setOnAction(e -> {
            useEntities.set(true);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(placeRandomEntity);

        Button startAdaptedSimulation = new Button("Start adapted simulation");
        startAdaptedSimulation.setOnAction(e -> {
            adaptedSimulation = new AdaptedSimulation(map);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(startAdaptedSimulation);

        Button pauseAdaptedSimulation = new Button("Pause adapted simulation");
        pauseAdaptedSimulation.setOnAction(e -> {
            if (adaptedSimulation != null && adaptedSimulation.isPaused()) {
                adaptedSimulation.unPause();
            } else if (adaptedSimulation != null) {
                adaptedSimulation.pause();
            }
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(pauseAdaptedSimulation);

        Slider adaptedSlider = new Slider(1, 101, 51);
        adaptedSlider.setShowTickMarks(true);
        adaptedSlider.setShowTickLabels(true);
        adaptedSlider.setMajorTickUnit(20);
        adaptedSlider.setMaxWidth(180);
        menu.getChildren().add(adaptedSlider);

        adaptedSlider.valueProperty().addListener((ov, oldValue, newValue) -> {
            //System.out.println("val = " + newValue);
            if (adaptedSimulation != null) {
                adaptedSimulation.setTimeStep((int) (double) newValue);
            }
        });

        // ****************************************************************************************************** //
        // JavaFX stuff
        // ****************************************************************************************************** //

        Scene scene = new Scene(outerLayout, 1400, 800);
        primaryStage.setTitle("Robin's Ruthless Robbers");

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/rrr_icon.png")));

        primaryStage.setScene(scene);
        primaryStage.show();
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

            if (mapPolygons.get(mapPolygons.size() - 1).getPoints().size() == 0) {
                mapPolygons.remove(mapPolygons.size() - 1);
            }
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
                for (int i = 0; i < mapPolygons.size(); i++) {
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map and controlledAgents data file", "*.maa"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(selectedFile))) {
                // write map to file
                out.println("map");
                for (int i = 0; i < mapPolygons.size(); i++) {
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
                            // adding the obstaclePolygons
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
                    // read controlledAgents data
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
                        pane.getChildren().add(visualAgent);
                    }
                    for (MapPolygon p : mapPolygons) {
                        p.toFront();
                    }
                    for (Shape c : covers) {
                        c.toFront();
                    }
                    mapPolygons.get(0).setMouseTransparent(true);
                } else {
                    currentState = ProgramState.MAP_EDITING;
                    // map data only
                    String[] coords;
                    double[] coordsDouble;
                    boolean firstLoop = true;
                    while (firstLoop || ((line = in.readLine()) != null && !line.isEmpty())) {
                        firstLoop = false;
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coords.length; i += 2) {
                            // adding the obstaclePolygons
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

                    indicatorLine.setVisible(false);
                }
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
                        for (int i = 1; i < mapPolygons.size(); i++) {
                            if (mapPolygons.get(i).contains(e.getX(), e.getY())) {
                                return;
                            }
                        }
                        for (VisualAgent va : visualAgents) {
                            if (va.contains(e.getX(), e.getY())) {
                                return;
                            }
                        }
                        if (e.isPrimaryButtonDown() && mapPolygons.get(0).contains(e.getX(), e.getY())) {
                            if (visualAgents == null) {
                                visualAgents = new ArrayList<>();
                            }
                            if (!useEntities.getValue()) {
                                // old behaviour
                                VisualAgent visualAgent = new VisualAgent(e.getX(), e.getY());
                                visualAgents.add(visualAgent);
                                pane.getChildren().add(visualAgent);

                                // covering areas beyond the controlledAgents's vision
                                for (Shape s : covers) {
                                    s.toFront();
                                }
                                mapPolygons.get(0).toFront();
                                mapPolygons.get(0).setMouseTransparent(true);
                            } else {
                                // new behaviour
                                VisualAgent va = new VisualAgent(e.getX(), e.getY());
                                va.getAgentBody().setFill(Color.INDIANRED);
                                pane.getChildren().add(va);

                                testDCREntity.addAgent(new Agent(va.getSettings()));

                                for (Shape s : covers) {
                                    s.toFront();
                                }
                                mapPolygons.get(0).toFront();
                                mapPolygons.get(0).setMouseTransparent(true);
                            }
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
                        //contains controlledAgents
                        ContextMenu contextMenu = new ContextMenu();
                        MenuItem editItem = new MenuItem("Edit");
                        MenuItem deleteItem = new MenuItem("Delete");
                        contextMenu.getItems().addAll(editItem, deleteItem);

                        editItem.setOnAction(ae -> {
                            Dialog<AgentSettings> dialog = new Dialog<>();
                            dialog.setTitle("Edit controlledAgents settings");
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
                            agentPolicy.getItems().addAll("Random policy", "Straight line policy", "Flocking evader policy", "Hide evader policy", "Dummy policy");
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

                            Platform.runLater(xpos::requestFocus);

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
                                    } else if (agentPolicy.getValue().equals("Flocking evader policy")) {
                                        s.setMovePolicy("flocking_evader_policy");
                                    } else if (agentPolicy.getValue().equals("Dummy policy")) {
                                        s.setMovePolicy("dummy_policy");
                                    } else if (agentPolicy.getValue().equals("Hide evader policy")) {
                                        s.setMovePolicy("hide_evader_policy");
                                    }

                                    return s;
                                }
                                return null;
                            });

                            Optional<AgentSettings> result = dialog.showAndWait();

                            result.ifPresent(va::adoptSettings);
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
