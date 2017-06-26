package entities.specific;

import entities.base.DistributedEntity;
import maps.MapRepresentation;

public class StaticEntity extends DistributedEntity {

    public StaticEntity(MapRepresentation map) {
        super(map);
    }

    @Override
    public void move() {}

}
