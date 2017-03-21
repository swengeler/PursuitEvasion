package simulation;

public class AgentSettings {

    private double x, y, speed, turnSpeed, fieldOfViewAngle, fieldOfViewRange;

    private boolean pursuing;

    public AgentSettings(double speed, double turnSpeed) {
        this.speed = speed;
        this.turnSpeed = turnSpeed;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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

    public boolean isPursuing() {
        return pursuing;
    }

    public void setPursuing(boolean pursuing) {
        this.pursuing = pursuing;
    }

}
