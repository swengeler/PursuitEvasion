package simulation;

import java.util.ArrayList;

public class Agent {

    private double speed, turnSpeed, turnAngle, fieldOfViewAngle, fieldOfViewRange, xPos, yPos;

    private MovePolicy policy;

    public Agent(double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange) {
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
        return xPos;
    }

    public void setXPos(double xPos) {
        this.xPos = xPos;
    }

    public double getYPos() {
        return yPos;
    }

    public void setYPos(double yPos) {
        this.yPos = yPos;
    }

    public MovePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(MovePolicy policy) {
        this.policy = policy;
    }

    public void move(MapRepresentation map, ArrayList<Agent> agents, long timeStep) {
        Move nextMove = policy.getNextMove(map, agents, timeStep);
        xPos += nextMove.getXDelta();
        yPos += nextMove.getYDelta();
        turnAngle += nextMove.getTurnDelta();
    }

}