package entities.specific;

import additionalOperations.Tuple;
import entities.base.CentralisedEntity;
import entities.base.Entity;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import simulation.*;
import ui.Main;

import java.util.*;

/**
 * DCR = Divide and Conquer, Randomized, Vision-Based
 */
public class DCRVEntity extends CentralisedEntity {

    private TraversalHandler traversalHandler;
    private ArrayList<ArrayList<Line>> componentBoundaryLines;
    private ArrayList<ArrayList<DEdge>> componentBoundaryEdges;
    private ArrayList<Shape> componentBoundaryShapes;
    private ArrayList<DEdge> separatingEdges;

    private Agent searcher;
    private PlannedPath currentSearcherPath;
    private ArrayList<Line> pathLines;
    private int searcherPathLineCounter;

    private ArrayList<Agent> guards;
    private ArrayList<PlannedPath> initGuardPaths;
    private ArrayList<Integer> guardPathLineCounters;
    private ArrayList<Line> guardPathLines;
    private boolean guardsPositioned;

    public DCRVEntity(MapRepresentation map) {
        super(map);
        computeRequirements();
    }

    @Override
    public void move() {
        // initialising some local variables
        double length, deltaX, deltaY;

        // check if any agent is caught
        for (Entity e : map.getEvadingEntities()) {
            if (e.isActive()) {
                for (Agent a1 : e.getControlledAgents()) {
                    if (a1.isActive()) {
                        for (Agent a2 : availableAgents) {
                            if (map.isVisible(a1, a2)) {
                                a1.setActive(false);
                            }
                        }
                    }
                }
            }
        }

        // it is assumed that the required number of agents is provided by the GUI
        // could change this to throw an exception if it is not the case, giving the user a warning
        if (searcher == null) {
            assignTasks();
            try {
                currentSearcherPath = traversalHandler.getRandomTraversal(searcher.getXPos(), searcher.getYPos());
            } catch (DelaunayError e) {
                e.printStackTrace();
            }
        }

        // ******************************************************************************************************************************** //
        // Guard movement
        // ******************************************************************************************************************************** //

        if (!guardsPositioned()) {
            // let the guards move along their respective paths
            for (int i = 0; i < guards.size(); i++) {
                if (guards.get(i).getXPos() != initGuardPaths.get(i).getEndX() || guards.get(i).getYPos() != initGuardPaths.get(i).getEndY()) {
                    // this guard is not at its final destination and will be moved along the path
                    guardPathLines = initGuardPaths.get(i).getPathLines();

                    length = Math.sqrt(Math.pow(guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guardPathLines.get(guardPathLineCounters.get(i)).getStartX(), 2) + Math.pow(guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guardPathLines.get(guardPathLineCounters.get(i)).getStartY(), 2));
                    deltaX = (guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guardPathLines.get(guardPathLineCounters.get(i)).getStartX()) / length * guards.get(i).getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;
                    deltaY = (guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guardPathLines.get(guardPathLineCounters.get(i)).getStartY()) / length * guards.get(i).getSpeed() * UNIVERSAL_SPEED_MULTIPLIER;

                    if (guardPathLines.get(guardPathLineCounters.get(i)).contains(guards.get(i).getXPos() + deltaX, guards.get(i).getYPos() + deltaY)) {
                        // move along line
                        guards.get(i).moveBy(deltaX, deltaY);
                    } else {
                        // move to end of line
                        guards.get(i).moveBy(guardPathLines.get(guardPathLineCounters.get(i)).getEndX() - guards.get(i).getXPos(), guardPathLines.get(guardPathLineCounters.get(i)).getEndY() - guards.get(i).getYPos());
                        guardPathLineCounters.set(i, guardPathLineCounters.get(i) + 1);
                    }
                }
            }
        }

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

    }

    @Override
    public int totalRequiredAgents() {
        return requiredAgents;
    }

    @Override
    public int remainingRequiredAgents() {
        return requiredAgents - availableAgents.size();
    }

    @Override
    public void addAgent(Agent a) {
        availableAgents.add(a);
    }

