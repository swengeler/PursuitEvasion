package simulation;

import java.util.ArrayList;

public class Simulation implements Runnable {

    private MapRepresentation map;
    private ArrayList<Agent> agents;

    private boolean simulationRunning;

    public Simulation(MapRepresentation map) {
        this.map = map;
    }

    public void run() {
        simulationRunning = true;
        while (simulationRunning) {
            for (Agent a : agents) {
                // should probably also have stuff like time elapsed since last step
                // so the agent knows how far they can move
                // maybe better?: define a static delay between steps that is enforced
                a.move(map);
                boolean simulationOver = checkSimulationOver();
                if (simulationOver) {
                    // do things
                    simulationRunning = false;
                }
            }
        }
    }

    public void pause() {
        simulationRunning = true;
    }

    public void unPause() {
        run();
    }

    public boolean checkSimulationOver() {
        return false;
    }

}
