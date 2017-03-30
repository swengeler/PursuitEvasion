package simulation;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class RandomMovePolicy extends MovePolicy {

    public RandomMovePolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
        revealedMap = new RevealedMap(map.getBorderPolygon(), map.getObstaclePolygons());
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        // update revealed part of the map
        //revealedMap.update(agent.getXPos(), agent.getYPos(), agent.getTurnAngle(), agent.getFieldOfViewAngle(), agent.getFieldOfViewRange());

        /*
        CODED BY WINSTON 2K17
         */
        double randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() /* * timeStep */ * 1 / 250;
        double randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() /* * timeStep */ * 1 / 250;
        //double randAngle = Math.atan2(randDeltaY, randDeltaX) * (180 / Math.PI);
        double randAngle = (ThreadLocalRandom.current().nextInt(-360, 360)) * agent.getTurnSpeed() /* * timeStep */ * 1 / 1000;

        while (!map.legalPosition(agent.getXPos() + randDeltaX, agent.getYPos() + randDeltaY)) {
            randDeltaX = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() /* * timeStep */ * 1 / 250;
            randDeltaY = (ThreadLocalRandom.current().nextInt(-10, 10 + 1)) * agent.getSpeed() /* * timeStep */ * 1 / 250;
            randAngle = (ThreadLocalRandom.current().nextInt(0, 360)) * agent.getTurnSpeed() /* * timeStep */ * 1 / 1000;
        }
        System.out.println(randDeltaX);
        return new Move(randDeltaX, randDeltaY, randAngle);
    }

}
