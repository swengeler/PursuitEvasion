package simulation;

import entities.CentralisedEntity;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static additionalOperations.GeometryOperations.polyToPoints;

public class Simulation {

    private Timer simulationTimer;

    private MapRepresentation map;
    private ArrayList<Agent> agents;

    private long timeStep = 300;

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
            for (Agent a : agents) {
                // should probably also have stuff like time elapsed since last step
                // so the controlledAgents knows how far they can move
                // maybe better?: define a static delay between steps that is enforced
                if (a.isActive()) {
                    a.move(map, agents);
                }
                // update UI
            }

            if (testCentralisedEntity != null) {
                testCentralisedEntity.move();
            }

            // check whether any new evaders have been captured
            for (Agent a1 : agents) {
                if (a1.isEvader()) {
                    for (Agent a2 : agents) {
                        if (a2.isPursuer() && a2.inRange(a1.getXPos(), a1.getYPos())) {
                            // remove captured controlledAgents
                            a1.setActive(false);
                        }
                    }
                }
            }

            // check whether all evaders are captured
            boolean simulationOver = true;
            for (Agent a : agents) {
                if (a.isEvader() && a.isActive()) {
                    simulationOver = false;
                }
            }

            if (simulationOver) {
                simulationTimer.stop();
                // send message to UI
                System.out.println("All evaders captured");
            }
        });
    }

    public void pause() {
        simulationTimer.stop();
    }

    public void unPause() {
        simulationTimer.restart();
    }

    public Agent getAgent(double x, double y) {
        for (Agent a : agents) {
            if (a.getXPos() == x && a.getYPos() == y) {
                return a;
            }
        }
        return null;
    }

    public void setTimeStep(long timeStep) {
        this.timeStep = timeStep;
        simulationTimer.stop();
        timerSetup();
    }

    public static CentralisedEntity testCentralisedEntity;

}
