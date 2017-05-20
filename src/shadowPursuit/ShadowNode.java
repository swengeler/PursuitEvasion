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
    ShadowNode left, right;
    //FOr Type3 Nodes
    private Line placedOn, occLine;
    //For Type4 Nodes
    private Line occLeft, occRight;
    private int type;
    private Point2D position;


    //Type1 Node
    public ShadowNode(Point2D position) {
        this.position = position;
        type = type1;
        left = null;
        right = null;
    }


    //Type2 Node()
    public ShadowNode(Point2D position, ShadowNode neighbor1, boolean left) {


        this.position = position;
        if (neighbor1.getRight() == null) {

            this.left = neighbor1;
            neighbor1.right = this;
        } else if (neighbor1.getLeft() == null) {
            this.right = neighbor1;
            neighbor1.left = this;
        } else {
            if (left) {
                this.left = neighbor1;
                neighbor1.right = this;
            } else {
                this.right = neighbor1;
                neighbor1.left = this;
            }
        }


        type = type2;
    }


    public ShadowNode(Point2D position, boolean isType2S) {

        if(isType2S) {
            this.position = position;
            this.left = null;
            this.right = null;
            type = type2;
        }
        else
            System.exit(51212);
    }

    public ShadowNode(Point2D position, ShadowNode neighbor1) {


        this.position = position;
        if (neighbor1.getRight() == null) {

            this.left = neighbor1;
            neighbor1.right = this;
        } else if (neighbor1.getLeft() == null) {
            this.right = neighbor1;
            neighbor1.left = this;
        }


        type = type2;
    }

    //For Type3
    public ShadowNode(Point2D position, ShadowNode type2Neighbor, Line placedOn) {
        this.position = position;
        this.left = type2Neighbor;
        this.right = null;

        this.placedOn = placedOn;
        this.occLine = new Line(position.getX(), position.getY(), type2Neighbor.getPosition().getX(), type2Neighbor.getPosition().getY());
    }

    //For Type3
    public ShadowNode(Point2D position, ShadowNode left, ShadowNode right, boolean T3) {

        System.out.println("ENTTEREDED");
        this.position = position;
        this.left = left;
        this.right = right;

        left.right = this;
        right.left = this;
        type = type3;

        //System.out.println(this);

        this.placedOn = placedOn;
        this.occLine = new Line(position.getX(), position.getY(), left.getPosition().getX(), left.getPosition().getY());
    }

    //For Type4
    public ShadowNode(Point2D position, ShadowNode type2Neighbor1, ShadowNode type2Neighbor2) {
        this.position = position;


        this.left = type2Neighbor1;
        this.right = type2Neighbor2;

        this.occLine = this.placedOn = placedOn;
        this.occLeft = new Line(position.getX(), position.getY(), type2Neighbor1.getPosition().getX(), type2Neighbor1.getPosition().getY());
        this.occRight = new Line(position.getX(), position.getY(), type2Neighbor2.getPosition().getX(), type2Neighbor2.getPosition().getY());
    }


    public int getType() {
        return type;
    }

    public void setRight(ShadowNode newNode) {
        this.right = newNode;
        newNode.left = this;
    }

    public void setLeft(ShadowNode newNode) {
        this.left = newNode;
        newNode.right = this;
    }

    public Point2D getPosition() {
        return position;
    }

    public Line getPlacedOn() {
        return placedOn;
    }

    public ShadowNode getLeft() {
        return left;
    }

    public ShadowNode getRight() {
        return right;
    }

    public void addrightType1(ShadowNode newNeighbor) {
        this.right = newNeighbor;
        newNeighbor.left = this;
    }

    public String toString() {
        Point2D leftPos, rightPos;
        leftPos = null;
        rightPos = null;

        if (left != null) {
            leftPos = left.getPosition();
        }
        if (right != null) {
            rightPos = right.getPosition();
        }


        return new String(this.getPosition().toString() + "\tleft = " + leftPos + "\tright = " + rightPos + "\tType = " + type);
    }


}
