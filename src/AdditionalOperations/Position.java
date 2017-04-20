package AdditionalOperations;

import javafx.geometry.Pos;

import javax.vecmath.Vector2d;

/**
 * Created by robin on 19.04.2017.
 */
public class Position {


    private double xPos, yPos;

    public Position(double xPos, double yPos)   {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public Position()   {
        this.xPos = 0;
        this.yPos = 0;
    }

    public void setxPos(double xPos) {
        this.xPos = xPos;
    }

    public void setyPos(double yPos) {
        this.yPos = yPos;
    }

    public double getxPos() {
        return xPos;
    }

    public double getyPos() {
        return yPos;
    }
}
