package simulation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.ArrayList;

public class Agent {

    private double speed, turnSpeed, turnAngle, fieldOfViewAngle, fieldOfViewRange;

    private DoubleProperty xPosProperty;
    private DoubleProperty yPosProperty;

    private MovePolicy policy;

    public Agent(double xPos, double yPos, double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange) {
        xPosProperty = new SimpleDoubleProperty(xPos);
        yPosProperty = new SimpleDoubleProperty(yPos);
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
        return turnAngle;
    }

    public void setTurnAngle(double turnAngle) {
        this.turnAngle = turnAngle;
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
        return xPosProperty.get();
    }

    public void setXPos(double xPos) {
        this.xPosProperty.set(xPos);
    }

    public double getYPos() {
        return yPosProperty.get();
    }

    public void setYPos(double yPos) {
        this.yPosProperty.set(yPos);
    }

    public DoubleProperty getXPosProperty() {
        return xPosProperty;
    }

    public DoubleProperty getYPosProperty() {
        return yPosProperty;
    }

    public MovePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(MovePolicy policy) {
        this.policy = policy;
    }

    public void move(MapRepresentation map, ArrayList<Agent> agents, long timeStep) {
        Move nextMove = policy.getNextMove(map, agents, timeStep);
        xPosProperty.set(xPosProperty.get() + nextMove.getXDelta());
        yPosProperty.set(yPosProperty.get() + nextMove.getYDelta());
        turnAngle += nextMove.getTurnDelta();
    }

}