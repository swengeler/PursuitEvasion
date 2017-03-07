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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import simulation.RevealedMap;

import java.util.ArrayList;

public class Main extends Application {

    private static final double CELLSIZE = 5;

    private HBox outerLayout;
    private VBox menu;
    private ZoomablePane pane;

    private Line indicatorLine;
    public static ArrayList<MapPolygon> mapPolygons;
    private MapPolygon currentMapPolygon;

    private ArrayList<Circle> pursuers;
    private ArrayList<Circle> evaders;
    private ArrayList<VisualAgent> visualAgents;

    private BooleanProperty addPoints;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // top-level container, partitions window into drawing pane and menu
        outerLayout = new HBox();
        outerLayout.setPrefSize(1200, 800);

        // sidebar menu, currently with dummy buttons
        menu = new VBox();
        menu.setStyle("-fx-background-color: #ffffff");
        menu.setMinWidth(190);
        menu.setPrefSize(190, 600);
        menu.setMaxWidth(190);
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
        menu.getChildren().add(simulationButton);
        Button testButton = new Button("Start Simulation");
        testButton.setOnAction(e -> {
            Controller.test(pursuers, evaders);
        });
        menu.getChildren().add(testButton);
        Button convertButton = new Button("Print Grid");
        convertButton.setOnAction(e -> {
            GridConversion.convert(mapPolygons, pursuers, evaders, CELLSIZE);
        });
        menu.getChildren().add(convertButton);
        addPoints = new SimpleBooleanProperty(false);
        CheckBox b = new CheckBox("To draw or\nnot to draw");
        addPoints.bind(b.selectedProperty());
        menu.getChildren().add(b);
        Button whyNotButton = new Button("Why not?");
        whyNotButton.setOnAction(e -> {
            Circle pursuer;
            if (pursuers == null) {
                pursuers = new ArrayList<>();
            }
            for (int i = 0; i < 1000; i++) {
                pursuer = new Circle(400, 400, 5, Color.RED);
                pursuers.add(pursuer);
                pane.getChildren().add(pursuer);
            }
        });
        menu.getChildren().add(whyNotButton);

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

        pursuers = new ArrayList<>();
        evaders = new ArrayList<>();

        Scene scene = new Scene(outerLayout, 1200, 800);
        primaryStage.setTitle("Coded by Winston v5.76.002 build 41 alpha");

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/rrr_icon.png")));
        //pane.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/like-3.png"))));

        primaryStage.setScene(scene);
        primaryStage.show();

        Button polygonTestButton = new Button("Polygon test");
        polygonTestButton.setOnAction(e -> {
            if (pane.getChildren().contains(mapPolygons.get(0))) {
                pane.getChildren().remove(mapPolygons.get(0));
            }
            pane.getChildren().removeAll(mapPolygons);
            polygonWithHoles = mapPolygons.get(0);
            System.out.println(mapPolygons.size());
            for (int i = 1; i < mapPolygons.size() - 1; i++) {
                changePolygonWithHoles(mapPolygons.get(i).getPolygon());
            }
            pane.getChildren().add(polygonWithHoles);
            polygonWithHoles.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.5));
            polygonWithHoles.setStroke(Color.GREEN);
            polygonWithHoles.toFront();
        });
        menu.getChildren().add(polygonTestButton);

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
            if (addPoints.getValue()) {
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
                    for (int i = 1; i < mapPolygons.size(); i++) {
                        if (mapPolygons.get(i) != currentMapPolygon && currentMapPolygon.contains(mapPolygons.get(i).getPoints().get(0), mapPolygons.get(i).getPoints().get(1))) {
                            // currentMapPolygon.removeAnchor(a);
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
                    if (connectedToOld && /*currentMapPolygon.isClosed()*/a.getCenterX() == currentMapPolygon.getPoints().get(0) && a.getCenterY() == currentMapPolygon.getPoints().get(1)) {
                        for (int i = 1; i < mapPolygons.size(); i++) {
                            if (mapPolygons.get(i) != currentMapPolygon && currentMapPolygon.contains(mapPolygons.get(i).getPoints().get(0), mapPolygons.get(i).getPoints().get(1))) {
                                // currentMapPolygon.removeAnchor(a);
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
            } else {
                if (mapPolygons.get(0).isClosed()) {
                    /*if (e.isPrimaryButtonDown()) {
                        Circle pursuer = new Circle(e.getX(), e.getY(), 5, Color.TOMATO);
                        if (pursuers == null) {
                            pursuers = new ArrayList<>();
                        }
                        pursuers.add(pursuer);
                        pane.getChildren().add(pursuer);
                    } else {
                        Circle evader = new Circle(e.getX(), e.getY(), 5, Color.GREEN);
                        if (evaders == null) {
                            evaders = new ArrayList<>();
                        }
                        evaders.add(evader);
                        pane.getChildren().add(evader);
                    }*/
                    if (e.isPrimaryButtonDown()) {
                        VisualAgent visualAgent = new VisualAgent(e.getX(), e.getY());
                        if (visualAgents == null) {
                            visualAgents = new ArrayList<>();
                        }
                        visualAgents.add(visualAgent);
                        pane.getChildren().add(visualAgent);
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
