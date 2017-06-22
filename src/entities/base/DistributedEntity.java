package entities.base;

import maps.MapRepresentation;
import simulation.Agent;

import java.util.ArrayList;

public abstract class DistributedEntity extends Entity {

    // TODO: maybe change this to a list (e.g. two agents might be controlled together but disconnected from other agents)
    protected Agent controlledAgent;

    public DistributedEntity(MapRepresentation map) {
        super(map);
    }

    public void setAgent(Agent a) {
        controlledAgent = a;
    }

    @Override
    public boolean isActive() {
        return controlledAgent.isActive();
    }

    @Override
    public ArrayList<Agent> getControlledAgents() {
        ArrayList<Agent> result = new ArrayList<>();
        result.add(controlledAgent);
        return result;
    }

}
