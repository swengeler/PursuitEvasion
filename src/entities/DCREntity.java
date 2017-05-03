package entities;

import simulation.Agent;
import simulation.MapRepresentation;

import java.util.ArrayList;

/**
 * DCR = Divide and Conquer, Randomized
 */
public class DCREntity extends CentralisedEntity {

    private Agent searcher, catcher;

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

        // five possibilities:
        // 1. no target and no agent visible
        // 2. no target but agent visible
        // 3. target but no agent visible
        // 4. target and target visible
        // 5. target and other agent visible

        // might be an idea to give individual agents specific tasks
        // e.g. the searcher just does the searcher thing to do when target not visible

        /*if (testTarget == null) {
            // have to search for target
            boolean targetFound = false;
            for (Agent a : evaders) {
                if (map.isVisible(searcher, a)) {
                    testTarget = a;
                    targetFound = true;
                    break;
                }
            }
            if (!targetFound) {
                // keep searching
            } else {
                // pursue target
            }
        } else {
            if (map.isVisible(searcher, testTarget)) {
                // perform lion's move
            } else {
                // get to pocket or smth
            }
        }*/
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
