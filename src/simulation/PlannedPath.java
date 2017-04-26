package simulation;

import javafx.scene.shape.Line;

import java.util.ArrayList;

public class PlannedPath {

    private int startIndex, endIndex;

    private ArrayList<Line> pathLines;

    public PlannedPath() {
        pathLines = new ArrayList<>();
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

}
