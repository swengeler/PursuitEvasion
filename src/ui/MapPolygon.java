package ui;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;

public class MapPolygon extends Polyline {

    private static ArrayList<Anchor> allAnchors = new ArrayList<>();

    private ZoomablePane parent;

    public MapPolygon(ZoomablePane parent) {
        super();
        this.parent = parent;
        setStroke(Color.BLUE);
        setFill(Color.TRANSPARENT);
        setStrokeWidth(3.0);
        setStrokeLineCap(StrokeLineCap.ROUND);
        setStrokeLineJoin(StrokeLineJoin.ROUND);
        //parent.getChildren().add(this);
    }

    public static ArrayList<Anchor> getAllAnchors() {
        return allAnchors;
    }

    public boolean lineIntersects(Line line) {
        double a1, a2, a3, a4;
        for (int i = 0; i < getPoints().size() - 2; i += 2) {
            if (!((line.getStartX() == getPoints().get(i) && line.getStartY() == getPoints().get(i + 1)) || (line.getEndX() == getPoints().get(i) && line.getEndY() == getPoints().get(i + 1)) ||
                (line.getStartX() == getPoints().get(i + 2) && line.getStartY() == getPoints().get(i + 3)) || (line.getEndX() == getPoints().get(i + 2) && line.getEndY() == getPoints().get(i + 3)))) {
                a1 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), getPoints().get(i), getPoints().get(i + 1));
                a2 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), getPoints().get(i + 2), getPoints().get(i + 3));
                if (a1 * a2 < 0) {
                    a3 = signed2DTriArea(getPoints().get(i + 2), getPoints().get(i + 3), getPoints().get(i), getPoints().get(i + 1), line.getStartX(), line.getStartY());
                    a4 = a3 + a2 - a1;
                    if (a3 * a4 < 0) {
                        return true;
                    }
                }
                //System.out.println("(" + line.getStartX() + "|" + line.getStartY() + ") to (" + line.getEndX() + "|" + line.getEndY() + ")" +
                //" not intersecting with (" + getPoints().get(i) + "|" + getPoints().get(i + 1) + ") to (" + getPoints().get(i + 2) + "|" + getPoints().get(i + 3) + ")");
            }
        }
        return false;
    }

    public Point2D lineIntersectionPoint(Line line) {
        Point2D intersectionPoint;
        double a1, a2, a3, a4, t;
        for (int i = 0; i < getPoints().size() - 2; i += 2) {
            a1 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), getPoints().get(i), getPoints().get(i + 1));
            a2 = signed2DTriArea(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(), getPoints().get(i + 2), getPoints().get(i + 3));
            if (a1 * a2 < 0) {
                a3 = signed2DTriArea(getPoints().get(i + 2), getPoints().get(i + 3), getPoints().get(i), getPoints().get(i + 1), line.getStartX(), line.getStartY());
                a4 = a3 + a2 - a1;
                if (a3 * a4 < 0) {
                    t = a3 / (a3 - a4);
                    intersectionPoint = new Point2D(line.getStartX() + t * (line.getEndX() - line.getStartX()), line.getStartY() + t * (line.getEndY() - line.getStartY()));
                    return intersectionPoint;
                }
            }
            //System.out.println("(" + line.getStartX() + "|" + line.getStartY() + ") to (" + line.getEndX() + "|" + line.getEndY() + ")" +
            //" not intersecting with (" + getPoints().get(i) + "|" + getPoints().get(i + 1) + ") to (" + getPoints().get(i + 2) + "|" + getPoints().get(i + 3) + ")");
        }
        return null;
    }

    private static double signed2DTriArea(double ax, double ay, double bx, double by, double cx, double cy) {
        return (ax - cx) * (by - cy) - (ay - cy) * (bx - cx);
    }

    public void addAnchor(Anchor a) {
        getPoints().addAll(a.getCenterX(), a.getCenterY());
        int currentSize = getPoints().size();
        a.centerXProperty().addListener((ov, oldValue, newValue) -> {
            getPoints().set(currentSize - 2, (double) newValue);
            // check for intersection?
        });
        a.centerYProperty().addListener((ov, oldValue, newValue) -> {
            getPoints().set(currentSize - 1, (double) newValue);
        });
        a.toFront();
        if (!allAnchors.contains(a)) {
            allAnchors.add(a);
            parent.getChildren().add(a);
        }
    }

    public void removeAnchor(Anchor a) {

    }

    public boolean isClosed() {
        return getPoints().size() > 2 &&
                ((double) getPoints().get(0) == getPoints().get(getPoints().size() - 2)) &&
                ((double) getPoints().get(1) == getPoints().get(getPoints().size() - 1));
    }

}
