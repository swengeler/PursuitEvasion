package ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import simulation.AgentSettings;

public class VisualAgent extends Group {

    private AgentSettings settings;

    private Circle agentBody;
    private Arc fieldOfView;

    public VisualAgent() {
        init();
    }

    public VisualAgent(double xPos, double yPos) {
        init();
        settings.setXPos(xPos);
        settings.setYPos(yPos);
    }

    private void init() {
        settings = new AgentSettings();

        agentBody = new Circle(5);
        agentBody.setStroke(Color.BLACK);
        agentBody.setFill(Color.LIGHTBLUE);

        agentBody.centerXProperty().bind(settings.xPosProperty());
        agentBody.centerYProperty().bind(settings.yPosProperty());

        fieldOfView = new Arc();
        fieldOfView.setFill(Color.AQUA.deriveColor(1, 1, 1, 0.3));
        fieldOfView.setType(ArcType.ROUND);

        fieldOfView.centerXProperty().bind(agentBody.centerXProperty());
        fieldOfView.centerYProperty().bind(agentBody.centerYProperty());
        fieldOfView.radiusXProperty().bind(settings.fieldOfViewRangeProperty());
        fieldOfView.radiusYProperty().bind(settings.fieldOfViewRangeProperty());
        fieldOfView.lengthProperty().bind(settings.fieldOfViewAngleProperty());
        settings.turnAngleProperty().addListener((ov, oldValue, newValue) -> {
            fieldOfView.setStartAngle(settings.getTurnAngle() - (settings.getFieldOfViewAngle() / 2));
        });
        fieldOfView.setStartAngle(settings.getTurnAngle() - (settings.getFieldOfViewAngle() / 2));

        /*fieldOfView.startAngleProperty().bind(turnAngle);
        fieldOfView.radiusXProperty().bind(fieldOfViewRange);
        fieldOfView.radiusYProperty().bind(fieldOfViewRange);
        fieldOfView.lengthProperty().bind(fieldOfViewAngle);*/

        getChildren().addAll(fieldOfView, agentBody);
    }

    public void adoptSettings(AgentSettings agentSettings) {
        settings.setXPos(agentSettings.getXPos());
        settings.setYPos(agentSettings.getYPos());
        settings.setSpeed(agentSettings.getSpeed());
        settings.setTurnSpeed(agentSettings.getTurnSpeed());
        settings.setTurnAngle(agentSettings.getTurnAngle());
        settings.setFieldOfViewAngle(agentSettings.getFieldOfViewAngle());
        settings.setFieldOfViewRange(agentSettings.getFieldOfViewRange());

        settings.setPursuing(agentSettings.isPursuing());
        if (settings.isPursuing()) {
            agentBody.setFill(Color.ORANGERED);
            fieldOfView.setFill(Color.ORANGERED.deriveColor(1, 1, 1, 0.3));
        } else {
            agentBody.setFill(Color.LIGHTBLUE);
            fieldOfView.setFill(Color.AQUA.deriveColor(1, 1, 1, 0.3));
        }
        settings.setMovePolicy(agentSettings.getMovePolicy());

        fieldOfView.setStartAngle(settings.getTurnAngle() - (settings.getFieldOfViewAngle() / 2));
    }

    public Circle getAgentBody() {
        return agentBody;
    }

    public AgentSettings getSettings() {
        return settings;
    }

}
