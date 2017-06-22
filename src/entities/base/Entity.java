package entities.base;

import entities.utils.ShortestPathRoadMap;
import maps.MapRepresentation;
import simulation.Agent;

import java.util.ArrayList;

public abstract class Entity {

    public static double UNIVERSAL_SPEED_MULTIPLIER = 1.0 / 50.0;

    protected MapRepresentation map;
    protected ShortestPathRoadMap shortestPathRoadMap;

    protected Entity(MapRepresentation map) {
        this.map = map;
        ShortestPathRoadMap.SHOW_ON_CANVAS = false;
        shortestPathRoadMap = new ShortestPathRoadMap(map);
        ShortestPathRoadMap.SHOW_ON_CANVAS = false;
    }

    public abstract void move();

    public abstract boolean isActive();

    public abstract ArrayList<Agent> getControlledAgents();

}