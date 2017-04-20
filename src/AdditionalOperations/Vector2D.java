package AdditionalOperations;

/**
 * Created by robin on 19.04.2017.
 */
public class Vector2D {

    private Position start,dest;
    private double x, y;

    public Vector2D(Position start, Position dest)   {
        this.start = start;
        this.dest = dest;
        x = dest.getxPos() - start.getxPos();
        y = dest.getyPos() - start.getyPos();
    }

    public Vector2D()   {
        this.start = null;
        this.dest = null;
        this.x = 0;
        this.y = 0;
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double dotProduct(Vector2D v1, Vector2D v2)  {
        return (v1.getX() * v2.getX()) + (v1.getY() * v2.getY());
    }
}
