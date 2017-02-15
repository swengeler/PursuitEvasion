package ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;

public class ZoomablePane extends Pane {

    private DoubleProperty zoomScale = new SimpleDoubleProperty(1.0);

    public ZoomablePane() {
        super();
        scaleXProperty().bind(zoomScale);
        scaleYProperty().bind(zoomScale);
        setStyle("-fx-background-color: #ffffff");
    }

    public double getScale() {
        return zoomScale.get();
    }

    public void setScale(double scale) {
        zoomScale.set(scale);
    }

    public void setPivot(double x, double y) {
        setTranslateX(getTranslateX() - x);
        setTranslateY(getTranslateY() - y);

        if (getTranslateX() > ((getScale() - 1) * (getWidth() / 2))) {
            setTranslateX((getScale() - 1) * (getWidth() / 2));
        }
        if (getTranslateY() > ((getScale() - 1) * (getHeight() / 2))) {
            setTranslateY((getScale() - 1) * (getHeight() / 2));
        }

        if (getTranslateX() < -((getScale() - 1) * (getWidth() / 2))) {
            setTranslateX(-((getScale() - 1) * (getWidth() / 2)));
        }
        if (getTranslateY() < -((getScale() - 1) * (getHeight() / 2))) {
            setTranslateY(-((getScale() - 1) * (getHeight() / 2)));
        }
    }

}
