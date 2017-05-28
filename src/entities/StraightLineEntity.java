package entities;

import javafx.scene.shape.Polygon;
import simulation.Agent;
import simulation.MapRepresentation;

import java.util.concurrent.ThreadLocalRandom;

public class StraightLineEntity extends DistributedEntity {

    double directionX, directionY;

    public StraightLineEntity(MapRepresentation map) {
        super(map);
    }

    @Override
    public void move() {
        if (directionX == 0 && directionY == 0) {
            directionX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
            directionY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
        }

        boolean legalMove;
        double moveX, moveY;

        do {
            moveX = controlledAgent.getSpeed() * (directionX / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * 1 / 50;
            moveY = controlledAgent.getSpeed() * (directionY / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * 1 / 50;
            legalMove = map.getBorderPolygon().contains(controlledAgent.getXPos() + moveX, controlledAgent.getYPos() + moveY);
            for (Polygon p : map.getObstaclePolygons()) {
                legalMove = legalMove && !p.contains(controlledAgent.getXPos() + moveX, controlledAgent.getYPos() + moveY);
            }
            if (!legalMove) {
                directionX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
                directionY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
            }
        } while (!legalMove);

        controlledAgent.moveBy(moveX, moveY);
    }

    @Override
    public void setAgent(Agent a) {
        controlledAgent = a;
    }
}
