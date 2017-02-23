package simulation;

import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public class RevealedMap {

    private Polygon outerBorder;
    private ArrayList<Polygon> holes;

    public RevealedMap() {}

    public RevealedMap(Polygon outerBorder, ArrayList<Polygon> holes) {
        this.outerBorder = outerBorder;
        this.holes = holes;
    }

    public RevealedMap(Polygon outerBorder) {
        this.outerBorder = outerBorder;
    }

    // TODO: DISTRIBUTE THIS ON SOMEBODY OTHER THAN MYSELF (RASHEED)

    public void update(double xPos, double yPos, double turnAngle, double fieldOfViewAngle, double fieldOfViewRange) {

    }

}
