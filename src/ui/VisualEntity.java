package ui;

import entities.base.Entity;

import java.util.ArrayList;

public class VisualEntity {

    private Entity correspondingEntity;
    private ArrayList<VisualAgent> visualAgents;

    public VisualEntity(Entity correspondingEntity) {
        this.correspondingEntity = correspondingEntity;
        visualAgents = new ArrayList<>();
    }

    public void highlight() {

    }

    public ArrayList<VisualAgent> getVisualAgents() {
        return visualAgents;
    }

}
