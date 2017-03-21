package simulation;

import java.util.ArrayList;

public abstract class MovePolicy {

    protected RevealedMap revealedMap;

    protected Agent agent;

    protected boolean pursuing;

    public MovePolicy(Agent agent, boolean pursuing) {
        this.agent = agent;
        this.pursuing = pursuing;
    }

    public abstract Move getNextMove(MapRepresentation map, ArrayList<Agent> agents);

    public boolean pursuingPolicy() {
        return pursuing;
    }

    public boolean evadingPolicy() {
        return !pursuing;
    }

}
