package ui;

import control.Controller;
import conversion.GridConversion;
import entities.CentralisedEntity;
import entities.DCREntity;
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

public class AltMain extends Application {

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

    // ************************************************************************************************************** //
    // Test stuff for centralised algorithm
    // ************************************************************************************************************** //
    private boolean testCentralised;
    private CentralisedEntity testCentralisedEntity;
    // ************************************************************************************************************** //
    // Test stuff for centralised algorithm
    // ************************************************************************************************************** //

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

        Button testCentralisedButton = new Button("Test centralised policy");
        testCentralisedButton.setOnAction(e -> {
            MapRepresentation map = new MapRepresentation(mapPolygons);
            testCentralisedEntity = new DCREntity(map);
            testCentralised = true;
            int requiredAgents = testCentralisedEntity.remainingRequiredAgents();
            System.out.println("Required agents: " + requiredAgents);
            // show required number of agents and settings for the algorithm
            // add the next <required number> agents to this entity
            // could make it an option to place a desire number of agents under the premise that capture is not guaranteed
        });
        menu.getChildren().add(testCentralisedButton);

        Button startTestCentralisedButton = new Button("Start centralised test");
        startTestCentralisedButton.setOnAction(e -> {
            Simulation.testCentralisedEntity = this.testCentralisedEntity;
            Simulation sim = Controller.getSimulation();
            if (sim == null) {
                if (mapPolygons == null || visualAgents == null || mapPolygons.isEmpty() || visualAgents.isEmpty()) {
                    System.out.println("Not enough data to construct simulation!");
                } else {
                    Controller.theBestTest(mapPolygons, visualAgents);
                    currentState = ProgramState.SIMULATION;
                }
            } else {
                sim.unPause();
            }
        });
        menu.getChildren().add(startTestCentralisedButton);

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
                        System.out.println(dt.getAngle(0));
                        System.out.println(dt.getAngle(1));
                        System.out.println(dt.getAngle(2));
                        System.out.println();
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

                    int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
                    boolean[] visitedNodes = new boolean[nodes.size()];
                    int[] parentNodes = new int[nodes.size()];

                    ArrayList<Integer> nextLayer;
                    ArrayList<Integer> currentLayer = new ArrayList<>();
                    currentLayer.add(0);
                    boolean unexploredLeft = true;

                    ArrayList<Line> tree = new ArrayList<>();
                    Line temp;
                    while (unexploredLeft) {
                        nextLayer = new ArrayList<>();
                        for (int i : currentLayer) {
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
                        if (difference && degree == 1 && !separatingTriangles.contains(nodes.get(adjacentIndex))) {
                            separatingTriangles.add(nodes.get(i));
                        }
                    }

                    // breadth-first search:
                    // go through list of nodes in current layer
                    // go through their children (not parents!) and check if they have been visited
                    // if no: add them to a list of nodes which are in the next layer
                    // if yes: mark current node as "special" leaf
                    // continue with next layer

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
                        pane.getChildren().add(tempTriangle);
                    }
                    pane.getChildren().addAll(tree);
                } catch (DelaunayError error) {
                    error.printStackTrace();
                }
            }
        });
        menu.getChildren().add(spanningTreeButton);

        Slider slider = new Slider(0, 150, 100);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(40);
        slider.setMaxWidth(180);
        menu.getChildren().add(slider);

        slider.valueProperty().addListener((ov, oldValue, newValue) -> {
            //System.out.println("val = " + newValue);
            Controller.getSimulation().setTimeStep((int) (double) newValue);
        });

        pursuers = new ArrayList<>();
        evaders = new ArrayList<>();

        Scene scene = new Scene(outerLayout, 1200, 800);
        primaryStage.setTitle("Coded by Winston v5.76.002 build 42 alpha");

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map and controlledAgents data file", "*.maa"));
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
                    } while ((line = in.readLine()) != null);

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
                        boolean placedInHole = false;
                        for (int i = 1; i < mapPolygons.size(); i++) {
                            if (mapPolygons.get(i).contains(e.getX(), e.getY())) {
                                placedInHole = true;
                            }
                        }
                        if (!placedInHole && e.isPrimaryButtonDown() && mapPolygons.get(0).contains(e.getX(), e.getY())) {
                            if (visualAgents == null) {
                                visualAgents = new ArrayList<>();
                            }
                            if (!testCentralised) {
                                VisualAgent visualAgent = new VisualAgent(e.getX(), e.getY());
                                visualAgents.add(visualAgent);
                                pane.getChildren().add(visualAgent);

                                // covering areas beyond the controlledAgents's vision
                                for (Shape s : covers) {
                                    s.toFront();
                                }
                                mapPolygons.get(0).toFront();
                            } else {
                                VisualAgent va1 = new VisualAgent(e.getX(), e.getY());
                                va1.getAgentBody().setFill(Color.LAWNGREEN);
                                VisualAgent va2 = new VisualAgent(e.getX(), e.getY());
                                va2.getAgentBody().setFill(Color.LAWNGREEN);
                                pane.getChildren().addAll(va1, va2);
                                testCentralisedEntity.addAgent(new Agent(va1.getSettings()));
                                testCentralisedEntity.addAgent(new Agent(va2.getSettings()));
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
