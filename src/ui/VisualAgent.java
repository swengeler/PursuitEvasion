package ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import simulation.AgentSettings;

public class VisualAgent extends Group {

    private DoubleProperty turnAngle;
    private DoubleProperty fieldOfViewAngle;
    private DoubleProperty fieldOfViewRange;
    private AgentSettings settings;

    private Circle agentBody;
    private Arc fieldOfView;

    public VisualAgent() {
        turnAngle = new SimpleDoubleProperty(0);
        fieldOfViewAngle = new SimpleDoubleProperty(45);
        fieldOfViewRange = new SimpleDoubleProperty(100);

        init();
    }

    public VisualAgent(double xPos, double yPos) {
        turnAngle = new SimpleDoubleProperty(0);
        fieldOfViewAngle = new SimpleDoubleProperty(45);
        fieldOfViewRange = new SimpleDoubleProperty(100);

        init();

        fieldOfView.centerXProperty().bind(agentBody.centerXProperty());
        fieldOfView.centerYProperty().bind(agentBody.centerYProperty());

        agentBody.centerXProperty().setValue(xPos);
        agentBody.centerYProperty().setValue(yPos);

        settings = new AgentSettings(100, 40);
        settings.setX(xPos);
        settings.setY(yPos);
        settings.setFieldOfViewAngle(fieldOfViewAngle.get());
        settings.setFieldOfViewRange(fieldOfViewRange.get());
    }

    private void init() {
        agentBody = new Circle(5);
        agentBody.setStroke(Color.BLACK);
        agentBody.setFill(Color.LIGHTBLUE);

        fieldOfView = new Arc();
        fieldOfView.startAngleProperty().bind(turnAngle);
        fieldOfView.radiusXProperty().bind(fieldOfViewRange);
        fieldOfView.radiusYProperty().bind(fieldOfViewRange);
        fieldOfView.lengthProperty().bind(fieldOfViewAngle);
        //fieldOfView.setStroke(Color.AQUA);
        fieldOfView.setFill(Color.AQUA.deriveColor(1, 1, 1, 0.3));
        fieldOfView.setType(ArcType.ROUND);

        getChildren().addAll(fieldOfView, agentBody);
    }

    public void setSettings(AgentSettings settings) {
        this.settings = settings;
    }

    public AgentSettings getSettings() {
        return settings;
    }

    public Arc getFieldOfView() {
        return fieldOfView;
    }

    public double getCenterX() {
        return agentBody.centerXProperty().get();
    }

    public DoubleProperty centerXProperty() {
        return agentBody.centerXProperty();
    }

    public double getCenterY() {
        return agentBody.centerYProperty().get();
    }

    public DoubleProperty centerYProperty() {
        return agentBody.centerYProperty();
    }

    public double getTurnAngle() {
        return turnAngle.get();
    }

    public DoubleProperty turnAngleProperty() {
        return turnAngle;
    }

    public double getFieldOfViewAngle() {
        return fieldOfViewAngle.get();
    }

    public DoubleProperty fieldOfViewAngleProperty() {
        return fieldOfViewAngle;
    }

    public double getFieldOfViewRange() {
        return fieldOfViewRange.get();
    }

    public DoubleProperty fieldOfViewRangeProperty() {
        return fieldOfViewRange;
    }

    public Circle getAgentBody() {
        return agentBody;
    }

}
