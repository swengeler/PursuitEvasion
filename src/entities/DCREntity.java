package entities;

import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

/**
 * DCR = Divide and Conquer, Randomized
 */
public class DCREntity extends CentralisedEntity {

    public DCREntity(MapRepresentation map) {
        super(map);
        availableAgents = new ArrayList<>();
        computeRequiredAgents();
    }

    @Override
    public void move() {
        // TODO: For now implement the 2 agent randomised approach for a simply-connected environment
        // if the evader is not visible, use the 2 agents to search for the pursuer together
        // if the evader was located before and just needs to be followed and caught:
        // use one agent as searcher and the other as catcher
    }

    @Override
    public int totalRequiredAgents() {
        return requiredAgents;
    }

    @Override
    public int remainingRequiredAgents() {
        return requiredAgents - availableAgents.size();
    }

    @Override
    public void addAgent(Agent a) {
        availableAgents.add(a);
    }

    private void computeRequiredAgents() {
        // build needed data structures and analyse map to see how many agents are required
        requiredAgents = 2;
    }

    public static Agent testTarget;

}
