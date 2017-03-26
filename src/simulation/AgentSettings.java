package simulation;

import javafx.beans.property.*;

public class AgentSettings {

    private DoubleProperty xPos, yPos;
    private DoubleProperty speed, turnSpeed;
    private DoubleProperty turnAngle, fieldOfViewAngle;
    private DoubleProperty fieldOfViewRange;

    private BooleanProperty pursuing;

    private StringProperty movePolicy;

    public AgentSettings() {
        xPos = new SimpleDoubleProperty(0);
        yPos = new SimpleDoubleProperty(0);

        speed = new SimpleDoubleProperty(150);
        turnSpeed = new SimpleDoubleProperty(25);

        turnAngle = new SimpleDoubleProperty(0);
        fieldOfViewAngle = new SimpleDoubleProperty(180);

        fieldOfViewRange = new SimpleDoubleProperty(100);

        pursuing = new SimpleBooleanProperty(false);

        movePolicy = new SimpleStringProperty("random_policy");
    }

    public double getXPos() {
        return xPos.get();
    }

    public DoubleProperty xPosProperty() {
        return xPos;
    }

    public void setXPos(double xPos) {
        this.xPos.set(xPos);
    }

    public double getYPos() {
        return yPos.get();
    }

    public DoubleProperty yPosProperty() {
        return yPos;
    }

    public void setYPos(double yPos) {
        this.yPos.set(yPos);
    }

    public double getSpeed() {
        return speed.get();
    }

    public DoubleProperty speedProperty() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed.set(speed);
    }

    public double getTurnSpeed() {
        return turnSpeed.get();
    }

    public DoubleProperty turnSpeedProperty() {
        return turnSpeed;
    }

    public void setTurnSpeed(double turnSpeed) {
        this.turnSpeed.set(turnSpeed);
    }

    public double getTurnAngle() {
        return turnAngle.get();
    }

    public DoubleProperty turnAngleProperty() {
        return turnAngle;
    }

    public void setTurnAngle(double turnAngle) {
        this.turnAngle.set(turnAngle);
    }

    public double getFieldOfViewAngle() {
        return fieldOfViewAngle.get();
    }

    public DoubleProperty fieldOfViewAngleProperty() {
        return fieldOfViewAngle;
    }

    public void setFieldOfViewAngle(double fieldOfViewAngle) {
        this.fieldOfViewAngle.set(fieldOfViewAngle);
    }

    public double getFieldOfViewRange() {
        return fieldOfViewRange.get();
    }

    public DoubleProperty fieldOfViewRangeProperty() {
        return fieldOfViewRange;
    }

    public void setFieldOfViewRange(double fieldOfViewRange) {
        this.fieldOfViewRange.set(fieldOfViewRange);
    }

    public boolean isPursuing() {
        return pursuing.get();
    }

    public BooleanProperty pursuingProperty() {
        return pursuing;
    }

    public void setPursuing(boolean pursuing) {
        this.pursuing.set(pursuing);
    }

    public String getMovePolicy() {
        return movePolicy.get();
    }

    public StringProperty movePolicyProperty() {
        return movePolicy;
    }

    public void setMovePolicy(String movePolicy) {
        this.movePolicy.set(movePolicy);
    }

}
