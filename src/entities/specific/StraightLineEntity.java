package entities.specific;

import entities.base.DistributedEntity;
import entities.base.Entity;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

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

        double moveX, moveY;

        do {
            moveX = controlledAgent.getSpeed() * (directionX / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * Entity.UNIVERSAL_SPEED_MULTIPLIER;
            moveY = controlledAgent.getSpeed() * (directionY / Math.sqrt(Math.pow(directionX, 2) + Math.pow(directionY, 2))) * Entity.UNIVERSAL_SPEED_MULTIPLIER;

            directionX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
            directionY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1));
        } while (!map.legalPosition(controlledAgent.getXPos() + moveX, controlledAgent.getYPos() + moveY));

        controlledAgent.moveBy(moveX, moveY);
    }

}
