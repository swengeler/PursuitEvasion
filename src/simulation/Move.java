package simulation;

public class Move {

    private double xDelta, yDelta;
    private double turnDelta;

    public Move(double xDelta, double yDelta, double turnDelta) {
        this.xDelta = xDelta;
        this.yDelta = yDelta;
        this.turnDelta = turnDelta;
    }

    public double getXDelta() {
        return xDelta;
    }

    public double getYDelta() {
        return yDelta;
    }

    public double getTurnDelta() {
        return turnDelta;
    }

}
