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

}
