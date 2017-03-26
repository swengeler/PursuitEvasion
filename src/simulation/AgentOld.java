/*
package simulation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.ArrayList;

public class Agent {

    private double speed, turnSpeed, fieldOfViewAngle, fieldOfViewRange, captureRange = 10;

    private DoubleProperty xPos;
    private DoubleProperty yPos;
    private DoubleProperty turnAngle;

    private MovePolicy policy;

    private boolean isActive = true;

    public Agent(double xPos, double yPos, double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange) {
        this.xPos = new SimpleDoubleProperty(xPos);
        this.yPos = new SimpleDoubleProperty(yPos);
        this.turnAngle = new SimpleDoubleProperty(0);
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.fieldOfViewAngle = fieldOfViewAngle;
        this.fieldOfViewRange = fieldOfViewRange;
    }

    public Agent(double xPos, double yPos, double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange, double captureRange) {
        this.xPos = new SimpleDoubleProperty(xPos);
        this.yPos = new SimpleDoubleProperty(yPos);
        this.turnAngle = new SimpleDoubleProperty(0);
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.fieldOfViewAngle = fieldOfViewAngle;
        this.fieldOfViewRange = fieldOfViewRange;
        this.captureRange = captureRange;
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

    public boolean inRange(double x, double y) {
        if (policy.evadingPolicy()) {
            return false;
        }
        return Math.sqrt(Math.pow(xPos.get() - x, 2) + Math.pow(yPos.get() - y, 2)) <= captureRange;
    }

    public boolean isPursuer() {
        return policy.pursuingPolicy();
    }

    public boolean isEvader() {
        return policy.evadingPolicy();
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public void move(MapRepresentation map, ArrayList<Agent> agents) {
        Move nextMove = policy.getNextMove(map, agents);
        xPos.set(xPos.get() + nextMove.getXDelta());
        yPos.set(yPos.get() + nextMove.getYDelta());
        turnAngle.set(turnAngle.get() + nextMove.getTurnDelta());
    }

}*/
