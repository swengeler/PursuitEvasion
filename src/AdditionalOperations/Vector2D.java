package AdditionalOperations;

/**
 * Created by robin on 19.04.2017.
 */
public class Vector2D {

    private Position start,dest;

    public Vector2D(Position start, Position dest)   {
        this.start = start;
        this.dest = dest;
    }

    public Vector2D()   {
        this.start = null;
        this.dest = null;
    }

    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Position getDest() {
        return dest;
    }

    public void setDest(Position dest) {
        this.dest = dest;
    }
}
