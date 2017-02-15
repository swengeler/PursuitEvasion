package ui;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class Anchor extends Circle {

    public static final double RADIUS = 10;

    private final DoubleProperty x, y;

    public Anchor(Color color, DoubleProperty x, DoubleProperty y) {
        super(x.get(), y.get(), RADIUS);
        setFill(color.deriveColor(1, 1, 1, 0.5));
        setStroke(color);
        setStrokeWidth(2);
        setStrokeType(StrokeType.OUTSIDE);

        this.x = x;
        this.y = y;

        x.bind(centerXProperty());
        y.bind(centerYProperty());
        enableDrag();
    }

    // make a node movable by dragging it around with the mouse.
    private void enableDrag() {
        final Delta dragDelta = new Delta();
        setOnMousePressed(e -> {
            // record a delta distance for the drag and drop operation.
            dragDelta.x = getCenterX() - e.getX();
            dragDelta.y = getCenterY() - e.getY();
            getScene().setCursor(Cursor.MOVE);
        });
        setOnMouseReleased(e -> {
            getScene().setCursor(Cursor.HAND);
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

    // records relative x and y co-ordinates.
    class Delta { double x, y; }

}