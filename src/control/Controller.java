package control;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import simulation.*;
import ui.Main;
import ui.MapPolygon;

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

    public static void betterTest(ArrayList<MapPolygon> map, ArrayList<Circle> pursuers, ArrayList<Circle> evaders) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (MapPolygon p : map) {
            polygons.add(p.getPolygon());
        }
        ArrayList<Polygon> subList = new ArrayList<>();
        for (int i = 1; i < map.size(); i++) {
            subList.add(polygons.get(i));
        }
        MapRepresentation mapRepresentation = new GridMapRepresentation(polygons.get(0), subList);

        ArrayList<Agent> agents = new ArrayList<>();
        Agent temp;
        for (Circle c : pursuers) {
            temp = new Agent(c.getCenterX(), c.getCenterY(), 50, 10, 10, 10);
            temp.setPolicy(new ShittyMovePolicy(temp, mapRepresentation));
            c.centerXProperty().bind(temp.getXPosProperty());
            c.centerYProperty().bind(temp.getYPosProperty());
            agents.add(temp);
        }
        for (Circle c : evaders) {
            temp = new Agent(c.getCenterX(), c.getCenterY(), 50, 10, 10, 10);
            temp.setPolicy(new ShittyMovePolicy(temp, mapRepresentation));
            c.centerXProperty().bind(temp.getXPosProperty());
            c.centerYProperty().bind(temp.getYPosProperty());
            agents.add(temp);
        }

        Simulation sim = new Simulation(mapRepresentation, agents);
    }

    public static void test(ArrayList<Circle> pursuers, ArrayList<Circle> evaders) {
        Simulation sim = new Simulation(pursuers, evaders);
    }

    public static void testBack() {

    }

}