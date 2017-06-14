package simulation;

import javafx.scene.shape.Line;
import pathfinding.PathVertex;
import ui.Main;

import java.util.ArrayList;
import java.util.Formatter;

public class PlannedPath {

    private int startIndex = -1;
    private int endIndex = -1;

    private ArrayList<Line> pathLines;
    private ArrayList<PathVertex> pathVertices;

    public PlannedPath() {
        pathLines = new ArrayList<>();
        pathVertices = new ArrayList<>();
    }

    public void addPathToEnd(PlannedPath path) {
        // assumes that the other path starts where this one ends
        addPathVertex(path.getPathVertex(0));
        for (int i = 0; i < path.getPathLines().size(); i++) {
            addLine(path.getPathLine(i));
            addPathVertex(path.getPathVertex(i + 1));
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

    public void addPathVertex(PathVertex pv) {
        pathVertices.add(pv);
    }

    public ArrayList<Line> getPathLines() {
        return pathLines;
    }

    public ArrayList<PathVertex> getPathVertices() {
        return pathVertices;
    }

    public Line getPathLine(int index) {
        return pathLines.get(index);
    }

    public Line getFirstPathLine() {
        return pathLines.get(0);
    }

    public Line getLastPathLine() {
        return pathLines.get(pathLines.size() - 1);
    }

    public PathVertex getPathVertex(int index) {
        return pathVertices.get(index);
    }

    public PathVertex getFirstPathVertex() {
        return pathVertices.get(0);
    }

    public PathVertex getLastPathVertex() {
        return pathVertices.get(pathVertices.size() - 1);
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

    public void draw() {
        for (Line l : pathLines) {
            Main.pane.getChildren().add(l);
        }
    }

    @Override
    public String toString() {
        Formatter f = new Formatter();
        StringBuilder result = new StringBuilder("PlannedPath [startIndex=" + startIndex + ", ");
        for (Line l : pathLines) {
            result.append(f.format("(%.3f|%.3f)-(%.3f|%.3f), ", l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY()).toString());
        }
        result.append("endIndex=").append(endIndex).append("]");
        return result.toString();
    }

}
