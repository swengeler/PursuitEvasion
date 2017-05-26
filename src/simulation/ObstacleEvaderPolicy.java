package simulation;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;

public class ObstacleEvaderPolicy extends MovePolicy {

    //is point in polygon? ray casting
    //compute 'velocity vector'
    //velo vector - polygon centroid
    //normalize * max avoidance
    //add to flocking?


    public ObstacleEvaderPolicy(Agent agent, boolean pursuing, MapRepresentation map) {
        super(agent, pursuing);
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        Agent evader = getSingleAgent();
        ArrayList<Polygon> obstacles = map.getObstaclePolygons();

        for (Polygon p: obstacles) {

        }


        return new Move(0, 0, 0);
    }

}
