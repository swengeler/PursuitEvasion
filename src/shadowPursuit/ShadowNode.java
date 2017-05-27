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
    boolean inCircle = false;


    private Point2D neighbourRightAgent, neighbourLeftAgent;


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

        if (isType2S) {
            this.position = position;
            this.left = null;
            this.right = null;
            type = type2;
        } else {
            System.exit(51212);
        }
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
    public ShadowNode(Point2D position, ShadowNode Type2, Line Ray) {

        //System.out.println("ENTTEREDED");
        this.position = position;

        if (Type2.getLeft() == null) {
            this.right = Type2;
            this.left = null;
        } else if (Type2.getRight() == null) {
            this.left = Type2;
            this.right = null;
        }

        type = type3;

        System.out.println(this);


        this.occLine = Ray;
    }

    //For Type3
    public ShadowNode(Point2D position, ShadowNode left, ShadowNode right, boolean T3) {

        //System.out.println("ENTTEREDED");
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


        if (type2Neighbor1.getLeft() == null && type2Neighbor2.getRight() == null) {

        }
        this.left = type2Neighbor1;
        this.right = type2Neighbor2;

        type = type4;


        this.occLine = this.placedOn = placedOn;
        this.occLeft = new Line(position.getX(), position.getY(), type2Neighbor1.getPosition().getX(), type2Neighbor1.getPosition().getY());
        this.occRight = new Line(position.getX(), position.getY(), type2Neighbor2.getPosition().getX(), type2Neighbor2.getPosition().getY());
    }

    //For Type4
    public ShadowNode(Point2D position, ShadowNode type2Neighbor1, ShadowNode type2Neighbor2, Point2D agent1, Point2D agent2) {
        //note that type2neighbour 1 is the type 2 corresponding to agent one that created an occlusion ray and vice versa for type2neighbour2 and agent2

        this.position = position;
        this.neighbourRightAgent = agent1;
        this.neighbourLeftAgent = agent2;


        System.out.println("Type1 => " + type2Neighbor1 + "\nType2 => " + type2Neighbor2);

        if (type2Neighbor1.getLeft() == null && type2Neighbor2.getRight() == null) {
            this.right = type2Neighbor1;
            this.left = type2Neighbor2;
            this.neighbourRightAgent = agent1;
            this.neighbourLeftAgent = agent2;
        } else if (type2Neighbor1.getRight() == null && type2Neighbor2.getLeft() == null) {
            this.left = type2Neighbor1;
            this.right = type2Neighbor2;
            this.neighbourRightAgent = agent2;
            this.neighbourLeftAgent = agent1;
        } else {
            System.exit(67666);
        }

        type = type4;


    }

    public Point2D getLeftAgent() {
        return neighbourLeftAgent;
    }

    public Point2D getRightAgent() {
        return neighbourRightAgent;
    }

    public void isInCircle() {
        if (right != null) {
            this.inCircle = true;
            if (right.inCircle == false) {
                right.isInCircle();
            }
        } else {
            System.exit(00000);
        }
    }


    public void connect(ShadowNode next) {
        if (this.getLeft() != null && this.getLeft() != next && this.getRight() == null) {
            right = next;
            next.left = this;
        } else if (this.getRight() != null && this.getRight() != next && this.getLeft() == null) {
            //System.out.println("THIS = " + this + "\nNEXT = " + next);
            left = next;
            next.right = this;
        }
    }


    public int numberOfLinks() {
        int count = 0;
        if (this.getLeft() != null) {
            count++;
        }
        if (this.getRight() != null) {
            count++;
        }
        return count;

    }


    public int getType() {
        return type;
    }

    //Exclusive to Typ3
    public void addT1(ShadowNode T1) {
        if (this.type == 3) {
            //TODO Continue here ROBIN
            if (left.getType() == 2) {
                right = T1;
                T1.left = this;
            } else if (right.getType() == 2) {
                left = T1;
                T1.right = this;
            } else {
                System.out.println("Add T1 error");
                System.exit(9889);
            }
        } else {
            System.out.println("stop cheating");
            System.exit(9999);
        }
    }

    public ShadowNode[] getAdjT3() {
        ShadowNode[] list = new ShadowNode[2];
        int i = 0;

        if (left.getType() == 3) {
            list[i] = left;
            i++;
        } else if (right.getType() == 3) {
            list[i] = right;
            i++;
        } else {
            return null;
        }

        return list;
    }

    public void overwriteT3(ShadowNode overW) {
        if (left.getType() == 3 && right.getType() == 1) {
            left = overW;
            overW.right = this;
        } else if (right.getType() == 3 && left.getType() == 1) {
            right = overW;
            overW.left = this;
        } else {
            System.exit(118);
        }
    }

    public void overwriteT3(ShadowNode overW, ShadowNode toOverWrite) {
        if (left == toOverWrite) {
            left = overW;
            overW.right = this;
        } else if (right == toOverWrite) {
            right = overW;
            overW.left = this;
        } else {
            System.exit(117);
        }
    }

    public Point2D getPosition() {
        return position;
    }

    public Line getRay() {
        return occLine;
    }

    public ShadowNode getLeft() {
        return left;
    }

    public void setLeft(ShadowNode newNode) {
        this.left = newNode;
        newNode.right = this;
    }

    public ShadowNode getRight() {
        return right;
    }

    public void setRight(ShadowNode newNode) {
        this.right = newNode;
        newNode.left = this;
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


    //For Type3
    public ShadowNode getConnectedType2() {
        //System.out.println("PROBLEM FOR =>\n" + this);
        if (getLeft() != null && getLeft().getType() == 2) {
            return left;
        } else if (getRight() != null && getRight().getType() == 2) {
            return right;
        } else {

            return null;
        }
    }

    //For Type3
    public ShadowNode getConnectedType3() {
        if (left.getType() == 3) {
            return left;
        } else if (right.getType() == 3) {
            return right;
        } else {
            return null;
        }
    }

    public void connectT2() {


        if (this.getType() == 3) {
            if (this.getLeft().getType() == 2) {
                this.getLeft().right = this;
            } else if (this.getRight().getType() == 2) {
                System.out.println("!!!!");
                this.getRight().left = this;
            }
        } else if (this.getType() == 4) {
            if (this.getLeft().getType() == 2) {
                this.getLeft().right = this;
            }
            if (this.getRight().getType() == 2) {
                this.getRight().left = this;
            }
        }
        System.out.println("BITCH = " + this);
    }
}
