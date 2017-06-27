package entities.shadows;

import entities.base.CentralisedEntity;
import maps.MapRepresentation;
import shadowPursuit.ShadowGraph;

/**
 * Created by jonty on 27/06/2017.
 */
public abstract class shadowEntity extends CentralisedEntity {


    protected shadowEntity(MapRepresentation map) {
        super(map);
    }


    @Override
    public void move() {

    }

}
