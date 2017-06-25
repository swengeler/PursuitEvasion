package entities.specific;

import additionalOperations.Tuple;
import entities.base.Entity;
import entities.base.PartitioningEntity;
import entities.guarding.GuardManager;
import entities.guarding.TriangleVisionGuardManager;
import entities.utils.PathLine;
import entities.utils.PlannedPath;
import experiments.DCRVStats;
import experiments.PartitioningEntityRequirements;
import javafx.scene.Group;
import maps.MapRepresentation;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import simulation.Agent;
import simulation.TraversalHandler;
import ui.Main;

import java.util.ArrayList;

/**
 * DCRV = Divide and Conquer, Randomised, Vision-Based
 */
public class DCRVEntity extends PartitioningEntity {

    public DCRVStats stats;
    private PartitioningEntityRequirements requirements;

    private TraversalHandler traversalHandler;

    private Agent searcher;
    private PlannedPath currentSearcherPath;
    private ArrayList<PathLine> pathLines;
    private int searcherPathLineCounter;

    private Group catchGraphics;
    private Group guardGraphics;

    public DCRVEntity(MapRepresentation map, PartitioningEntityRequirements requirements) {
        this(map);
        if (requirements.isConfigured()) {
            requiredAgents = requirements.requiredAgents;
            componentBoundaryLines = requirements.componentBoundaryLines;
            componentBoundaryEdges = requirements.componentBoundaryEdges;
            componentBoundaryShapes = requirements.componentBoundaryShapes;
            separatingEdges = requirements.separatingEdges;
            separatingLines = requirements.separatingLines;
            traversalHandler = requirements.traversalHandler;
            for (GuardManager gm : requirements.guardManagers) {
                guardManagers.add(new TriangleVisionGuardManager(((TriangleVisionGuardManager) gm).getMap(), ((TriangleVisionGuardManager) gm).getOriginalPositions().get(0).x, ((TriangleVisionGuardManager) gm).getOriginalPositions().get(0).y));
            }
        } else {
            this.requirements = requirements;
            computeRequirements();
        }
    }

    public DCRVEntity(MapRepresentation map) {
        super(map);
        computeRequirements();
        catchGraphics = new Group();
        guardGraphics = new Group();
        Main.pane.getChildren().addAll(catchGraphics, guardGraphics);
    }

    @Override
    protected void determineTarget() {
        System.out.println("wat3: " + this.stats.getCounter());
        outer:
        for (Entity e : map.getEvadingEntities()) {
            if (e.isActive()) {
                for (Agent a : e.getControlledAgents()) {
                    if (a.isActive()) {
                        target = a;
                        for (GuardManager gm : guardManagers) {
                            gm.initTargetPosition(target);
                        }
                        break outer;
                    }
                }
            }
        }
        System.out.println("wat4: " + this.stats.getCounter());
    }

    @Override
    protected void doPrecedingOperations() {
        System.out.println("wat5: " + this.stats.getCounter());
    }

    @Override
    protected void doGuardOperations() {
        System.out.println("wat6: " + this.stats.getCounter());
    }

