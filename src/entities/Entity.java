package entities;

import pathfinding.ShortestPathRoadMap;
import simulation.MapRepresentation;

public abstract class Entity {

    protected MapRepresentation map;
    protected ShortestPathRoadMap paths;

    public Entity(MapRepresentation map) {
        this.map = map;
        paths = new ShortestPathRoadMap(map);
    }

    public abstract void move();

}
