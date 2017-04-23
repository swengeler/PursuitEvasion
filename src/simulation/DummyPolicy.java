package simulation;

import java.util.ArrayList;

public class DummyPolicy extends MovePolicy {

    public DummyPolicy(Agent agent, boolean pursuing) {
        super(agent, pursuing);
        // construct tree from (simple) map
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        // if there is not "fixed plan" currently (for the round), compute a new one, i.e. a path to follow trough the map (tree)
        // the current path to follow changes if a) it is finished or b) an evader is spotted


        return new Move(0, 0, 0);
    }

}