    @Override
    protected void doSearchAndCatchOperations() {
        System.out.println("Check");
        System.out.println("wat7: " + this.stats.getCounter());

        if (currentSearcherPath == null) {
            try {
                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                searcherPathLineCounter = 0;
            } catch (DelaunayError e) {
                e.printStackTrace();
            }
        }

        if (traversalHandler.getNodeIndex(searcher.getXPos(), searcher.getYPos()) == currentSearcherPath.getEndIndex()) {
            // end of path reached, compute new path
            try {
                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
            } catch (DelaunayError e) {
                e.printStackTrace();
            }
            searcherPathLineCounter = 0;
            if (stats != null) {
                stats.increaseNrLeafRuns();
            }
        }

        // move searcher and catcher using same paths
        pathLines = currentSearcherPath.getPathLines();
        length = Math.sqrt(Math.pow(pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX(), 2) + Math.pow(pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY(), 2));
        deltaX = (pathLines.get(searcherPathLineCounter).getEndX() - pathLines.get(searcherPathLineCounter).getStartX()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
        deltaY = (pathLines.get(searcherPathLineCounter).getEndY() - pathLines.get(searcherPathLineCounter).getStartY()) / length * searcher.getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
        if (pathLines.get(searcherPathLineCounter).contains(searcher.getXPos() + deltaX, searcher.getYPos() + deltaY)) {
            // move along line
            searcher.moveBy(deltaX, deltaY);
        } else {
            // move to end of line
            searcher.moveBy(pathLines.get(searcherPathLineCounter).getEndX() - searcher.getXPos(), pathLines.get(searcherPathLineCounter).getEndY() - searcher.getYPos());
            searcherPathLineCounter++;
        }

        if (stats != null) {
            System.out.println(stats.getCounter());
            stats.increaseNrSteps();
        }

        // check whether target is visible
        for (Entity e : map.getEvadingEntities()) {
            if (e.isActive()) {
                for (Agent a1 : e.getControlledAgents()) {
                    if (a1.isActive()) {
                        for (Agent a2 : getControlledAgents()) {
                            if (map.isVisible(a1, a2)) {
                                if (stats != null) {
                                    System.out.println("HELL-o");
                                    stats.setCaughtBySearcher(a2.equals(searcher));
                                    stats.targetCaught();
                                }
                                a1.setActive(false);
                                target = null;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doSucceedingOperations() {
    }

    @Override
    protected void assignTasks() {
        long before = System.currentTimeMillis();
        super.assignTasks();
        for (Agent a : availableAgents) {
            if (!guards.contains(a)) {
                searcher = a;
                break;
            }
        }
        System.out.println("Time to assign DCRVEntity tasks: " + (System.currentTimeMillis() - before));
    }

    @Override
    protected void computeRequirements() {
        long before = System.currentTimeMillis();
        // build needed data structures and analyse map to see how many agents are required
        try {
            // computing the triangulation of the given map
            // what is returned are the triangles of the map itself and those of the holes
            Tuple<ArrayList<DTriangle>, ArrayList<DTriangle>> triangles = triangulate(map);
            ArrayList<DTriangle> nodes = triangles.getFirst();
            ArrayList<DTriangle> holeTriangles = triangles.getSecond();

            // computing adjacency between the triangles in the map -> modelling it as a graph
            Tuple<int[][], int[]> matrices = computeAdjacency(nodes);
            int[][] originalAdjacencyMatrix = matrices.getFirst();
            int[] degreeMatrix = matrices.getSecond();

            // grouping hole triangles by holes (through adjacency)
            ArrayList<ArrayList<DTriangle>> holes = computeHoles(holeTriangles);

            // compute separating triangles and the updated adjacency matrix
            Triplet<ArrayList<DTriangle>, ArrayList<DEdge>, int[][]> separation = computeSeparatingTriangles(nodes, holes, originalAdjacencyMatrix, degreeMatrix);
            ArrayList<DTriangle> separatingTriangles = separation.getValue0();
            int[][] spanningTreeAdjacencyMatrix = separation.getValue2();

            // compute the simply connected components in the graph
            ArrayList<DTriangle> componentNodes = new ArrayList<>();
            for (DTriangle dt : nodes) {
                if (!separatingTriangles.contains(dt)) {
                    componentNodes.add(dt);
                }
            }
            Tuple<ArrayList<ArrayList<DTriangle>>, int[]> componentInfo = computeConnectedComponents(nodes, componentNodes, spanningTreeAdjacencyMatrix);
            ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = componentInfo.getFirst();

            guardManagers = computeGuardManagers(separatingTriangles);

            traversalHandler = new TraversalHandler(shortestPathRoadMap, nodes, simplyConnectedComponents, spanningTreeAdjacencyMatrix);
            traversalHandler.separatingTriangleBased(separatingTriangles);

            for (GuardManager gm : guardManagers) {
                requiredAgents += gm.totalRequiredGuards();
            }
            requiredAgents++;
            System.out.println("\nrequiredAgents: " + requiredAgents);

            if (requirements != null) {
                requirements.configure(requiredAgents, componentBoundaryLines, componentBoundaryEdges, componentBoundaryShapes, separatingEdges, separatingLines, guardManagers, traversalHandler);
            }
        } catch (DelaunayError error) {
            error.printStackTrace();
        }
        System.out.println("Time to compute DCRVEntity requirements: " + (System.currentTimeMillis() - before));
    }

    public void trackStats(DCRVStats stats) {
        this.stats = stats;
        System.out.println("wat: " + this.stats.getCounter());
    }

    private ArrayList<GuardManager> computeGuardManagers(ArrayList<DTriangle> separatingTriangles) throws DelaunayError {
        ArrayList<GuardManager> triangleVisionGuardManager = new ArrayList<>(separatingTriangles.size());

        for (DTriangle dt : separatingTriangles) {
            triangleVisionGuardManager.add(new TriangleVisionGuardManager(map, dt.getBarycenter().getX(), dt.getBarycenter().getY()));
        }

        return triangleVisionGuardManager;
    }

}
