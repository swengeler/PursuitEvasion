package simulation;

import javafx.scene.shape.Circle;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Simulation {

    private Timer simulationTimer;

    private MapRepresentation map;
    private ArrayList<Agent> agents;

    private boolean simulationRunning;

    private long timeStep = 10;

    public Simulation(MapRepresentation map, ArrayList<Agent> agents) {
        this.map = map;
        this.agents = agents;
        timerSetup();
    }

    public Simulation(ArrayList<Circle> pursuers, ArrayList<Circle> evaders) {
        simulationTimer = FxTimer.runPeriodically(Duration.ofMillis(timeStep), () -> {
            for (Circle c : pursuers) {
                c.setCenterX(c.getCenterX() + ThreadLocalRandom.current().nextInt(-9, 10));
                c.setCenterY(c.getCenterY() + ThreadLocalRandom.current().nextInt(-9, 10));
            }
            for (Circle c : evaders) {
                c.setCenterX(c.getCenterX() + ThreadLocalRandom.current().nextInt(-9, 10));
                c.setCenterY(c.getCenterY() + ThreadLocalRandom.current().nextInt(-9, 10));
            }
        });
    }

    private void timerSetup() {
        simulationTimer = FxTimer.runPeriodically(Duration.ofMillis(timeStep), () -> {
            System.out.println("New timer tick");
            for (Agent a : agents) {
                // should probably also have stuff like time elapsed since last step
                // so the agent knows how far they can move
                // maybe better?: define a static delay between steps that is enforced
                a.move(map, agents, timeStep);
                // update UI
            }
            /*boolean simulationOver = checkSimulationOver();
            if (simulationOver) {
                // do things
                simulationRunning = false;
            }*/
        });
    }

    public void pause() {
        simulationTimer.stop();
    }

    public void unPause() {
        simulationTimer.restart();
    }

    public void setTimeStep(long timeStep) {
        this.timeStep = timeStep;
        simulationTimer.stop();
        timerSetup();
    }

    public boolean checkSimulationOver() {
        return false;
    }

}
