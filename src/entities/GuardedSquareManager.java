package entities;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import simulation.Agent;

import java.util.ArrayList;

import static java.lang.Double.NaN;

public class GuardedSquareManager {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private ArrayList<LineSegment> squareSides; // the four sides of the guardedSquare
    private ArrayList<LineSegment> actualSegments; // the actual line segments that guards are assigned to and have to guard
    private ArrayList<Integer> collinearInformation; // pointers from the above to the collinear side of the guardedSquare
    private LinearRing guardedSquare; // the square shape, used for covers() checks

    private ArrayList<Coordinate> originalCornerGuardPositions; // used to return to these points and to identify which guards should be used for which line segment that is crossed
    private ArrayList<Coordinate> originalLineGuardPositions; // similar for line guards
    private ArrayList<LineSegment> currentCornerGuardSegments; // line guards only have one segment to guard, corner guards can have two

    private ArrayList<int[]> cornerGuardAssignmentIndeces; // assignment of corner guards to line segments (i.e. pointers)
    private ArrayList<Integer> lineGuardAssignmentIndeces; // similar for line guards

    private LineString crossingLine, entranceLine;
    private Point currentEvaderPos, lastEvaderPos;

    public GuardedSquareManager() {
        currentEvaderPos = new Point(new CoordinateArraySequence(1), geometryFactory);
        lastEvaderPos = new Point(new CoordinateArraySequence(1), geometryFactory);
        crossingLine = new LineString(new CoordinateArraySequence(new Coordinate[]{lastEvaderPos.getCoordinate(), currentEvaderPos.getCoordinate()}), geometryFactory);
        entranceLine = new LineString(new CoordinateArraySequence(2), geometryFactory);
    }

    public void updateEvaderPosition(Agent a) {
        lastEvaderPos.getCoordinate().x = currentEvaderPos.getX();
        lastEvaderPos.getCoordinate().y = currentEvaderPos.getY();
        currentEvaderPos.getCoordinate().x = a.getXPos();
        currentEvaderPos.getCoordinate().y = a.getYPos();
        if (guardedSquare.covers(currentEvaderPos) && !guardedSquare.covers(lastEvaderPos)) {
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
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesParallel[0], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesParallel[0], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[0])[1]));
                        }
                    }
                    if (guardIndecesParallel[1] != -1) {
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesParallel[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesParallel[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesParallel[1])[1]));
                        }
                    }

                    LineSegment perpendicular1 = getPerpendicular1(i);
                    int[] guardIndecesPerp1 = getCornerGuardIndeces(perpendicular1);
                    if (guardIndecesPerp1[0] != -1 && guardIndecesPerp1[0] != guardIndecesParallel[0] && guardIndecesPerp1[0] != guardIndecesParallel[1]) {
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[1]));
                        }
                    } else if (guardIndecesPerp1[1] != -1 && guardIndecesPerp1[1] != guardIndecesParallel[0] && guardIndecesPerp1[1] != guardIndecesParallel[1]) {
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp1[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp1[1])[1]));
                        }
                    }

                    LineSegment perpendicular2 = getPerpendicular1(i);
                    int[] guardIndecesPerp2 = getCornerGuardIndeces(perpendicular2);
                    if (guardIndecesPerp2[0] != -1 && guardIndecesPerp2[0] != guardIndecesParallel[0] && guardIndecesPerp2[0] != guardIndecesParallel[1]) {
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[1]));
                        }
                    } else if (guardIndecesPerp2[1] != -1 && guardIndecesPerp2[1] != guardIndecesParallel[0] && guardIndecesPerp2[1] != guardIndecesParallel[1]) {
                        if (squareSides.get(collinearInformation.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0])).equals(parallel)) {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[0]));
                        } else {
                            currentCornerGuardSegments.set(guardIndecesPerp2[1], actualSegments.get(cornerGuardAssignmentIndeces.get(guardIndecesPerp2[1])[1]));
                        }
                    }
                    break;
                }
            }

            // make first move towards the projection point or capture the evader if possible
        } else if (guardedSquare.covers(currentEvaderPos)) {
            // moved within the square, need to adjust target points to move to, or capture evader if possible
        } else {
            // not inside the square, return to previous positions, except if evader is in catchable range
        }

    }

    public void initEvaderPosition(Agent a) {
        currentEvaderPos.getCoordinate().x = a.getXPos();
        currentEvaderPos.getCoordinate().y = a.getYPos();
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
