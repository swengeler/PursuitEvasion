package simulation;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;

public class SimpleEvaderPolicy extends MovePolicy {

    /*This simple evader policy steers evaders away from pursuers within a certain range (set by maxSeparationDistance)

    TODO:
    Come up with a way to handle bounds
    In the case of multiple evaders, we need to consider them too since we can't collide with them either.
     */

    public SimpleEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        double maxSeperationDistance = 150;
        double deltaX = 0;
        double deltaY = 0;
        int numberOfPursuers = 0;

        for (Agent agent : agents) {
            if (agent.isPursuer()) {
                double dist = Math.sqrt(Math.pow(agent.getXPos() - getSingleAgent().getXPos(), 2) + Math.pow(agent.getYPos() - getSingleAgent().getYPos(), 2));
                if (dist < maxSeperationDistance) {
                    deltaX += (agent.getXPos() - getSingleAgent().getXPos());
                    deltaY += (agent.getYPos() - getSingleAgent().getYPos());
                }
                numberOfPursuers++;
            }
        }

        //check out of bounds
        if (!map.legalPosition(getSingleAgent().getXPos() - deltaX/numberOfPursuers * getSingleAgent().getSpeed() * 1/4000, getSingleAgent().getYPos() -deltaY/numberOfPursuers * getSingleAgent().getSpeed() * 1/4000)) {
            System.out.println("Move impossible: out of bounds");
            return new Move(0, 0, 0);
            //TODO: fix
        }

        //is 1/250, 1/4000 etc just a parameter we can set?
        return new Move(-deltaX/numberOfPursuers * getSingleAgent().getSpeed() * 1/4000, -deltaY/numberOfPursuers * getSingleAgent().getSpeed() * 1/4000, 0);
    }
}