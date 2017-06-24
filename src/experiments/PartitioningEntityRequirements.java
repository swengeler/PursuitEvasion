package experiments;

import entities.guarding.GuardManager;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import simulation.TraversalHandler;

import java.util.ArrayList;

public class PartitioningEntityRequirements {

    public int requiredAgents;
    public ArrayList<ArrayList<Line>> componentBoundaryLines;
    public ArrayList<ArrayList<DEdge>> componentBoundaryEdges;
    public ArrayList<Shape> componentBoundaryShapes;
    public ArrayList<DEdge> separatingEdges;
    public ArrayList<Line> separatingLines;
    public ArrayList<GuardManager> guardManagers;
    public TraversalHandler traversalHandler;

    private boolean isConfigured;

    public void configure(int requiredAgents, ArrayList<ArrayList<Line>> componentBoundaryLines, ArrayList<ArrayList<DEdge>> componentBoundaryEdges, ArrayList<Shape> componentBoundaryShapes, ArrayList<DEdge> separatingEdges, ArrayList<Line> separatingLines, ArrayList<GuardManager> guardManagers, TraversalHandler traversalHandler) {
        this.requiredAgents = requiredAgents;
        this.componentBoundaryLines = componentBoundaryLines;
        this.componentBoundaryEdges = componentBoundaryEdges;
        this.componentBoundaryShapes = componentBoundaryShapes;
        this.separatingEdges = separatingEdges;
        this.separatingLines = separatingLines;
        this.guardManagers = guardManagers;
        this.traversalHandler = traversalHandler;
        isConfigured = true;
    }

    /*
    number of pursuers needed
    separatingLines
    separatingEdges
    separatingTriangles
    componentBoundaryLines
    componentBoundaryShapes
    guardManagers
    (gSqrIntersectingTriangles)
    nodes
    simplyConnectedComponents
    spanningTreeAdjacencyMatrix
    reconnectedComponents
    reconnectedAdjacencyMatrix
    restrictedShortestPathRoadMap
     */

    public boolean isConfigured() {
        return isConfigured;
    }

}
