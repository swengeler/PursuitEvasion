package simulation;

import java.util.ArrayList;

public abstract class MovePolicy {

    protected RevealedMap revealedMap;

    protected Agent agent;

    public MovePolicy(Agent agent) {
        this.agent = agent;
    }

    abstract Move getNextMove(MapRepresentation map, ArrayList<Agent> agents, long timeStep);

}
