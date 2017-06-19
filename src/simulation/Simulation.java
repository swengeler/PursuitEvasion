package simulation;

import entities.base.CentralisedEntity;
import javafx.collections.ObservableList;
import javafx.scene.shape.Circle;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Simulation {

    private static Timer simulationTimer;

    public static void masterPause() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
    }

    private MapRepresentation map;
    private ArrayList<Agent> agents;

    private long timeStep = 300;

    //extra boolean to printout polygons for shadowtests
    private final boolean polyPrint = true;

    public Simulation(MapRepresentation map, ArrayList<Agent> agents) {
        this.map = map;
        this.agents = agents;
        timerSetup();


        //Want to print out certai polygosns to know whether shadowpoins are calulated correctly
        if (polyPrint) {
            ObservableList<Double> envPoints = map.getBorderPolygon().getPoints();
            System.out.println("Environment points");
            for (Double entry : envPoints) {
                System.out.print(entry + ",");
            }
            System.out.println("\n");

            for (int i = 0; i < map.getObstaclePolygons().size(); i++) {
                System.out.println("Obstacle: " + (i + 1));
                ObservableList<Double> obstpoints = map.getObstaclePolygons().get(i).getPoints();
                for (Double entry : obstpoints) {
                    System.out.print(entry + " ,");
                }
            }
            System.out.println("\n");
            for (Agent agent : agents) {
                System.out.println("Agent -> X = " + agent.getXPos() + "\t Y = " + agent.getYPos());
            }

        }

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
