package entities;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import java.util.ArrayList;

public class GuardedSquare {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private ArrayList<LineSegment> squareSides;
    private ArrayList<LineSegment> actualSegments;
    private ArrayList<Integer> parallelInformation;
    private LinearRing square;

    private ArrayList<Coordinate> originalCornerGuardPositions;
    private ArrayList<Coordinate> originalLineGuardPositions;
    private ArrayList<LineSegment> currentCornerGuardSegments;

    private ArrayList<int[]> cornerGuardAssignmentIndeces;
    private ArrayList<Integer> lineGuardAssignmentIndeces;

    private LineString crossingLine, entranceLine;
    private Point currentEvaderPos, lastEvaderPos;

    public GuardedSquare() {
        currentEvaderPos = new Point(new CoordinateArraySequence(1), geometryFactory);
        lastEvaderPos = new Point(new CoordinateArraySequence(1), geometryFactory);
        crossingLine = new LineString(new CoordinateArraySequence(new Coordinate[]{lastEvaderPos.getCoordinate(), currentEvaderPos.getCoordinate()}), geometryFactory);
        entranceLine = new LineString(new CoordinateArraySequence(2), geometryFactory);
    }

    public void updateEvaderPosition(int x, int y) {
        lastEvaderPos.getCoordinate().x = currentEvaderPos.getX();
        lastEvaderPos.getCoordinate().y = currentEvaderPos.getY();
        currentEvaderPos.getCoordinate().x = x;
        currentEvaderPos.getCoordinate().y = y;
        if (square.contains(currentEvaderPos) && !square.contains(lastEvaderPos)) {
            // need to start following the thing
            // find out which line was crossed
            for (int i = 0; i < 4; i++) {
                entranceLine.getStartPoint().getCoordinate().x = squareSides.get(i).getCoordinate(0).x;
                entranceLine.getStartPoint().getCoordinate().y = squareSides.get(i).getCoordinate(0).y;
                entranceLine.getEndPoint().getCoordinate().x = squareSides.get(i).getCoordinate(1).x;
                entranceLine.getEndPoint().getCoordinate().y = squareSides.get(i).getCoordinate(1).y;
                if (entranceLine.intersects(crossingLine)) {
                    // assign lines/line segments to guards
                    // assume that the first four squareSides entries are ordered as follow:
                    // separating line: 0, perpendicular line at startpoint: 1, perpendicular line at endpoint: 2, parallel line: 3
                    // for any guard that has only one line segment to worry about, only need to take care of guarding that one

                    // start with parallel line
                    LineSegment parallel = getParallel(i);
                    int[] guardIndecesParallel = getCornerGuardIndeces(parallel);
                    if (guardIndecesParallel[0] != -1) {
                        // find parallel segment
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesParallel[0], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesParallel[0], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[1]));
                        }
                    }
                    if (guardIndecesParallel[1] != -1) {
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesParallel[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesParallel[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[1]));
                        }
                    }

                    LineSegment perpendicular1 = getPerpendicular1(i);
                    int[] guardIndecesPerp1 = getCornerGuardIndeces(perpendicular1);
                    if (guardIndecesPerp1[0] != -1 && guardIndecesPerp1[0] != guardIndecesParallel[0] && guardIndecesPerp1[0] != guardIndecesParallel[1]) {
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[1]));
                        }
                    } else if (guardIndecesPerp1[1] != -1 && guardIndecesPerp1[1] != guardIndecesParallel[0] && guardIndecesPerp1[1] != guardIndecesParallel[1]) {
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[1]));
                        }
                    }

                    LineSegment perpendicular2 = getPerpendicular1(i);
                    int[] guardIndecesPerp2 = getCornerGuardIndeces(perpendicular2);
                    if (guardIndecesPerp2[0] != -1 && guardIndecesPerp2[0] != guardIndecesParallel[0] && guardIndecesPerp2[0] != guardIndecesParallel[1]) {
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[1]));
                        }
                    } else if (guardIndecesPerp2[1] != -1 && guardIndecesPerp2[1] != guardIndecesParallel[0] && guardIndecesPerp2[1] != guardIndecesParallel[1]) {
                        if (squareSides.get(parallelInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[1]));
                        }
                    }
                    break;
                }
            }
        } else if (square.contains(currentEvaderPos)) {
            // moved within the square, need to adjust target points to move to
        }
    }

    public void initEvaderPosition(int x, int y) {
        currentEvaderPos.getCoordinate().x = x;
        currentEvaderPos.getCoordinate().y = y;
    }

    private int[] getCornerGuardIndeces(LineSegment ls) {
        int[] result = new int[]{-1, -1};
        for (int i = 0; i < originalCornerGuardPositions.size(); i++) {
            if (originalCornerGuardPositions.get(i).equals2D(ls.getCoordinate(0))) {
                result[0] = i;
            }
            if (originalCornerGuardPositions.get(i).equals2D(ls.getCoordinate(1))) {
                result[1] = i;
            }
        }
        return result;
    }

    private LineSegment getParallel(LineSegment line) {
        return getParallel(squareSides.indexOf(line));
    }

    private LineSegment getParallel(int index) {
        if (index == 0) {
            return squareSides.get(3);
        } else if (index == 1) {
            return squareSides.get(2);
        } else if (index == 2) {
            return squareSides.get(1);
        } else if (index == 3) {
            return squareSides.get(0);
        }
        return null;
    }

    private LineSegment getPerpendicular1(LineSegment line) {
        return getPerpendicular1(squareSides.indexOf(line));
    }

    private LineSegment getPerpendicular1(int index) {
        if (index == 0) {
            return squareSides.get(1);
        } else if (index == 1) {
            return squareSides.get(3);
        } else if (index == 2) {
            return squareSides.get(0);
        } else if (index == 3) {
            return squareSides.get(2);
        }
        return null;
    }

    private LineSegment getPerpendicular2(LineSegment line) {
        return getPerpendicular2(squareSides.indexOf(line));
    }

    private LineSegment getPerpendicular2(int index) {
        if (index == 0) {
            return squareSides.get(2);
        } else if (index == 1) {
            return squareSides.get(0);
        } else if (index == 2) {
            return squareSides.get(3);
        } else if (index == 3) {
            return squareSides.get(1);
        }
        return null;
    }

}
