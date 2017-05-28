package simulation;

import java.util.ArrayList;

public class Agent {

    private double captureRange = 10;

    private AgentSettings settings;

    private MovePolicy policy;

    private boolean isActive = true;

    public Agent(AgentSettings agentSettings) {
        settings = agentSettings;
    }

    public double getSpeed() {
        return settings.getSpeed();
    }

    public void setSpeed(double speed) {
        settings.setSpeed(speed);
    }

    public double getTurnSpeed() {
        return settings.getTurnSpeed();
    }

    public void setTurnSpeed(double turnSpeed) {
        settings.setTurnSpeed(turnSpeed);
    }

    public double getTurnAngle() {
        return settings.getTurnAngle();
    }

    public void setTurnAngle(double turnAngle) {
        settings.setTurnAngle(turnAngle);
    }

    public double getFieldOfViewAngle() {
        return settings.getFieldOfViewAngle();
    }

    public void setFieldOfViewAngle(double fieldOfViewAngle) {
        settings.setFieldOfViewAngle(fieldOfViewAngle);
    }

    public double getFieldOfViewRange() {
        return settings.getFieldOfViewRange();
    }

    public void setFieldOfViewRange(double fieldOfViewRange) {
        settings.setFieldOfViewRange(fieldOfViewRange);
    }

    public double getXPos() {
        return settings.getXPos();
    }

    public void setXPos(double xPos) {
        settings.setXPos(xPos);
    }

    public double getYPos() {
        return settings.getYPos();
    }

    public void setYPos(double yPos) {
        settings.setYPos(yPos);
    }

    public void moveBy(double deltaX, double deltaY) {
        setXPos(getXPos() + deltaX);
        setYPos(getYPos() + deltaY);
        //System.out.printf("Agent moved by (%.4f|%.4f)\n", deltaX, deltaY);
    }

    public MovePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(MovePolicy policy) {
        this.policy = policy;
        settings.setMovePolicy(policy.toString());
    }

    public void setPolicy(MapRepresentation map, String policyEncoding) {
        if (policyEncoding.equals("random_policy")) {
            policy = new RandomMovePolicy(this, settings.isPursuing(), map);
        } else if (policyEncoding.equals("straight_line_policy")) {
            policy = new StraightLineMovePolicy(this, settings.isPursuing(), map);
        } else if (policyEncoding.equals("flocking_evader_policy")) {
            policy = new FlockingEvaderPolicy(this, settings.isPursuing(), map);
        } else if (policyEncoding.equals("dummy_policy")) {
            policy = new DummyPolicy(this, settings.isPursuing());
        } else if (policyEncoding.equals("hide_evader_policy")) {
            policy = new HideEvaderPolicy(this, settings.isPursuing(), map);
        }
        //policy = new RandomMovePolicy(this, settings.isPursuing(), map);
        //policy = new FollowMovePolicy(this, settings.isPursuing(), map);
    }

    public boolean inRange(double x, double y) {
        if (policy.evadingPolicy()) {
            return false;
        }
        return Math.sqrt(Math.pow(getXPos() - x, 2) + Math.pow(getYPos() - y, 2)) <= captureRange;
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
        setXPos(getXPos() + nextMove.getXDelta());
        setYPos(getYPos() + nextMove.getYDelta());
        setTurnAngle(getTurnAngle() + nextMove.getTurnDelta());
    }

    /*@Override
    public String toString() {
        Formatter f = new Formatter();
        return f.format("Agent [xPos=%.3f, yPos=%.3f, speed=%.1f]", getXPos(), getYPos(), getSpeed()).toString();
    }*/

}