package entities.guarding;

import com.vividsolutions.jts.geom.Coordinate;
import simulation.Agent;

import java.util.ArrayList;

public interface GuardManager {

    void initTargetPosition(Agent a);

    void updateTargetPosition(Agent a);

    void assignGuards(ArrayList<Agent> guards);

    int totalRequiredGuards();

    ArrayList<Coordinate> getOriginalPositions();

}
