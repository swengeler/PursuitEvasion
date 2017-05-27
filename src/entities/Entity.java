package entities;

import pathfinding.ShortestPathRoadMap;
import simulation.MapRepresentation;

public abstract class Entity {

    public static double UNIVERSAL_SPEED_MULTIPLIER = 1.0 / 50.0;

    protected MapRepresentation map;
    protected ShortestPathRoadMap shortestPathRoadMap;

    protected Entity(MapRepresentation map) {
        this.map = map;
        shortestPathRoadMap = new ShortestPathRoadMap(map);
    }

    public abstract void move();

    public abstract boolean isActive();

}