    private void assignTasks() {
        // assign a certain number of agents to be guards for separating triangles
        try {
            initGuardPaths = new ArrayList<>();
            guards = new ArrayList<>();
            guardPathLineCounters = new ArrayList<>();
            double bestDistance, currentDistance;
            PlannedPath bestShortestPath, currentShortestPath;
            Agent tempClosestAgent;
            for (DTriangle dt : traversalHandler.getSeparatingTriangles()) {
                bestDistance = Double.MAX_VALUE;
                bestShortestPath = null;
                tempClosestAgent = null;
                for (Agent a : availableAgents) {
                    if (!guards.contains(a)) {
                        // compute distance to current triangle
                        currentShortestPath = shortestPathRoadMap.getShortestPath(a.getXPos(), a.getYPos(), dt.getBarycenter().getX(), dt.getBarycenter().getY());
                        currentDistance = currentShortestPath.getTotalLength();
                        if (currentDistance < bestDistance) {
                            bestDistance = currentDistance;
                            bestShortestPath = currentShortestPath;
                            tempClosestAgent = a;
                        }
                    }
                }
                guards.add(tempClosestAgent);
                Label l = new Label("guard");
                l.setTranslateX(tempClosestAgent.getXPos());
                l.setTranslateY(tempClosestAgent.getYPos());
                Main.pane.getChildren().add(l);
                initGuardPaths.add(bestShortestPath);
            }
            System.out.println("guards: " + guards.size());
            for (Agent ignored : guards) {
                guardPathLineCounters.add(0);
            }

            // the computed PlannedPath objects will initially be used to position all the guards in their correct locations
            // the (at least 2) remaining agents will be assigned to be searcher (and catcher)
            for (Agent a : availableAgents) {
                if (!guards.contains(a)) {
                    searcher = a;
                    break;
                }
            }
            currentSearcherPath = traversalHandler.getRestrictedShortestPathRoadMap().getShortestPath(searcher.getXPos(), searcher.getYPos(), searcher.getXPos(), searcher.getYPos());
        } catch (DelaunayError e) {
            e.printStackTrace();
        }
    }

    private boolean guardsPositioned() {
        if (!guardsPositioned) {
            for (int i = 0; i < guards.size(); i++) {
                if (guards.get(i).getXPos() != initGuardPaths.get(i).getEndX() || guards.get(i).getYPos() != initGuardPaths.get(i).getEndY()) {
                    return false;
                }
            }
            guardsPositioned = true;
            return true;
        }
        return true;
    }

