package entities;

import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

public abstract class CentralisedEntity extends Entity {

    protected ArrayList<Agent> availableAgents;
    protected int requiredAgents;

    protected CentralisedEntity(MapRepresentation map) {
        super(map);
    }

    public abstract int totalRequiredAgents();
    public abstract int remainingRequiredAgents();
    public abstract void addAgent(Agent a);

    @Override
    public boolean isActive() {
        for (Agent a : availableAgents) {
            if (a.isActive()) {
                return true;
            }
        }
        return false;
    }

}
