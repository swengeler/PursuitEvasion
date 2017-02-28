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
        double randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() * timeStep * 1 / 1000;
        double randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() * timeStep * 1 / 1000;
        //double randAngle = Math.atan2(randDeltaY, randDeltaX) * (180 / Math.PI);
        double randAngle = (ThreadLocalRandom.current().nextInt(-360, 360)) * agent.getTurnSpeed() * timeStep * 1 / 1000;

        while (!map.legalPosition(agent.getXPos() + randDeltaX, agent.getYPos() + randDeltaY)) {
            randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() * timeStep * 1 / 1000;
            randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() * timeStep * 1 / 1000;
            randAngle = (ThreadLocalRandom.current().nextInt(0, 360)) * agent.getTurnSpeed() * timeStep * 1 / 1000;
        }

        return new Move(randDeltaX, randDeltaY, randAngle);
    }

}
