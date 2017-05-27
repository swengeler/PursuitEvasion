package entities;

import simulation.Agent;
import simulation.MapRepresentation;

public class RandomEntity extends DistributedEntity {

    public RandomEntity(MapRepresentation map) {
        super(map);
    }

    @Override
    public void move() {
        double randDeltaX = Math.random() - 0.5;
        double randDeltaY = Math.random() - 0.5;
        double length = Math.sqrt(Math.pow(randDeltaX, 2) + Math.pow(randDeltaY, 2));
        randDeltaX /= length;
        randDeltaY /= length;
        randDeltaX *= UNIVERSAL_SPEED_MULTIPLIER * controlledAgent.getSpeed();
        randDeltaY *= UNIVERSAL_SPEED_MULTIPLIER * controlledAgent.getSpeed();

        while (!map.legalPosition(controlledAgent.getXPos() + randDeltaX, controlledAgent.getYPos() + randDeltaY)) {
            randDeltaX = Math.random() - 0.5;
            randDeltaY = Math.random() - 0.5;
            length = Math.sqrt(Math.pow(randDeltaX, 2) + Math.pow(randDeltaY, 2));
            randDeltaX /= length;
            randDeltaY /= length;
            randDeltaX *= UNIVERSAL_SPEED_MULTIPLIER * controlledAgent.getSpeed();
            randDeltaY *= UNIVERSAL_SPEED_MULTIPLIER * controlledAgent.getSpeed();
        }

        controlledAgent.moveBy(randDeltaX, randDeltaY);
    }

    @Override
    public void setAgent(Agent a) {
        controlledAgent = a;
    }

}
