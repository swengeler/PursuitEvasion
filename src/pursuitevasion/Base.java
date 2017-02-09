/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pursuitevasion;

import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author jyrr
 */
public class Base extends Application {

    //used to continously draw a 'possible line' when the mouse is moving on the scene
    private Line possibleLine = null;
    
    private ArrayList<Circle> points;
    private ArrayList<Line> lines;
    private ArrayList<Polygon> polygons;

    //used to indicate whether we should draw the 'possible line' or not
    private boolean drawPos;

    @Override
    public void start(Stage primaryStage) {
        //radius for circles aka points
        float radius = 7.5f;
        
        //offset for clicking on a point aka circle, within this offset it will be seen as the point itself
        double offset = 15;

        Pane root = new Pane();

        Scene scene = new Scene(root, 500, 500);

        drawPos = true;
        points = new ArrayList<>();
        lines = new ArrayList<>();
        polygons = new ArrayList<>();

        scene.setOnMouseClicked(eh -> {
            //probably need some more sophisticated logic for this... :D
            root.getChildren().clear();
            
            //avoiding errors
            if (possibleLine != null) {
                //adding to our line array (line between two points)
                lines.add(possibleLine);
            }

            //create new point
            Circle c = new Circle(eh.getX(), eh.getY(), radius);
            c.setFill(Color.VIOLET);

            //check if point already exists, if so we need to create a new polygon
            for (Circle ci : points) {
                if (((c.getCenterX() + offset) > ci.getCenterX()) && ((c.getCenterX() - offset) < ci.getCenterX())
                        && (((c.getCenterY() + offset) > ci.getCenterY()) && ((c.getCenterY() - offset) < ci.getCenterY()))) {
                    drawPos = false;
                }
            }

            //add point
            points.add(c);

            //logic to add a new polygon -> remove possibleLine, points and lines, we start over again
            //only thing left over is the polygon
            if (!drawPos) {
                Polygon p = new Polygon();
                for (int i = 0; i < points.size() - 1; i++) {
                    //add all points to the new polygon except for the last (it will be the point that was in the proximity)
                    Circle cp = points.get(i);
                    p.getPoints().add(cp.getCenterX());
                    p.getPoints().add(cp.getCenterY());
                }
                
                //addition of polygon, setting stroke, color etc
                p.setFill(null);
                p.setStrokeWidth(2);
                p.setStroke(Color.TOMATO);
                polygons.add(p);
                
                //set everything to default
                possibleLine = null;
                points.clear();
                lines.clear();
                drawPos = true;

                //some sexy animations
                FadeTransition ft = new FadeTransition(Duration.millis(500), p);
                ft.setFromValue(0);
                ft.setToValue(1.0);
                ft.setCycleCount(1);
                ft.play();
            }

            //idem
            FadeTransition ft = new FadeTransition(Duration.millis(150), c);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.setCycleCount(1);
            ft.play();

            //add to root to draw them all, probably very bad logic
            root.getChildren().addAll(points);
            root.getChildren().addAll(lines);
            root.getChildren().addAll(polygons);
        });

        scene.setOnMouseMoved(eh -> {
            //check if we should even draw the possible line, and if we have a starting point (otherwise we'd get errors)
            if (drawPos && points.size() > 0) {
                root.getChildren().clear();
                
                //retrieve last point, and current point the mouse is hovering over
                Circle pc = points.get(points.size() - 1);
                Circle nc = new Circle(eh.getX(), eh.getY(), radius);
                
                //construct 'possible line'
                possibleLine = new Line(pc.getCenterX(), pc.getCenterY(), nc.getCenterX(), nc.getCenterY());
                possibleLine.setStrokeWidth(2);
                
                root.getChildren().addAll(points);
                root.getChildren().addAll(lines);
                root.getChildren().addAll(polygons);
                root.getChildren().add(possibleLine);
            }
        });

        primaryStage.setTitle("Multi-Agent Pursuit Evasion");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
