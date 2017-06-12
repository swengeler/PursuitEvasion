package simulation;

import java.util.ArrayList;

public abstract class MovePolicy {

    protected ArrayList<Agent> controlledAgents;

    protected boolean pursuing;

    protected MovePolicy(ArrayList<Agent> controlledAgents, boolean pursuing) {
        this.controlledAgents = controlledAgents;
        this.pursuing = pursuing;
    }

    protected MovePolicy(Agent singleAgent, boolean pursuing) {
        controlledAgents = new ArrayList<>();
        controlledAgents.add(singleAgent);
        this.pursuing = pursuing;
    }

    public abstract Move getNextMove(MapRepresentation map, ArrayList<Agent> agents);

    public boolean pursuingPolicy() {
        return pursuing;
    }

    public boolean evadingPolicy() {
        return !pursuing;
    }

    protected Agent getSingleAgent() {
        if (controlledAgents.size() == 1) {
            return controlledAgents.get(0);
        }
        return null;
    }

}
