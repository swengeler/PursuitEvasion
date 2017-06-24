package entities.base;

import entities.utils.ShortestPathRoadMap;
import maps.MapRepresentation;
import simulation.Agent;

import java.util.ArrayList;

public abstract class Entity {

    public static double UNIVERSAL_SPEED_MULTIPLIER = 1.0 / 50.0;

    protected static MapRepresentation map;
    protected static ShortestPathRoadMap shortestPathRoadMap;

    protected Entity(MapRepresentation map) {
        if (Entity.map == null) {
            Entity.map = map;
        }
        if (Entity.shortestPathRoadMap == null) {
            long before = System.currentTimeMillis();
            Entity.shortestPathRoadMap = new ShortestPathRoadMap(map);
            System.out.println("Time to generate shortest path map: " + (System.currentTimeMillis() - before));
        }
    }

    public abstract void move();

    public abstract boolean isActive();

    public abstract ArrayList<Agent> getControlledAgents();

    public static void reset() {
        map = null;
        shortestPathRoadMap = null;
    }

}