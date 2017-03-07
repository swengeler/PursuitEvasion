package simulation;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;

public class RevealedMap {

    public static void showMapDebug(Pane pane) {
        debugPane = pane;
    }

    private static Pane debugPane = new Pane();

    private Shape revealedArea;

    private Polygon outerBorder;
    private ArrayList<Polygon> holes;

    public RevealedMap(Polygon outerBorder, ArrayList<Polygon> holes) {
        revealedArea = new Rectangle();
        this.outerBorder = outerBorder;
        this.holes = holes;
    }

    public RevealedMap(Polygon outerBorder) {
        this.outerBorder = outerBorder;
    }

    // TODO: DISTRIBUTE THIS ON SOMEBODY OTHER THAN MYSELF (RASHEED)

    public void update(double xPos, double yPos, double turnAngle, double fieldOfViewAngle, double fieldOfViewRange) {
        // constructing the shape of the field of vision that the agent has
        Arc arc = new Arc();
        arc.setCenterX(xPos);
        arc.setCenterY(yPos);
        arc.setRadiusX(fieldOfViewRange);
        arc.setRadiusY(fieldOfViewRange);
        arc.setStartAngle(turnAngle);
        arc.setLength(fieldOfViewAngle);
        arc.setType(ArcType.ROUND);
        arc.setFill(Color.BLACK);

        // getting the shape of the area that the agent has vision of which is actually on the map
        Shape addition = arc;
        for (Polygon h : holes) {
            addition = Shape.subtract(addition, h);
        }
        addition = Shape.intersect(addition, outerBorder);

        if (debugPane.getChildren().contains(revealedArea)) {
            debugPane.getChildren().remove(revealedArea);
        }

        // adding the new revealed area to the map
        long before = System.currentTimeMillis();
        revealedArea = Shape.union(revealedArea, addition);
        System.out.println("Time to compute revealedArea: " + (System.currentTimeMillis() - before));
        revealedArea.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.1));

        debugPane.getChildren().add(revealedArea);
    }

}
