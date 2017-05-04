package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

/**
 * Created by Robins on 30.04.2017.
 */
public class ShadowNode {

    private final int type1 = 1;
    private final int type2 = 2;
    private final int type3 = 3;
    private final int type4 = 4;


    //FOr Type3 Nodes
    private Line placedOn, occLine;

    //For Type4 Nodes
    private Line occLeft, occRight;

    private int type;

    ShadowNode prev, next;


    private Point2D position;


    //Type1 Node
    public ShadowNode(Point2D position) {
        this.position = position;
        type = type1;
    }


    //Type2 Node()
    public ShadowNode(Point2D position, ShadowNode neighbor1) {
        this.position = position;
        this.prev = neighbor1;
        this.next = null;

        type = type2;
    }

    //For Type3
    public ShadowNode(Point2D position, ShadowNode type2Neighbor, Line placedOn) {
        this.position = position;
        this.prev = type2Neighbor;
        this.next = null;

        this.placedOn = placedOn;
        this.occLine = new Line(position.getX(), position.getY(), type2Neighbor.getPosition().getX(), type2Neighbor.getPosition().getY());
    }

    //For Type4
    public ShadowNode(Point2D position, ShadowNode type2Neighbor1, ShadowNode type2Neighbor2) {
        this.position = position;


        this.prev = type2Neighbor1;
        this.next = type2Neighbor2;

        this.occLine =

                this.placedOn = placedOn;
        this.occLeft = new Line(position.getX(), position.getY(), type2Neighbor1.getPosition().getX(), type2Neighbor1.getPosition().getY());
        this.occRight = new Line(position.getX(), position.getY(), type2Neighbor2.getPosition().getX(), type2Neighbor2.getPosition().getY());
    }


    public int getType() {
        return type;
    }

    public Point2D getPosition() {
        return position;
    }

    public Line getPlacedOn() {
        return placedOn;
    }

    public ShadowNode getPrev() {
        return prev;
    }

    public ShadowNode getNext() {
        return next;
    }

    public void addnextType1(ShadowNode newNeighbor) {
        this.next = newNeighbor;
        newNeighbor.prev = this;
    }

    public String toString() {
        return new String(this.getPosition().toString());
    }


}
