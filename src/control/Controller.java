package control;

import javafx.beans.property.DoubleProperty;
import simulation.Simulation;
import ui.Main;

import java.util.ArrayList;

public class Controller {

    private Main userInterface;
    private Simulation simulation;

    private ArrayList<DoubleProperty> positions;

    public void setUserInterface(Main userInterface) {
        this.userInterface = userInterface;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

}
