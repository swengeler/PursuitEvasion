package experiments;

import com.vividsolutions.jts.geom.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class SquareGuardInfo implements Serializable {

    public Polygon guardedSquare;
    public ArrayList<LineString> squareSides;
    public HashMap<LineString, ArrayList<LineString>> entranceToGuarded;
    public HashMap<LineString, ArrayList<LineSegment>> guardedToSegments;
    public ArrayList<Coordinate> originalPositions;

    public SquareGuardInfo(Polygon guardedSquare, ArrayList<LineString> squareSides, HashMap<LineString, ArrayList<LineString>> entranceToGuarded, HashMap<LineString, ArrayList<LineSegment>> guardedToSegments, ArrayList<Coordinate> originalPositions) {
        this.guardedSquare = guardedSquare;
        this.squareSides = squareSides;
        this.entranceToGuarded = entranceToGuarded;
        this.guardedToSegments = guardedToSegments;
        this.originalPositions = originalPositions;
    }

}
