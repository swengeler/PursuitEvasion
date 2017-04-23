package simulation;

import org.jdelaunay.delaunay.geometries.DTriangle;

import java.util.ArrayList;

/**
 * A Triangulation Graph Node.
 */
public class TGNode {

    private DTriangle triangle;

    private ArrayList<TGNode> neighbours;

    public TGNode(DTriangle triangle) {
        this.triangle = triangle;
        neighbours = new ArrayList<>();
    }

    public void addNeighbour(TGNode neighbour) {
        neighbours.add(neighbour);
    }

    public void addAllNeighbours(ArrayList<TGNode> newNeighbours) {
        neighbours.addAll(newNeighbours);
    }

    public DTriangle getTriangle() {
        return triangle;
    }

    public ArrayList<TGNode> getNeighbours() {
        return neighbours;
    }

}
