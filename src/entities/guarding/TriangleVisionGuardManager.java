package entities.guarding;

import com.vividsolutions.jts.geom.Coordinate;
import maps.MapRepresentation;
import simulation.Agent;

import java.util.ArrayList;

public class TriangleVisionGuardManager implements GuardManager {

    private MapRepresentation map;
    private Agent guard;
    private ArrayList<Coordinate> originalPosition;

    public TriangleVisionGuardManager(MapRepresentation map, double positionX, double positionY) {
        this.map = map;
        originalPosition = new ArrayList<>(1);
        originalPosition.add(new Coordinate(positionX, positionY));
    }

    @Override
    public void initTargetPosition(Agent a) {
        if (map.isVisible(a, guard)) {
            a.setActive(false);
        }
    }

    @Override
    public void updateTargetPosition(Agent a) {
        if (map.isVisible(a, guard)) {
            a.setActive(false);
        }
    }

    @Override
    public void assignGuards(ArrayList<Agent> guards) {
        guard = guards.get(0);
    }

    @Override
    public int totalRequiredGuards() {
        return 1;
    }

    @Override
    public ArrayList<Coordinate> getOriginalPositions() {
        return originalPosition;
    }

}
