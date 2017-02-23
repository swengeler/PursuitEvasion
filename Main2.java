package ui;

import conversion.GridConversion;
import javafx.animation.StrokeTransition;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        menu.getChildren().addAll(
                new Button("Dummy 1"),
                new Button("Dummy 2")
        );
        Button convertButton = new Button("Dummy 3");
        convertButton.setOnAction(e -> {
            GridConversion.convert(mapPolygons, pursuers, evaders, pane.getWidth(), pane.getHeight(), CELLSIZE);
        });
        menu.getChildren().add(convertButton);
        addPoints = new SimpleBooleanProperty(false);
        CheckBox b = new CheckBox("Dummy 4");
        addPoints.bind(b.selectedProperty());
        menu.getChildren().add(b);

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
        primaryStage.setTitle("Robin's Ruthless Robbers");
        primaryStage.setScene(scene);
        primaryStage.show();
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
                    if (e.isPrimaryButtonDown()) {
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
