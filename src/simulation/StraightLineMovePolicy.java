package simulation;

import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class StraightLineMovePolicy extends MovePolicy {

    double directionX, directionY;

    public StraightLineMovePolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        if (directionX == 0 && directionY == 0) {
            directionX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
            directionY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
        }

        boolean legalMove;
        double moveX, moveY;

        do {
            moveX = getSingleAgent().getSpeed() * (directionX / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * 1 / 50;
            moveY = getSingleAgent().getSpeed() * (directionY / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * 1 / 50;
            legalMove = map.getBorderPolygon().contains(getSingleAgent().getXPos() + moveX, getSingleAgent().getYPos() + moveY);
            for (Polygon p : map.getObstaclePolygons()) {
                legalMove = legalMove && !p.contains(getSingleAgent().getXPos() + moveX, getSingleAgent().getYPos() + moveY);
            }
            if (!legalMove) {
                directionX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
                directionY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
            }
        } while (!legalMove);

        return new Move(moveX, moveY, 1);
    }

}
