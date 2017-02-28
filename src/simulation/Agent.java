package simulation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.ArrayList;

public class Agent {

    private double speed, turnSpeed, fieldOfViewAngle, fieldOfViewRange;

    private DoubleProperty xPos;
    private DoubleProperty yPos;
    private DoubleProperty turnAngle;

    private MovePolicy policy;

    public Agent(double xPos, double yPos, double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange) {
        this.xPos = new SimpleDoubleProperty(xPos);
        this.yPos = new SimpleDoubleProperty(yPos);
        this.turnAngle = new SimpleDoubleProperty(0);
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.fieldOfViewAngle = fieldOfViewAngle;
        this.fieldOfViewRange = fieldOfViewRange;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getTurnSpeed() {
        return turnSpeed;
    }

    public void setTurnSpeed(double turnSpeed) {
        this.turnSpeed = turnSpeed;
    }

    public double getTurnAngle() {
        return turnAngle.get();
    }

    public void setTurnAngle(double turnAngle) {
        this.turnAngle.set(turnAngle);
    }

    public double getFieldOfViewAngle() {
        return fieldOfViewAngle;
    }

    public void setFieldOfViewAngle(double fieldOfViewAngle) {
        this.fieldOfViewAngle = fieldOfViewAngle;
    }

    public double getFieldOfViewRange() {
        return fieldOfViewRange;
    }

    public void setFieldOfViewRange(double fieldOfViewRange) {
        this.fieldOfViewRange = fieldOfViewRange;
    }

    public double getXPos() {
        return xPos.get();
    }

    public void setXPos(double xPos) {
        this.xPos.set(xPos);
    }

    public double getYPos() {
        return yPos.get();
    }

    public void setYPos(double yPos) {
        this.yPos.set(yPos);
    }

    public DoubleProperty xPosProperty() {
        return xPos;
    }

    public DoubleProperty yPosProperty() {
        return yPos;
    }

    public DoubleProperty turnAngleProperty() {
        return turnAngle;
    }

    public MovePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(MovePolicy policy) {
        this.policy = policy;
    }

    public void move(MapRepresentation map, ArrayList<Agent> agents, long timeStep) {
        Move nextMove = policy.getNextMove(map, agents, timeStep);
        xPos.set(xPos.get() + nextMove.getXDelta());
        yPos.set(yPos.get() + nextMove.getYDelta());
        turnAngle.set(turnAngle.get() + nextMove.getTurnDelta());
    }

}