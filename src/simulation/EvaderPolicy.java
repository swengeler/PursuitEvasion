package simulation;

import java.util.ArrayList;
import java.awt.Point;
import java.util.List;

/**
 * Created by winstonnolten on 19/04/2017.
 */
public class EvaderPolicy extends MovePolicy {

    public EvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
        revealedMap = new RevealedMap(map.getBorderPolygon(), map.getObstaclePolygons());
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        ArrayList<Point> pursuerPoints = new ArrayList<Point>();

        for (Agent a: agents) {
            if (a.isPursuer()) {
                pursuerPoints.add(new Point((int) a.getXPos(), (int) a.getYPos()));
            }
        }

        Point evaderPoint = new Point((int) agent.getXPos(), (int) agent.getYPos());

        Point evaderUp = new Point((int) agent.getXPos(), (int) agent.getYPos()-1);
        Point evaderDown = new Point((int) agent.getXPos(), (int) agent.getYPos()+1);
        Point evaderLeft = new Point((int) agent.getXPos()-1, (int) agent.getYPos());
        Point evaderRight = new Point((int) agent.getXPos()+1, (int) agent.getYPos()-1);


        Point[] evaderMoves = new Point[]{evaderUp, evaderDown, evaderLeft, evaderRight};
        double max = 0;
        int winningPoint = -1;

        for (int i = 0; i < evaderMoves.length; i++) {
            Point p = evaderMoves[i];
            double total = 0;

            for (Point pp: pursuerPoints) {
                double dist = Math.sqrt(Math.pow(pp.getX() - p.getX(), 2), Math.pow(pp.getY() - p.getY(), 2);
                total += dist;
            }

            if (total >= max) {
                max = total;
                winningPoint = i;
            }
        }

        if (winningPoint == 0) {

        }

        return null;
    }
}