package ui;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class PaneEvents {

    private static final double MAX_SCALE = 10.0;
    private static final double MIN_SCALE = 1.0;

    private DragContext sceneDragContext = new DragContext();

    private ZoomablePane pane;

    public PaneEvents(ZoomablePane pane) {
        this.pane = pane;
    }

    public EventHandler<MouseEvent> getOnMousePressedEventHandler() {
        return onMousePressedEventHandler;
    }

    public EventHandler<MouseEvent> getOnMouseDraggedEventHandler() {
        return onMouseDraggedEventHandler;
    }

    public EventHandler<ScrollEvent> getOnScrollEventHandler() {
        return onScrollEventHandler;
    }

    private EventHandler<MouseEvent> onMousePressedEventHandler = event -> {
        if (!event.isSecondaryButtonDown()) {
            return;
        }

        sceneDragContext.mouseAnchorX = event.getSceneX();
        sceneDragContext.mouseAnchorY = event.getSceneY();

        sceneDragContext.translateAnchorX = pane.getTranslateX();
        sceneDragContext.translateAnchorY = pane.getTranslateY();
    };

    private EventHandler<MouseEvent> onMouseDraggedEventHandler = event -> {
        if (!event.isSecondaryButtonDown()) {
            return;
        }

        pane.setTranslateX(2 + (sceneDragContext.translateAnchorX + event.getSceneX() - sceneDragContext.mouseAnchorX));
        pane.setTranslateY(2 + (sceneDragContext.translateAnchorY + event.getSceneY() - sceneDragContext.mouseAnchorY));

        if (pane.getTranslateX() > ((pane.getScale() - 1) * (pane.getWidth() / 2))) {
            pane.setTranslateX((pane.getScale() - 1) * (pane.getWidth() / 2));
        }
        if (pane.getTranslateY() > ((pane.getScale() - 1) * (pane.getHeight() / 2))) {
            pane.setTranslateY((pane.getScale() - 1) * (pane.getHeight() / 2));
        }

        if (pane.getTranslateX() < -((pane.getScale() - 1) * (pane.getWidth() / 2))) {
            pane.setTranslateX(-((pane.getScale() - 1) * (pane.getWidth() / 2)));
        }
        if (pane.getTranslateY() < -((pane.getScale() - 1) * (pane.getHeight() / 2))) {
            pane.setTranslateY(-((pane.getScale() - 1) * (pane.getHeight() / 2)));
        }

        event.consume();
    };

    private EventHandler<ScrollEvent> onScrollEventHandler = event -> {
        double delta = 1.2;

        double scale = pane.getScale();
        double oldScale = scale;

        if (event.getDeltaY() < 0) {
            scale /= delta;
        } else {
            scale *= delta;
        }

        scale = clamp(scale, MIN_SCALE, MAX_SCALE);

        double f = (scale / oldScale) - 1;

        double dx = (event.getSceneX() - (pane.getBoundsInParent().getWidth() / 2 + pane.getBoundsInParent().getMinX()));
        double dy = (event.getSceneY() - (pane.getBoundsInParent().getHeight() / 2 + pane.getBoundsInParent().getMinY()));

        pane.setScale(scale);

        // note: pivot value must be untransformed, i. e. without scaling
        pane.setPivot(f * dx, f * dy);

        event.consume();
    };

    private static double clamp(double value, double min, double max) {
        if (Double.compare(value, min) < 0) {
            return min;
        }
        if (Double.compare(value, max) > 0) {
            return max;
        }
        return value;
    }

}