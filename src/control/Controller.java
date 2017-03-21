package control;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import simulation.*;
import ui.*;

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

    public static Simulation getSimulation() {
        return simulation;
    }

    public static void theBestTest(ArrayList<MapPolygon> map, ArrayList<VisualAgent> visualAgents) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (MapPolygon p : map) {
            polygons.add(p.getPolygon());
        }
        ArrayList<Polygon> subList = new ArrayList<>();
        for (int i = 1; i < map.size() - 1; i++) {
            subList.add(polygons.get(i));
        }
        MapRepresentation mapRepresentation = new GridMapRepresentation(polygons.get(0), subList);

        ArrayList<Agent> agents = new ArrayList<>();
        Agent temp;
        for (VisualAgent a : visualAgents) {
            AgentSettings s = a.getSettings();
            temp = new Agent(s.getX(), s.getY(), s.getSpeed(), s.getTurnSpeed(), s.getFieldOfViewAngle(), s.getFieldOfViewRange());
            temp.setPolicy(new ShittyMovePolicy(temp, s.isPursuing(), mapRepresentation));
            a.centerXProperty().bind(temp.xPosProperty());
            a.centerYProperty().bind(temp.yPosProperty());
            a.turnAngleProperty().bind(temp.turnAngleProperty());
            agents.add(temp);
        }

        Simulation sim = new Simulation(mapRepresentation, agents);
        setSimulation(sim);
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
            temp = new Agent(c.getCenterX(), c.getCenterY(), 100, 10, 10, 10);
            temp.setPolicy(new ShittyMovePolicy(temp, false, mapRepresentation));
            c.centerXProperty().bind(temp.xPosProperty());
            c.centerYProperty().bind(temp.yPosProperty());
            agents.add(temp);
        }
        for (Circle c : evaders) {
            temp = new Agent(c.getCenterX(), c.getCenterY(), 100, 10, 10, 10);
            temp.setPolicy(new ShittyMovePolicy(temp, false, mapRepresentation));
            c.centerXProperty().bind(temp.xPosProperty());
            c.centerYProperty().bind(temp.yPosProperty());
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