    private void computeRequirements() {
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
            ArrayList<DEdge> nonSeparatingLines = separation.getValue1();
            int[][] spanningTreeAdjacencyMatrix = separation.getValue2();

            // show the computed spanning tree on the main pane
            ArrayList<Line> tree = showSpanningTree(nodes, spanningTreeAdjacencyMatrix);

            // compute the simply connected components in the graph
            ArrayList<DTriangle> componentNodes = new ArrayList<>();
            for (DTriangle dt : nodes) {
                if (!separatingTriangles.contains(dt)) {
                    componentNodes.add(dt);
                }
            }
            Tuple<ArrayList<ArrayList<DTriangle>>, int[]> componentInfo = computeConnectedComponents(nodes, componentNodes, spanningTreeAdjacencyMatrix);
            ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = componentInfo.getFirst();
            int[] parentNodes = componentInfo.getSecond();
            System.out.println("holes.size(): " + holes.size() + "\nseparatingTriangles.size(): " + separatingTriangles.size() + "\nsimplyConnectedComponents.size(): " + simplyConnectedComponents.size());

            // if there are more than 2 components compute change triangles around so that there is only one
            // looks like it may not be needed at all
            //computeSingleConnectedComponent(simplyConnectedComponents, holes, nodes, separatingTriangles, spanningTreeAdjacencyMatrix, originalAdjacencyMatrix, parentNodes, tree);

            Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> lineSeparation = computeGuardingLines(separatingTriangles, nonSeparatingLines);
            ArrayList<Line> separatingLines = lineSeparation.getValue0();
            ArrayList<DEdge> reconnectingEdges = lineSeparation.getValue1();
            ArrayList<DEdge> separatingEdges = lineSeparation.getValue2();

            Tuple<int[][], ArrayList<ArrayList<DTriangle>>> reconnectedAdjacency = computeReconnectedAdjacency(nodes, simplyConnectedComponents, reconnectingEdges, spanningTreeAdjacencyMatrix, separatingTriangles);
            int[][] reconnectedAdjacencyMatrix = reconnectedAdjacency.getFirst();
            ArrayList<ArrayList<DTriangle>> reconnectedComponents = reconnectedAdjacency.getSecond();

            Tuple<ArrayList<ArrayList<Line>>, ArrayList<Shape>> componentBoundaries = computeComponentBoundaries(reconnectedComponents, separatingEdges);
            componentBoundaryLines = componentBoundaries.getFirst();
            componentBoundaryShapes = componentBoundaries.getSecond();

            // given the spanning tree adjacency matrix and all the triangles, the tree structure that will be used
            // for deciding on randomised paths can be constructed
            traversalHandler = new TraversalHandler(shortestPathRoadMap, nodes, simplyConnectedComponents, spanningTreeAdjacencyMatrix);
            traversalHandler.separatingTriangleBased(separatingTriangles);

            requiredAgents = 1 + separatingTriangles.size();
            System.out.println("\nrequiredAgents: " + requiredAgents);
        } catch (DelaunayError error) {
            error.printStackTrace();
        }
    }

    // ******************************************************************************************************************************** //
    // Methods for initial computations (before task assignment)
    // ******************************************************************************************************************************** //

    private Tuple<ArrayList<ArrayList<Line>>, ArrayList<Shape>> computeComponentBoundaries(ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<DEdge> separatingEdges) {
        System.out.println("simplyConnectedComponents.size(): " + simplyConnectedComponents.size());
        ArrayList<ArrayList<Line>> boundaryLines = new ArrayList<>();
        componentBoundaryEdges = new ArrayList<>();
        ArrayList<Shape> componentShapes = new ArrayList<>();
        ArrayList<Line> temp;
        ArrayList<DEdge> tempEdges, tempComponentBoundaryEdges;
        Shape tempShape;
        for (ArrayList<DTriangle> arr : simplyConnectedComponents) {
            temp = new ArrayList<>();
            tempEdges = new ArrayList<>();
            tempComponentBoundaryEdges = new ArrayList<>();
            tempShape = new Polygon(0, 0);
            for (DTriangle dt : arr) {
                tempEdges.addAll(Arrays.asList(dt.getEdges()));
                tempShape = Shape.union(tempShape, new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY()));
            }
            componentShapes.add(tempShape);
            /*tempShape.setFill(Color.BLACK.brighter().brighter().brighter().brighter());
            Main.pane.getChildren().add(tempShape);*/
            for (DEdge de : tempEdges) {
                if (tempEdges.indexOf(de) == tempEdges.lastIndexOf(de) || separatingEdges.contains(de)) {
                    temp.add(new Line(de.getPointLeft().getX(), de.getPointLeft().getY(), de.getPointRight().getX(), de.getPointRight().getY()));
                    tempComponentBoundaryEdges.add(de);
                    /*Line l = new Line(de.getPointLeft().getEstX(), de.getPointLeft().getEstY(), de.getPointRight().getEstX(), de.getPointRight().getEstY());
                    l.setStroke(Color.BLUE);
                    l.setStrokeWidth(2);
                    Main.pane.getChildren().add(l);*/
                }
            }
            boundaryLines.add(temp);
            componentBoundaryEdges.add(tempComponentBoundaryEdges);
        }
        Polygon p;
        /*for (ArrayList<Line> arr : boundaryLines) {
            temp = (ArrayList<Line>) arr.clone();
            p = new Polygon();
            p.getPoints().addAll(
                    temp.get(0).getStartX(), temp.get(0).getStartY(),
                    temp.get(0).getEndX(), temp.get(0).getEndY()
            );
            temp.remove(0);
            while (!temp.isEmpty()) {
                boolean found = false;
                for (int j = 0; !found && j < temp.size(); j++) {
                    if (temp.get(j).getStartX() == p.getPoints().get(p.getPoints().size() - 2) && temp.get(j).getStartY() == p.getPoints().get(p.getPoints().size() - 1)) {
                        p.getPoints().addAll(temp.get(j).getEndX(), temp.get(j).getEndY());
                        temp.remove(j);
                        found = true;
                    } else if (temp.get(j).getEndX() == p.getPoints().get(p.getPoints().size() - 2) && temp.get(j).getEndY() == p.getPoints().get(p.getPoints().size() - 1)) {
                        p.getPoints().addAll(temp.get(j).getStartX(), temp.get(j).getStartY());
                        temp.remove(j);
                        found = true;
                    }
                }
                System.out.println("Doing line stuff");
            }
            componentShapes.add(p);
            p.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.2));
            p.setStroke(Color.RED);
            Main.pane.getChildren().add(p);
        }*/
        return new Tuple<>(boundaryLines, componentShapes);
    }

    private Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> computeGuardingLines(ArrayList<DTriangle> separatingTriangles, ArrayList<DEdge> nonSeparatingLines) {
        // for now its enough to just cover one side of each separating triangle because they are computed to have one edge adjacent to a polygon (i.e. they have degree 2 in the dual triangulation graph)
        ArrayList<Line> separatingLines = new ArrayList<>();
        separatingEdges = new ArrayList<>();
        ArrayList<DEdge> reconnectingEdges = new ArrayList<>();
        double minLength, maxLength, currentLengthSquared;
        DEdge minLengthEdge, maxLengthEdge;
        for (DTriangle dt : separatingTriangles) {
            minLength = Double.MAX_VALUE;
            maxLength = -Double.MAX_VALUE;
            minLengthEdge = null;
            maxLengthEdge = null;
            for (DEdge de : dt.getEdges()) {
                if (!nonSeparatingLines.contains(de)) {
                    currentLengthSquared = de.getSquared2DLength();
                    if (currentLengthSquared < minLength) {
                        minLength = currentLengthSquared;
                        minLengthEdge = de;
                    }
                    if (currentLengthSquared > maxLength) {
                        maxLength = currentLengthSquared;
                        maxLengthEdge = de;
                    }
                }
            }
            separatingLines.add(new Line(minLengthEdge.getPointLeft().getX(), minLengthEdge.getPointLeft().getY(), minLengthEdge.getPointRight().getX(), minLengthEdge.getPointRight().getY()));
            Line l = new Line(minLengthEdge.getPointLeft().getX(), minLengthEdge.getPointLeft().getY(), minLengthEdge.getPointRight().getX(), minLengthEdge.getPointRight().getY());
            l.setStrokeWidth(6);
            l.setFill(Color.ALICEBLUE);
            Main.pane.getChildren().add(l);
            separatingEdges.add(minLengthEdge);
            reconnectingEdges.add(maxLengthEdge);
        }
        return new Triplet<>(separatingLines, reconnectingEdges, separatingEdges);
    }

    private Tuple<int[][], ArrayList<ArrayList<DTriangle>>> computeReconnectedAdjacency(ArrayList<DTriangle> triangles, ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<DEdge> reconnectingEdges, int[][] disconnectedAdjacencyMatrix, ArrayList<DTriangle> separatingTriangles) {
        int[][] reconnectedAdjacencyMatrix = new int[disconnectedAdjacencyMatrix.length][disconnectedAdjacencyMatrix.length];
        for (int i = 0; i < disconnectedAdjacencyMatrix.length; i++) {
            System.arraycopy(disconnectedAdjacencyMatrix[i], 0, reconnectedAdjacencyMatrix[i], 0, disconnectedAdjacencyMatrix.length);
        }
        ArrayList<ArrayList<DTriangle>> reconnectedComponents = new ArrayList<>();
        ArrayList<DTriangle> temp;
        for (ArrayList<DTriangle> al : simplyConnectedComponents) {
            temp = new ArrayList<>();
            temp.addAll(al);
            reconnectedComponents.add(temp);
        }
        int index1, index2, connectingTriangleIndex, reconnectingTriangleIndex;
        for (DEdge de : reconnectingEdges) {
            index1 = triangles.indexOf(de.getLeft());
            index2 = triangles.indexOf(de.getRight());
            //index1 = -1;
            //index2 = -1;
            for (int i = 0; i < triangles.size(); i++) {
                if (triangles.get(i).isEdgeOf(de)) {
                    if (index1 == -1) {
                        index1 = i;
                    } else {
                        index2 = i;
                    }
                }
            }
            reconnectedAdjacencyMatrix[index1][index2] = 1;
            reconnectedAdjacencyMatrix[index2][index1] = 1;

            if (!separatingTriangles.contains(triangles.get(index1))) {
                connectingTriangleIndex = index1;
                reconnectingTriangleIndex = index2;
            } else {
                connectingTriangleIndex = index2;
                reconnectingTriangleIndex = index1;
            }
            for (ArrayList<DTriangle> al : reconnectedComponents) {
                if (al.contains(triangles.get(connectingTriangleIndex))) {
                    al.add(triangles.get(reconnectingTriangleIndex));
                }
            }
        }
        return new Tuple<>(reconnectedAdjacencyMatrix, reconnectedComponents);
    }

    private Tuple<ArrayList<DTriangle>, ArrayList<DTriangle>> triangulate(MapRepresentation map) throws DelaunayError {
        ArrayList<Polygon> mapPolygons = map.getAllPolygons();
        ArrayList<DEdge> constraintEdges = new ArrayList<>();
        for (Polygon p : mapPolygons) {
            if (p != null) {
                for (int i = 0; i < p.getPoints().size(); i += 2) {
                    constraintEdges.add(new DEdge(new DPoint(p.getPoints().get(i), p.getPoints().get(i + 1), 0), new DPoint(p.getPoints().get((i + 2) % p.getPoints().size()), p.getPoints().get((i + 3) % p.getPoints().size()), 0)));
                }
            }
        }
        ConstrainedMesh mesh = new ConstrainedMesh();
        mesh.setConstraintEdges(constraintEdges);
        mesh.processDelaunay();

        List<DTriangle> triangles = mesh.getTriangleList();
        ArrayList<DTriangle> includedTriangles = new ArrayList<>();
        ArrayList<DTriangle> holeTriangles = new ArrayList<>();
        for (DTriangle dt : triangles) {
            // check if triangle in polygon
            double centerX = dt.getBarycenter().getX();
            double centerY = dt.getBarycenter().getY();
            boolean inPolygon = true;
            boolean inHole = false;
            if (!mapPolygons.get(0).contains(centerX, centerY)) {
                inPolygon = false;
            }
            for (int i = 1; inPolygon && i < mapPolygons.size(); i++) {
                if (mapPolygons.get(i).contains(centerX, centerY)) {
                    inPolygon = false;
                    inHole = true;
                }
            }
            if (Math.abs(dt.getAngle(0) + dt.getAngle(1) + dt.getAngle(2)) < 5) {
                inPolygon = false;
            }
            if (inPolygon) {
                includedTriangles.add(dt);
            }
            if (inHole) {
                holeTriangles.add(dt);
            }
        }
        return new Tuple<>(includedTriangles, holeTriangles);
    }

    private Tuple<int[][], int[]> computeAdjacency(ArrayList<DTriangle> nodes) {
        int[][] originalAdjacencyMatrix = new int[nodes.size()][nodes.size()];

        // checking for adjacency between nodes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        DEdge de;
        for (int i = 0; i < nodes.size(); i++) {
            dt1 = nodes.get(i);
            // go through the edges of each triangle
            for (int j = 0; j < 3; j++) {
                de = dt1.getEdge(j);
                if (!checkedEdges.contains(de)) {
                    int neighbourIndex = -1;
                    for (int k = 0; neighbourIndex == -1 && k < nodes.size(); k++) {
                        dt2 = nodes.get(k);
                        if (k != i && dt2.isEdgeOf(de)) {
                            // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                            neighbourIndex = k;
                        }
                    }
                    if (neighbourIndex != -1) {
                        originalAdjacencyMatrix[i][neighbourIndex] = 1;
                        originalAdjacencyMatrix[neighbourIndex][i] = 1;
                    }
                    checkedEdges.add(de);
                }
            }
        }

        int[] degreeMatrix = new int[nodes.size()];
        int degreeCount;
        for (int i = 0; i < nodes.size(); i++) {
            degreeCount = 0;
            for (int j = 0; j < nodes.size(); j++) {
                if (originalAdjacencyMatrix[i][j] == 1) {
                    degreeCount++;
                }
            }
            degreeMatrix[i] = degreeCount;
        }

        return new Tuple<>(originalAdjacencyMatrix, degreeMatrix);
    }

    private ArrayList<ArrayList<DTriangle>> computeHoles(ArrayList<DTriangle> holeTriangles) {
        // 1. group hole triangles by hole
        // 2. choose degree-2 triangle adjacent to the hole (can be according to some criterion)
        // 3. ???
        // 4. profit

            /*
            for each triangle, find all the triangles that are connected to it from holeTriangles
            then do the same for the triangles added in that iteration
            start with the first hole/triangle
            find all its immediate neighbours from holeTriangles (and remove them from that list)
            find their immediate neighbours (and so forth) until no new triangles are added to the hole
             */

        // checking for adjacency of triangles in the holes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1;
        ArrayList<ArrayList<DTriangle>> holes = new ArrayList<>();
        ArrayList<DTriangle> temp;
        for (int i = 0; i < holeTriangles.size(); i++) {
            dt1 = holeTriangles.get(i);
            temp = new ArrayList<>();
            temp.add(dt1);
            holeTriangles.remove(dt1);

            boolean addedToHole = true;
            while (addedToHole) {
                addedToHole = false;
                // go through the remaining triangles and see if they are adjacent to the ones already in the hole
                for (int j = 0; j < temp.size(); j++) {
                    for (int k = 0; k < holeTriangles.size(); k++) {
                        if (temp.get(j) != holeTriangles.get(k) && (holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(0)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(1)) || holeTriangles.get(k).isEdgeOf(temp.get(j).getEdge(2)))) {
                            temp.add(holeTriangles.get(k));
                            holeTriangles.remove(k);
                            addedToHole = true;
                            k--;
                        }
                    }
                }
            }
            holes.add(temp);
            i--;
        }

        System.out.println("Nr. holes: " + holes.size());
        for (ArrayList<DTriangle> hole : holes) {
            System.out.println("Hole with " + hole.size() + (hole.size() > 1 ? " triangles" : " triangle"));
        }

        Polygon tempTriangle;
        Color currentColor;
        for (ArrayList<DTriangle> hole : holes) {
            //currentColor = new Color(Math.random(), Math.random(), Math.random(), 0.7);
            currentColor = Color.rgb(255, 251, 150, 0.4);
            for (DTriangle dt : hole) {
                tempTriangle = new Polygon(dt.getPoint(0).getX(), dt.getPoint(0).getY(), dt.getPoint(1).getX(), dt.getPoint(1).getY(), dt.getPoint(2).getX(), dt.getPoint(2).getY());
                tempTriangle.setFill(currentColor);
                tempTriangle.setStroke(Color.GREY);
                Main.pane.getChildren().add(tempTriangle);
            }
        }

        return holes;
    }

    private Triplet<ArrayList<DTriangle>, ArrayList<DEdge>, int[][]> computeSeparatingTriangles(ArrayList<DTriangle> nodes, ArrayList<ArrayList<DTriangle>> holes, int[][] originalAdjacencyMatrix, int[] degreeMatrix) {
        // separating triangles are those adjacent to a hole and (for now) degree 2
        ArrayList<DTriangle> separatingTriangles = new ArrayList<>();
        ArrayList<DEdge> nonSeparatingEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        for (ArrayList<DTriangle> hole : holes) {
            boolean triangleFound = false;
            // go through triangles in the hole
            for (int i = 0; !triangleFound && i < hole.size(); i++) {
                dt1 = hole.get(i);
                // go through triangles outside the hole
                for (int j = 0; !triangleFound && j < nodes.size(); j++) {
                    dt2 = nodes.get(j);
                    // if the triangle has degree two and is adjacent to the holeTriangle, make it a separating triangle
                    if (degreeMatrix[j] == 2 && (dt2.isEdgeOf(dt1.getEdge(0)) || dt2.isEdgeOf(dt1.getEdge(1)) || dt2.isEdgeOf(dt1.getEdge(2)))) {
                        int vertexCount = 0;
                        for (DTriangle holeTriangle : hole) {
                            for (int k = 0; k < dt2.getPoints().size(); k++) {
                                if (holeTriangle.getPoints().contains(dt2.getPoint(k))) {
                                    vertexCount++;
                                }
                            }
                        }
                        int what = 0;
                        for (int z = 0; z < separatingTriangles.size(); z++) {
                            for (int k = 0; k < dt2.getPoints().size(); k++) {
                                if (separatingTriangles.get(z).getPoints().contains(dt2.getPoint(k))) {
                                    what++;
                                }
                            }
                        }
                        System.out.println("vertexCount = " + vertexCount);
                        if (vertexCount <= 4 && what < 2) {
                            separatingTriangles.add(dt2);
                            if (dt2.isEdgeOf(dt1.getEdge(0))) {
                                nonSeparatingEdges.add(dt1.getEdge(0));
                            }
                            if (dt2.isEdgeOf(dt1.getEdge(1))) {
                                nonSeparatingEdges.add(dt1.getEdge(1));
                            }
                            if (dt2.isEdgeOf(dt1.getEdge(2))) {
                                nonSeparatingEdges.add(dt1.getEdge(2));
                            }
                            triangleFound = true;
                        }
                    }
                }
            }
        }
        // if they form a loop, then change one?
        // run spanning tree on the generated graph and see whether branches "meet"
        // if so, break that loop either by adding a separating triangle or by changing a separating triangle
        // also, could just use this from the start?

        //int[][] spanningTreeAdjacencyMatrix = originalAdjacencyMatrix.clone();
        int[][] spanningTreeAdjacencyMatrix = new int[nodes.size()][nodes.size()];
        for (int i = 0; i < originalAdjacencyMatrix.length; i++) {
            System.arraycopy(originalAdjacencyMatrix[i], 0, spanningTreeAdjacencyMatrix[i], 0, originalAdjacencyMatrix[0].length);
        }
        for (DTriangle dt : separatingTriangles) {
            for (int i = 0; i < nodes.size(); i++) {
                spanningTreeAdjacencyMatrix[nodes.indexOf(dt)][i] = 0;
                spanningTreeAdjacencyMatrix[i][nodes.indexOf(dt)] = 0;
            }
        }

        return new Triplet<>(separatingTriangles, nonSeparatingEdges, spanningTreeAdjacencyMatrix);
    }

    private Tuple<ArrayList<ArrayList<DTriangle>>, int[]> computeConnectedComponents(ArrayList<DTriangle> nodes, ArrayList<DTriangle> componentNodes, int[][] spanningTreeAdjacencyMatrix) {
        ArrayList<ArrayList<DTriangle>> simplyConnectedComponents = new ArrayList<>();
        ArrayList<DTriangle> temp;
        ArrayList<Integer> currentLayer, nextLayer;
        boolean[] visitedNodes = new boolean[nodes.size()];
        int[] componentParentNodes = new int[nodes.size()];
        boolean unexploredLeft;
        for (int i = 0; i < componentNodes.size(); i++) {
            temp = new ArrayList<>();
            temp.add(componentNodes.get(i));

            unexploredLeft = true;
            currentLayer = new ArrayList<>();
                        /*System.out.println("i: " + i);
                        System.out.println("componentNodes.size(): " + componentNodes.size());*/
            currentLayer.add(nodes.indexOf(componentNodes.get(i)));
            componentNodes.remove(i);
            while (unexploredLeft) {
                nextLayer = new ArrayList<>();
                for (int j : currentLayer) {
                    visitedNodes[j] = true;
                    for (int k = 0; k < componentNodes.size(); k++) {
                        if (spanningTreeAdjacencyMatrix[j][nodes.indexOf(componentNodes.get(k))] == 1 && nodes.indexOf(componentNodes.get(k)) != componentParentNodes[j] && !visitedNodes[nodes.indexOf(componentNodes.get(k))]) {
                            nextLayer.add(nodes.indexOf(componentNodes.get(k)));
                            componentParentNodes[nodes.indexOf(componentNodes.get(k))] = j;
                            visitedNodes[nodes.indexOf(componentNodes.get(k))] = true;
                            temp.add(componentNodes.get(k));
                            componentNodes.remove(k);
                            k--;
                        }
                    }
                }
                currentLayer = nextLayer;
                if (nextLayer.size() == 0) {
                    unexploredLeft = false;
                }
            }
            simplyConnectedComponents.add(temp);
            i--;
        }
        return new Tuple<>(simplyConnectedComponents, componentParentNodes);
    }

    private void computeSingleConnectedComponent(ArrayList<ArrayList<DTriangle>> simplyConnectedComponents, ArrayList<ArrayList<DTriangle>> holes, ArrayList<DTriangle> nodes, ArrayList<DTriangle> separatingTriangles, int[][] spanningTreeAdjacencyMatrix, int[][] originalAdjacencyMatrix, int[] parentNodes, ArrayList<Line> tree) throws DelaunayError {
        if (parentNodes == null) {
            return;
        }
        if (simplyConnectedComponents.size() == 2) {
            simplyConnectedComponents.sort((o1, o2) -> o1.size() > o2.size() ? -1 : (o1.size() == o2.size() ? 0 : 1));
            //System.out.println("simplyConnectedComponents.size(): " + simplyConnectedComponents.size());

            // now cut through any possible loops:
            // want a triangle adjacent to one of the enclosing triangles for each cut-off region
            // (and on the outside) that is also part of the loop -> how can you identify that?
            // 1 could check whether everything is still reachable from the adjacent nodes of the one you want to make a separator
            // 2 2.1 search for nodes in the tree which are not
            //       a) separating triangles
            //       b) parent and child
            //       but are still adjacent in the separating tree graph
            //   2.2 then trace their paths back to the last common ancestor
            //       -> take path from one back to the root and store nodes, then take path from the other until you meet one of the nodes
            //       -> thereby automatically iterate over all of the nodes on the loop
            //   2.3 find a hole that has a triangle on the loop adjacent to it, then move the current adjacent
            //       separating triangle to that new-found triangle (can use some other criterion for selection too)

                        /*for (int i = 0; i < adjacencyMatrix.length; i++) {
                            System.out.print(i + " | ");
                            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                                System.out.print(adjacencyMatrix[i][j] + " ");
                            }
                            System.out.println();
                        }*/

            // finding pairs of adjacent nodes that are not parent and child (and thus form a loop)
            ArrayList<int[]> adjacentPairs = new ArrayList<>();
            for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                for (int j = 0; j < i; j++) {
                    if (spanningTreeAdjacencyMatrix[i][j] == 1 && !(parentNodes[i] == j || parentNodes[j] == i)) {
                        adjacentPairs.add(new int[]{i, j});
                        System.out.println(i + " and " + j + " adjacent but not parent and child");
                    }
                }
            }

            // trace the paths of the pair back to the last common ancestor
            ArrayList<ArrayList<Integer>> loops = new ArrayList<>();
            ArrayList<Integer>[] tempIndeces;
            for (int[] adjacentPair : adjacentPairs) {
                tempIndeces = new ArrayList[2];
                tempIndeces[0] = new ArrayList<>();
                tempIndeces[0].add(adjacentPair[0]);
                int currentParent = parentNodes[adjacentPair[0]];
                while (currentParent != -1) {
                    tempIndeces[0].add(currentParent);
                    currentParent = parentNodes[currentParent];
                }

                tempIndeces[1] = new ArrayList<>();
                tempIndeces[1].add(adjacentPair[1]);
                currentParent = parentNodes[adjacentPair[1]];
                while (!tempIndeces[0].contains(currentParent)) {
                    tempIndeces[1].add(currentParent);
                    currentParent = parentNodes[currentParent];
                }

                int commonAncestor = currentParent;
                int currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                while (currentIndex != commonAncestor) {
                    tempIndeces[0].remove(tempIndeces[0].size() - 1);
                    currentIndex = tempIndeces[0].get(tempIndeces[0].size() - 1);
                }

                for (int j = tempIndeces[1].size() - 1; j >= 0; j--) {
                    tempIndeces[0].add(tempIndeces[1].get(j));
                }
                loops.add(tempIndeces[0]);
            }

            for (int i = 0; i < loops.size(); i++) {
                if (loops.get(i).size() <= 3) {
                    loops.remove(i);
                    i--;
                }
            }

            DTriangle dt1, dt2, dt3;
            if (loops.size() == 1) {
                // find a hole adjacent to both the loop and the disconnected component
                // then use its separating triangle to break the loop and open up the disconnected component
                ArrayList<DTriangle> currentConnectedComponent = simplyConnectedComponents.get(1);
                ArrayList<Integer> loopBreakingCandidates = new ArrayList<>();
                ArrayList<DTriangle> currentHole;
                for (int z = 0; z < holes.size(); z++) {
                    currentHole = holes.get(z);
                    // see whether the hole is adjacent to the connected components
                    // i.e. (its separating triangle) could be used to "break up" the barrier enclosing that component
                    boolean disconnectedAdjacencyFound = false;
                    boolean loopAdjacencyFound = false;
                    for (int i = 0; (!disconnectedAdjacencyFound || !loopAdjacencyFound) && i < currentHole.size(); i++) {
                        dt1 = currentHole.get(i);
                        for (int j = 0; !disconnectedAdjacencyFound && j < currentConnectedComponent.size(); j++) {
                            dt2 = currentConnectedComponent.get(j);
                            for (int k = 0; !disconnectedAdjacencyFound && k < dt1.getPoints().size(); k++) {
                                if (dt2.isOnAnEdge(dt1.getPoint(k))) {
                                    disconnectedAdjacencyFound = true;
                                }
                            }
                        }
                        for (int j = 0; !loopAdjacencyFound && j < loops.get(0).size(); j++) {
                            dt3 = nodes.get(loops.get(0).get(j));
                            for (int k = 0; !loopAdjacencyFound && k < dt1.getEdges().length; k++) {
                                if (dt3.isEdgeOf(dt1.getEdge(k))) {
                                    loopAdjacencyFound = true;
                                }
                            }
                        }
                    }
                    if (disconnectedAdjacencyFound && loopAdjacencyFound) {
                        loopBreakingCandidates.add(z);
                    }
                }
                System.out.println("loopBreakingCandidates.size(): " + loopBreakingCandidates.size());

                if (loopBreakingCandidates.size() != 0) {
                    // change separating triangle
                    // find a triangle adjacent to the loop
                    DTriangle newSeparatingTriangle = null;
                    DTriangle oldSeparatingTriangle = separatingTriangles.get(loopBreakingCandidates.get(0));
                    boolean newSeparatingTriangleFound = false;
                    for (int i = 0; !newSeparatingTriangleFound && i < holes.get(loopBreakingCandidates.get(0)).size(); i++) {
                        dt1 = holes.get(loopBreakingCandidates.get(0)).get(i);
                        for (int j = 0; !newSeparatingTriangleFound && j < loops.get(0).size(); j++) {
                            dt2 = nodes.get(loops.get(0).get(j));
                            for (int k = 0; !newSeparatingTriangleFound && k < dt1.getEdges().length; k++) {
                                if (dt2.isEdgeOf(dt1.getEdge(k))) {
                                    newSeparatingTriangle = dt2;
                                    newSeparatingTriangleFound = true;
                                }
                            }
                        }
                    }
                    if (newSeparatingTriangleFound) {
                        separatingTriangles.set(loopBreakingCandidates.get(0), newSeparatingTriangle);
                        // updating the adjacency matrix
                        for (int i = 0; i < nodes.size(); i++) {
                            spanningTreeAdjacencyMatrix[nodes.indexOf(newSeparatingTriangle)][i] = 0;
                            spanningTreeAdjacencyMatrix[i][nodes.indexOf(newSeparatingTriangle)] = 0;
                        }
                    } else {
                        System.out.println("No new separating triangle found.");
                    }

                    // updating the adjacency matrix
                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        if (originalAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] == 1 || originalAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] == 1) {
                            spanningTreeAdjacencyMatrix[nodes.indexOf(oldSeparatingTriangle)][i] = 1;
                            spanningTreeAdjacencyMatrix[i][nodes.indexOf(oldSeparatingTriangle)] = 1;
                            System.out.println("Happens");
                        }
                    }

                    // visuals, aye
                    Line tempLine;
                    for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
                        for (int j = 0; j < i; j++) {
                            if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                                tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                                tempLine.setStroke(Color.RED);
                                tempLine.setStrokeWidth(4);
                                tree.add(tempLine);
                            }
                        }
                    }
                }
            } else {
                System.out.println("There are " + (loops.size() > 1 ? "multiple" : "no") + "loops.");
            }
        } else {
            System.out.println("There are " + (simplyConnectedComponents.size() == 1 ? "no more" : simplyConnectedComponents.size()) + " simply-connected components.");
        }
    }

    private ArrayList<Line> showSpanningTree(ArrayList<DTriangle> nodes, int[][] spanningTreeAdjacencyMatrix) throws DelaunayError {
        boolean unexploredLeft = true;
        ArrayList<Integer> currentLayer = new ArrayList<>();
        currentLayer.add(0);
        ArrayList<Integer> nextLayer;

        boolean[] visitedNodes = new boolean[nodes.size()];
        int[] parentNodes = new int[nodes.size()];
        parentNodes[0] = -1;

        ArrayList<Line> tree = new ArrayList<>();
        Line tempLine;
        while (unexploredLeft) {
            nextLayer = new ArrayList<>();
            for (int i : currentLayer) {
                visitedNodes[i] = true;
                for (int j = 0; j < nodes.size(); j++) {
                    if (spanningTreeAdjacencyMatrix[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                        nextLayer.add(j);
                        parentNodes[j] = i;
                        visitedNodes[j] = true;

                        tempLine = new Line(nodes.get(i).getBarycenter().getX(), nodes.get(i).getBarycenter().getY(), nodes.get(j).getBarycenter().getX(), nodes.get(j).getBarycenter().getY());
                        tempLine.setStroke(Color.RED);
                        tempLine.setStrokeWidth(4);
                        tree.add(tempLine);
                    }
                }
            }
            currentLayer = nextLayer;
            if (nextLayer.size() == 0) {
                unexploredLeft = false;
            }
        }

        for (int i = 0; i < spanningTreeAdjacencyMatrix.length; i++) {
            int c = 0;
            for (int j = 0; j < spanningTreeAdjacencyMatrix[0].length; j++) {
                if (spanningTreeAdjacencyMatrix[i][j] == 1) {
                    c++;
                }
            }
            System.out.println("Node " + i + " has " + c + " neighbours");
        }

        return tree;
    }

    public static Agent testTarget;

}
