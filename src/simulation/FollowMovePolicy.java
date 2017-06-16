package simulation;

import entities.utils.ShortestPathRoadMap;

import java.util.ArrayList;

public class FollowMovePolicy extends MovePolicy {

    private ShortestPathRoadMap roadMap;

    public FollowMovePolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
        roadMap = new ShortestPathRoadMap(map);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        /*
        for (Evader e : agents) {
            measure distance;
        }
        compute path to closest one;
        follow that path;

        OR:

        for (Evader e : agents) {
            if (in view) {
                measure distance;
            }
        }
        follow closest one;
        */
        return null;
    }

}
