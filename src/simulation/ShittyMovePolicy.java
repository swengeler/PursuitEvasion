package simulation;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ShittyMovePolicy extends MovePolicy {

    public ShittyMovePolicy(Agent agent, MapRepresentation map) {
        super(agent);
        revealedMap = new RevealedMap(map.getBorderPolygon(), map.getObstaclePolygons());
    }

    @Override
    Move getNextMove(MapRepresentation map, ArrayList<Agent> agents, long timeStep) {
        /*
        CODED BY WINSTON 2K17
         */
        double randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1) - agent.getXPos()) * agent.getSpeed() * timeStep;
        double randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1) - agent.getYPos()) * agent.getSpeed() * timeStep;
        double randAngle = Math.atan2(randDeltaY, randDeltaX) * (180 / Math.PI);

        while (!map.legalPosition(agent.getXPos() + randDeltaX, agent.getYPos() + randDeltaY)) {
            randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1) - agent.getXPos()) * agent.getSpeed() * timeStep;
            randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1) - agent.getYPos()) * agent.getSpeed() * timeStep;
            randAngle = Math.atan2(randDeltaY, randDeltaX) * (180 / Math.PI);
        }

        return new Move(randDeltaX, randDeltaY, randAngle);
    }

}
