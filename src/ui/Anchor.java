package ui;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class Anchor extends Circle {

    public static final double RADIUS = 10;

    private final DoubleProperty x, y;

    private double lastLegalX, lastLegalY;

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
            lastLegalX = getCenterX();
            lastLegalY = getCenterY();
        });
        setOnMouseReleased(e -> {
            getScene().setCursor(Cursor.HAND);
            for (MapPolygon mp1 : Main.mapPolygons) {
                for (MapPolygon mp2 : Main.mapPolygons) {
                    // check for intersection between lines
                    for (int i = 0; i < mp2.getPoints().size() - 2; i += 2) {
                        if (mp1.lineIntersects(new Line(mp2.getPoints().get(i), mp2.getPoints().get(i + 1), mp2.getPoints().get(i + 2), mp2.getPoints().get(i + 3)))) {
                            setCenterX(lastLegalX);
                            setCenterY(lastLegalY);
                            return;
                        }
                    }
                }
            }
            for (int i = 1; i < Main.mapPolygons.size(); i++) {
                for (int j = 0; j < Main.mapPolygons.get(i).getPoints().size(); j += 2) {
                    if (!Main.mapPolygons.get(0).contains(Main.mapPolygons.get(i).getPoints().get(j), Main.mapPolygons.get(i).getPoints().get(j + 1))) {
                        setCenterX(lastLegalX);
                        setCenterY(lastLegalY);
                        return;
                    }
                }
            }
        });
        setOnMouseDragged(e -> {
            /*for (MapPolygon mp1 : Main.mapPolygons) {
                for (MapPolygon mp2 : Main.mapPolygons) {
                    // check for intersection between lines
                    for (int i = 0; i < mp2.getPoints().size() - 2; i += 2) {
                        if (mp1.lineIntersects(new Line(mp2.getPoints().get(i), mp2.getPoints().get(i + 1), mp2.getPoints().get(i + 2), mp2.getPoints().get(i + 3)))) {
                            System.out.println("e.getXPos() = " + e.getXPos() + ", lastLegalX = " + lastLegalX);
                            System.out.println("e.getYPos() = " + e.getYPos() + ", lastLegalY = " + lastLegalY);
                            setCenterX(lastLegalX - dragDelta.x);
                            setCenterY(lastLegalY - dragDelta.y);
                            return;
                        }
                    }
                }
            }*/

            /*boolean anchorBelongsToBorder = false;
            for (int i = 0; i < Main.mapPolygons.get(0).getPoints().size(); i += 2) {
                if (getCenterX() == Main.mapPolygons.get(0).getPoints().get(i) && getCenterY() == Main.mapPolygons.get(0).getPoints().get(i + 1)) {
                    anchorBelongsToBorder = true;
                    break;
                }
            }
            if (!anchorBelongsToBorder && !Main.mapPolygons.get(0).contains(e.getXPos(), e.getYPos())) {
                //Point2D intersectionPoint = Main.mapPolygons.get(0).lineIntersectionPoint(new Line(getCenterX() - 0.1 * (e.getXPos() - getCenterX()), getCenterY() - 0.1 * (e.getYPos() - getCenterY()), e.getXPos(), e.getYPos()));
                //setCenterX(intersectionPoint.getXPos());
                //setCenterY(intersectionPoint.getYPos());
                return;
            }*/
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
            //lastLegalX = getCenterX();
            //lastLegalY = getCenterY();
            //System.out.println("lastLegalX = " + lastLegalX + ", lastLegalY = " + lastLegalY);
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
    class Delta {
        double x, y;
    }

}