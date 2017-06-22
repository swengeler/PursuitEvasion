package simulation;

import maps.MapRepresentation;

import java.util.ArrayList;

public class FlockingEvaderPolicy extends MovePolicy {

    /*This simple evader policy steers evaders away from pursuers within a certain range (set by maxSeparationDistance)
    it also steers evaders towards the center of the pursuers if they are in a wider range (set by maxCohesionDistance)*/

    public FlockingEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        double maxSeperationDistance = 150;
        //separation -> move 180 degrees from the average position of the pursuers within maxSeperationDistance

        double maxCohesionDistance = 250;
        //cohesion -> move towards the center of the pursuers within maxCohesionDistance but not within maxSeperationDistance

        double deltaX = 0;
        double deltaY = 0;
        //the x and y position to go to

        int numberOfSeparationPursuers = 0;
        //number of pursuers within maxSeperationDistance

        int numberOfCohesionPursuers = 0;
        //number of pursuer within maxCohesionDistance but not within maxSeperationDistance

        Agent evader = getSingleAgent();
        //to make things clearer

        for (Agent pursuer : agents) {
            //cycle through all agents

            if (pursuer.isPursuer()) {
                //if it is a pursuer

                double dist = Math.sqrt(Math.pow(pursuer.getXPos() - evader.getXPos(), 2) + Math.pow(pursuer.getYPos() - evader.getYPos(), 2));
                //calculate euclidean distance

                if (dist <= maxSeperationDistance) {
                    //if distance is within maxSeperationDistance, do seperation calculations

                    deltaX += (pursuer.getXPos() - evader.getXPos());
                    deltaY += (pursuer.getYPos() - evader.getYPos());
                    numberOfSeparationPursuers++;
                } else if (dist <= maxCohesionDistance) {
                    //if distance is within maxCohesionDistance, do cohesion calculations

                    deltaX += pursuer.getXPos();
                    deltaY += pursuer.getYPos();
                    numberOfCohesionPursuers++;
                }

            }

        }

        if (numberOfSeparationPursuers != 0) {
            //if there are seperation pursuers, do further calculations (normalizing, reversing (180 degrees))

            deltaX = -deltaX / numberOfSeparationPursuers;
            deltaY = -deltaY / numberOfSeparationPursuers;
        }

        if (numberOfCohesionPursuers != 0) {
            //if there are cohesion pursuers, do further calculations (normalizing, ??)

            deltaX /= numberOfCohesionPursuers;
            deltaY /= numberOfCohesionPursuers;

            deltaX = (deltaX - evader.getXPos());
            deltaY = (deltaY - evader.getYPos());
        }

        double length = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        deltaX /= length;
        deltaY /= length;


        //check out of bounds, for now we just don't do anything if the move would be out of bounds
        if (!map.legalPosition(evader.getXPos() + deltaX * evader.getSpeed() * 1 / 250, evader.getYPos() + deltaY * evader.getSpeed() * 1 / 250)) {
            System.out.println("Move impossible: out of bounds");
            return new Move(0, 0, 0);
        }

        //if there are no seperation and no cohesion pursuers near, do nothing (for now)
        //we could do straightlinepolicy, randommovepolicy or something like that here
        if (deltaX == 0 && deltaY == 0) {
            return new Move(0, 0, 0);
        } else {
            return new Move(deltaX * evader.getSpeed() * 1 / 250, deltaY * evader.getSpeed() * 1 / 250, 0);
        }
    }
}