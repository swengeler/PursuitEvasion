package entities;

import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

public abstract class CentralisedEntity extends Entity {

    protected ArrayList<Agent> availableAgents;
    protected int requiredAgents;

    protected CentralisedEntity(MapRepresentation map) {
        super(map);
        availableAgents = new ArrayList<>();
    }

    public int totalRequiredAgents() {
        return requiredAgents;
    }

    public int remainingRequiredAgents() {
        return requiredAgents - availableAgents.size();
    }

    @Override
    public boolean isActive() {
        for (Agent a : availableAgents) {
            if (a.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<Agent> getControlledAgents() {
        return availableAgents;
    }

    public void addAgent(Agent a) {
        availableAgents.add(a);
    }

}
