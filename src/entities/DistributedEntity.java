package entities;

import simulation.Agent;
import simulation.MapRepresentation;

public abstract class DistributedEntity extends Entity {

    protected Agent controlledAgent;

    public DistributedEntity(MapRepresentation map) {
        super(map);
    }

    public abstract void setAgent(Agent a);

    @Override
    public boolean isActive() {
        return controlledAgent.isActive();
    }

}
