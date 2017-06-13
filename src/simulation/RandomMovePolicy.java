package simulation;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class RandomMovePolicy extends MovePolicy {

    public RandomMovePolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        // update revealed part of the map
        //revealedMap.update(getSingleAgent().getXPos(), getSingleAgent().getYPos(), getSingleAgent().getTurnAngle(), getSingleAgent().getFieldOfViewAngle(), getSingleAgent().getFieldOfViewRange());

        /*
        CODED BY WINSTON 2K17
         */
        double randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1))/* * timeStep */;
        double randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) /* * timeStep */;
        double length = Math.sqrt(Math.pow(randDeltaX, 2) + Math.pow(randDeltaY, 2));
        randDeltaX /= length * 50 / getSingleAgent().getSpeed();
        randDeltaY /= length * 50 / getSingleAgent().getSpeed();
        //double randAngle = Math.atan2(randDeltaY, randDeltaX) * (180 / Math.PI);
        double randAngle = (ThreadLocalRandom.current().nextInt(-360, 360)) * getSingleAgent().getTurnSpeed() /* * timeStep */ * 1 / 1000;

        while (!map.legalPosition(getSingleAgent().getXPos() + randDeltaX, getSingleAgent().getYPos() + randDeltaY)) {
            randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1))/* * timeStep */;
            randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1))/* * timeStep */;
            length = Math.sqrt(Math.pow(randDeltaX, 2) + Math.pow(randDeltaY, 2));
            randDeltaX /= length * 50 / getSingleAgent().getSpeed();
            randDeltaY /= length * 50 / getSingleAgent().getSpeed();
            randAngle = (ThreadLocalRandom.current().nextInt(0, 360)) * getSingleAgent().getTurnSpeed() /* * timeStep */ * 1 / 1000;
        }
        return new Move(randDeltaX, randDeltaY, 0);
    }

}
