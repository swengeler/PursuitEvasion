package entities.specific;

import additionalOperations.Tuple;
import com.vividsolutions.jts.geom.Coordinate;
import entities.base.Entity;
import entities.base.PartitioningEntity;
import entities.guarding.GuardManager;
import entities.guarding.TriangleVisionGuardManager;
import entities.utils.PathLine;
import entities.utils.PlannedPath;
import experiments.*;
import javafx.scene.Group;
import maps.MapRepresentation;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import simulation.*;
import ui.Main;

import java.util.ArrayList;

/**
 * DCRV = Divide and Conquer, Randomised, Vision-Based
 */
public class DCRVEntity extends PartitioningEntity {

    private DCRVStats stats;
    public int evaderCounter = 0;
    private PartitioningEntityRequirements requirements;

    private TraversalHandler traversalHandler;

    private Agent searcher;
    private PlannedPath currentSearcherPath;
    private ArrayList<PathLine> pathLines;
    private int searcherPathLineCounter;

    private ArrayList<ArrayList<Coordinate>> triangleGuardOriginalPositions;

    private Group catchGraphics;
    private Group guardGraphics;

    public DCRVEntity(MapRepresentation map, PartitioningEntityRequirements requirements, ArrayList<ArrayList<Coordinate>> triangleGuardOriginalPositions) {
        super(map);
        catchGraphics = new Group();
        guardGraphics = new Group();
        Main.pane.getChildren().addAll(catchGraphics, guardGraphics);

        this.triangleGuardOriginalPositions = triangleGuardOriginalPositions;
        if (requirements.isConfigured()) {
            System.out.println("Check");
            requiredAgents = requirements.requiredAgents;
            componentBoundaryLines = requirements.componentBoundaryLines;
            componentBoundaryEdges = requirements.componentBoundaryEdges;
            componentBoundaryShapes = requirements.componentBoundaryShapes;
            separatingEdges = requirements.separatingEdges;
            separatingLines = requirements.separatingLines;
            traversalHandler = requirements.traversalHandler;
            guardManagers = new ArrayList<>();
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
        //System.out.println("wat3: " + evaderCounter);
        /*outer:
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
        }*/
        //System.out.println("wat4: " + evaderCounter);
    }

    @Override
    protected void doPrecedingOperations() {
        //System.out.println("wat5: " + evaderCounter);
    }

    @Override
    protected void doGuardOperations() {
        //System.out.println("wat6: " + evaderCounter);
    }

    @Override
    protected void doSearchAndCatchOperations() {
        //System.out.println("wat7: " + evaderCounter);

        if (currentSearcherPath == null) {
            try {
                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                searcherPathLineCounter = 0;
                if (currentSearcherPath == null) {
                    AdaptedSimulation.masterPause("DCRVEntity");
                    ExperimentConfiguration.interruptCurrentRun();
                    return;
                }
                if (stats != null) {
                    //stats.nrLeafRuns[evaderCounter]++;
                    stats.increaseNrLeafRuns();
                }
            } catch (DelaunayError e) {
                e.printStackTrace();
            }
        }

        if (traversalHandler.getNodeIndex(searcher.getXPos(), searcher.getYPos()) == currentSearcherPath.getEndIndex()) {
            // end of path reached, compute new path
            try {
                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
                searcherPathLineCounter = 0;
                if (currentSearcherPath == null) {
                    AdaptedSimulation.masterPause("DCRVEntity");
                    ExperimentConfiguration.interruptCurrentRun();
                    return;
                }
                if (stats != null) {
                    //stats.nrLeafRuns[evaderCounter]++;
                    stats.increaseNrLeafRuns();
                }
            } catch (DelaunayError e) {
                e.printStackTrace();
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
            //stats.nrSteps[evaderCounter]++;
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
                                    //stats.caughtBySearcher[evaderCounter] = a2.equals(searcher);
                                    stats.setCaughtBySearcher(a2.equals(searcher));
                                    System.out.println("HELL-o (" + a2.equals(searcher) + ", " + stats.getCounter() + ")");
                                    stats.targetCaught();
                                    //evaderCounter = evaderCounter + 1;
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

            if (triangleGuardOriginalPositions == null) {
                guardManagers = computeGuardManagers(separatingTriangles, map);
            } else {
                guardManagers = new ArrayList<>();
                for (int i = 0; i < separatingTriangles.size(); i++) {
                    guardManagers.add(new TriangleVisionGuardManager(map, triangleGuardOriginalPositions.get(i).get(0).x, triangleGuardOriginalPositions.get(i).get(0).y));
                }
            }

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
        //System.out.println("wat: " + evaderCounter);
    }

    public static ArrayList<GuardManager> computeGuardManagers(ArrayList<DTriangle> separatingTriangles, MapRepresentation map) {
        ArrayList<GuardManager> triangleVisionGuardManager = new ArrayList<>(separatingTriangles.size());

        for (DTriangle dt : separatingTriangles) {
            try {
                triangleVisionGuardManager.add(new TriangleVisionGuardManager(Entity.map, dt.getBarycenter().getX(), dt.getBarycenter().getY()));
            } catch (DelaunayError e) {
                e.printStackTrace();
            }
        }

        return triangleVisionGuardManager;
    }

}
