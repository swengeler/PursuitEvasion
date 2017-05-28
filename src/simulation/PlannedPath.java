package simulation;

import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.Formatter;

public class PlannedPath {

    private int startIndex = -1;
    private int endIndex = -1;

    private ArrayList<Line> pathLines;

    public PlannedPath() {
        pathLines = new ArrayList<>();
    }

    public void addPathToEnd(PlannedPath path) {
        // assumes that the other path starts where this one ends
        for (Line l : path.getPathLines()) {
            addLine(l);
        }
        setEndIndex(path.getEndIndex());
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void addInitLine(Line line) {
        pathLines.add(0, line);
    }

    public void addLine(Line line) {
        pathLines.add(line);
    }

    public ArrayList<Line> getPathLines() {
        return pathLines;
    }

    public Line getPathLine(double xPos, double yPos) {
        return null;
    }

    public double getStartX() {
        return pathLines.get(0).getStartX();
    }

    public double getStartY() {
        return pathLines.get(0).getStartY();
    }

    public double getEndX() {
        return pathLines.get(pathLines.size() - 1).getEndX();
    }

    public double getEndY() {
        return pathLines.get(pathLines.size() - 1).getEndY();
    }

    public double getTotalLength() {
        double length = 0;
        for (Line l : pathLines) {
            length += Math.sqrt(Math.pow(l.getEndX() - l.getStartX(), 2) + Math.pow(l.getEndY() - l.getStartY(), 2));
        }
        return length;
    }

    public int pathLength() {
        return pathLines.size() - 1;
    }

    @Override
    public String toString() {
        Formatter f = new Formatter();
        StringBuilder result = new StringBuilder("PlannedPath [startIndex=" + startIndex + ", ");
        for (Line l : pathLines) {
            result.append(f.format("(%.3f|%.3f)-(%.3f|%.3f), ", l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY()));
        }
        result.append("endIndex=").append(endIndex).append("]");
        return result.toString();
    }

}
