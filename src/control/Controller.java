package control;

import javafx.scene.shape.Circle;
import simulation.Simulation;
import ui.Main;

import java.util.ArrayList;

public class Controller {

    private static Main userInterface;
    private static Simulation simulation;

    public static void setUserInterface(Main userInterfaceInput) {
        userInterface = userInterfaceInput;
    }

    public static void setSimulation(Simulation simulationInput) {
        simulation = simulationInput;
    }

    public static void test(ArrayList<Circle> pursuers, ArrayList<Circle> evaders) {
        Simulation sim = new Simulation(pursuers, evaders);
    }

    public static void testBack() {

    }

}