package entities;

import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

public abstract class CentralisedEntity extends Entity {

    protected ArrayList<Agent> controlledAgents;

    public CentralisedEntity(MapRepresentation map) {
        super(map);
    }

}
