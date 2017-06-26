package entities.utils;

import additionalOperations.GeometryOperations;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import javafx.scene.shape.Line;

public class PathLine extends Line {

    private LineSegment lineSegment;

    public PathLine(double x1, double y1, double x2, double y2) {
        super(x1, y1, x2, y2);
        lineSegment = new LineSegment(x1, y1, x2, y2);
    }

    @Override
    public boolean contains(double x, double y) {
        return lineSegment.distance(new Coordinate(x, y)) < GeometryOperations.PRECISION_EPSILON;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathLine && ((PathLine) o).getStartX() == getStartX() && ((PathLine) o).getStartY() == getStartY() && ((PathLine) o).getEndX() == getEndX() && ((PathLine) o).getEndY() == getEndY();
    }

}
