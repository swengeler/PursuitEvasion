package simulation;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by winstonnolten on 19/04/2017.
 */
public class EvaderPolicy extends MovePolicy {

    public EvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        ArrayList<Point> pursuerPoints = new ArrayList<Point>();

        for (Agent a : agents) {
            if (a.isPursuer()) {
                pursuerPoints.add(new Point((int) a.getXPos(), (int) a.getYPos()));
            }
        }

        Point evaderPoint = new Point((int) getSingleAgent().getXPos(), (int) getSingleAgent().getYPos());

        Point evaderUp = new Point((int) getSingleAgent().getXPos(), (int) getSingleAgent().getYPos() - 1);
        Point evaderDown = new Point((int) getSingleAgent().getXPos(), (int) getSingleAgent().getYPos() + 1);
        Point evaderLeft = new Point((int) getSingleAgent().getXPos() - 1, (int) getSingleAgent().getYPos());
        Point evaderRight = new Point((int) getSingleAgent().getXPos() + 1, (int) getSingleAgent().getYPos());


        Point[] evaderMoves = new Point[]{evaderUp, evaderDown, evaderLeft, evaderRight};
        double max = 0;
        int winningPoint = -1;

        for (int i = 0; i < evaderMoves.length; i++) {
            Point p = evaderMoves[i];
            double total = 0;

            for (Point pp : pursuerPoints) {
                double dist = Math.sqrt(Math.pow(pp.getX() - p.getX(), 2) + Math.pow(pp.getY() - p.getY(), 2));
                total += dist;
            }

            if (total >= max) {
                max = total;
                winningPoint = i;
            }
        }

        //check out of bounds

        if (winningPoint == 0) {
            return new Move(0, getSingleAgent().getSpeed() * 1 / 250, 0);
        } else if (winningPoint == 1) {
            return new Move(0, getSingleAgent().getSpeed() * -1 / 250, 0);
        } else if (winningPoint == 2) {
            return new Move(getSingleAgent().getSpeed() * -1 / 250, 0, 0);
        } else if (winningPoint == 3) {
            return new Move(getSingleAgent().getSpeed() * 1 / 250, 0, 0);
        } else {
            System.out.println("No valid move found");
            return null;
        }
    }
}