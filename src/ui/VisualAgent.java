package ui;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
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

        getChildren().addAll(agentBody);

        enableDrag();
    }

    private void enableDrag() {
        final Delta dragDelta = new Delta();
        setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            // record a delta distance for the drag and drop operation.
            dragDelta.x = getCenterX() - e.getX();
            dragDelta.y = getCenterY() - e.getY();
            getScene().setCursor(Cursor.MOVE);
            dragDelta.lastLegalX = getCenterX();
            dragDelta.lastLegalY = getCenterY();
        });
        setOnMouseReleased(e -> {
            getScene().setCursor(Cursor.HAND);
            System.out.println(Main.mapPolygons.size());
            if (!Main.mapPolygons.get(0).contains(e.getX(), e.getY())) {
                setCenterX(dragDelta.lastLegalX);
                setCenterY(dragDelta.lastLegalY);
                return;
            }
            for (int i = 1; i < Main.mapPolygons.size(); i++) {
                if (Main.mapPolygons.get(i).contains(e.getX(), e.getY())) {
                    setCenterX(dragDelta.lastLegalX);
                    setCenterY(dragDelta.lastLegalY);
                    return;
                }
            }
        });
        setOnMouseDragged(e -> {
            double newX = e.getX() + dragDelta.x;
            if (newX > 0 && newX < ((Pane) getParent()).getWidth()) {
                setCenterX(newX);
            } else if (newX > 0) {
                setCenterX(((Pane) getParent()).getWidth());
            } else {
                setCenterX(0);
            }
            double newY = e.getY() + dragDelta.y;
            if (newY > 0 && newY < ((Pane) getParent()).getHeight()) {
                setCenterY(newY);
            } else if (newY > 0) {
                setCenterY(((Pane) getParent()).getHeight());
            } else {
                setCenterY(0);
            }
        });
        setOnMouseEntered(e -> {
            if (!e.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.HAND);
            }
        });
        setOnMouseExited(e -> {
            if (!e.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
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

    public double getCenterX() {
        return agentBody.getCenterX();
    }

    public double getCenterY() {
        return agentBody.getCenterY();
    }

    public void setCenterX(double centerX) {
        settings.setXPos(centerX);
    }

    public void setCenterY(double centerY) {
        settings.setYPos(centerY);
    }

}
