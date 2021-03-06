package simulation;

import entities.utils.*;
import experiments.ExperimentConfiguration;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import maps.MapRepresentation;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;
import ui.Main;

import java.util.ArrayList;

// could consider using this class for both randomised and deterministic approach (randomised already pretty clear)
public class TraversalHandler {

    private static final boolean PRINT_PATH_CONSTRUCT = false;

    private ArrayList<TGNode> nodes;
    private int[][] adjacencyMatrix;

    private Line[][] adjacencyLineMatrix;
    private DEdge[][] adjacencyEdgeMatrix;

    public ShortestPathRoadMap shortestPathRoadMap;
    private ShortestPathRoadMap restrictedShortestPathRoadMap;

    private ArrayList<DTriangle> nodess;
    private ArrayList<ArrayList<DTriangle>> components;
    private ArrayList<DTriangle> separatingTriangles;
    private ArrayList<Line> separatingLines;

    private ShortestPathRoadMap pocketShortestPathRoadMap;
    private ArrayList<DTriangle> pocketComponent;
    private int[][] pocketAdjacencyMatrix;
    private boolean restrictToPocket;

    private EnumeratedIntegerDistribution rng;

    private Group graphics;

    public TraversalHandler(ArrayList<DTriangle> triangles) {
        init(triangles);
        nodess = triangles;
        graphics = new Group();
        Main.pane.getChildren().add(graphics);
    }

    public TraversalHandler(ShortestPathRoadMap shortestPathRoadMap, ArrayList<DTriangle> nodes, ArrayList<ArrayList<DTriangle>> components, int[][] adjacencyMatrix) {
        // this constructor should be able to handle any input where the map has been converted into
        // one or multiple SIMPLY-CONNECTED components
        // then this class can deal with transitions between components as well as traversals within components
        init(nodes);
        this.shortestPathRoadMap = shortestPathRoadMap;
        this.restrictedShortestPathRoadMap = shortestPathRoadMap;
        this.nodess = nodes;
        this.components = components;
        this.adjacencyMatrix = adjacencyMatrix;

        graphics = new Group();
        Main.pane.getChildren().add(graphics);

        /*for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = 0; j < adjacencyMatrix.length; j++) {
                System.out.print(adjacencyMatrix[i][j] + " ");
            }
            System.out.println();
        }*/
    }

    public void separatingTriangleBased(ArrayList<DTriangle> separatingTriangles) {
        this.separatingTriangles = separatingTriangles;
        restrictedShortestPathRoadMap = new ShortestPathRoadMap(shortestPathRoadMap.getMap(), separatingTriangles);
    }

    public void separatingTriangleBased(ArrayList<DTriangle> separatingTriangles, ShortestPathRoadMap restrictedShortestPathRoadMap) {
        this.separatingTriangles = separatingTriangles;
        this.restrictedShortestPathRoadMap = restrictedShortestPathRoadMap;
    }

    public void separatingLineBased(ArrayList<Line> separatingLines, ArrayList<ArrayList<DTriangle>> reconnectedComponents, int[][] reconnectedAdjacencyMatrix) {
        this.separatingLines = separatingLines;
        this.components = reconnectedComponents;
        this.adjacencyMatrix = reconnectedAdjacencyMatrix;
        System.out.println("size: " + shortestPathRoadMap.getMap().getAllPolygons().get(0).getPoints().size());
        restrictedShortestPathRoadMap = new ShortestPathRoadMap(separatingLines, shortestPathRoadMap.getMap());
    }

    public void separatingLineBased(ArrayList<Line> separatingLines, ArrayList<ArrayList<DTriangle>> reconnectedComponents, int[][] reconnectedAdjacencyMatrix, ShortestPathRoadMap restrictedShortestPathRoadMap) {
        this.separatingLines = separatingLines;
        this.components = reconnectedComponents;
        this.adjacencyMatrix = reconnectedAdjacencyMatrix;
        this.restrictedShortestPathRoadMap = restrictedShortestPathRoadMap;
    }

    public void separatingLineBased(ArrayList<Line> separatingLines) {
        this.separatingLines = separatingLines;
        restrictedShortestPathRoadMap = new ShortestPathRoadMap(separatingLines, shortestPathRoadMap.getMap());
    }

    /*public TraversalHandler(MapRepresentation map, ShortestPathRoadMap shortestPathRoadMap, ArrayList<DTriangle> nodes, ArrayList<ArrayList<DTriangle>> components, ArrayList<Line> separatingLines, int[][] adjacencyMatrix) {
        // this constructor should be able to handle any input where the map has been converted into
        // one or multiple SIMPLY-CONNECTED components
        // then this class can deal with transitions between components as well as traversals within components
        init(nodes);
        this.shortestPathRoadMap = shortestPathRoadMap;
        this.restrictedShortestPathRoadMap = new ShortestPathRoadMap(map, separatingTriangles);
        this.nodess = nodes;
        this.components = components;
        this.separatingTriangles = separatingTriangles; // might not actually be needed
        this.adjacencyMatrix = adjacencyMatrix;
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                System.out.print(adjacencyMatrix[i][j] + " ");
            }
            System.out.println();
        }
    }*/

    private void init(ArrayList<DTriangle> triangles) {
        // initialising list of nodes and adjacency matrix
        nodes = new ArrayList<>(triangles.size());
        for (DTriangle dt : triangles) {
            nodes.add(new TGNode(dt));
        }
        adjacencyMatrix = new int[nodes.size()][nodes.size()];
        adjacencyLineMatrix = new Line[nodes.size()][nodes.size()];
        adjacencyEdgeMatrix = new DEdge[nodes.size()][nodes.size()];

        // checking for adjacency between nodes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        DEdge de;
        for (int i = 0; i < triangles.size(); i++) {
            dt1 = triangles.get(i);
            // go through the edges of each triangle
            for (int j = 0; j < 3; j++) {
                de = dt1.getEdge(j);
                if (!checkedEdges.contains(de)) {
                    int neighbourIndex = -1;
                    for (int k = 0; neighbourIndex == -1 && k < triangles.size(); k++) {
                        dt2 = triangles.get(k);
                        if (k != i && dt2.isEdgeOf(de)) {
                            // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                            neighbourIndex = k;
                        }
                    }
                    if (neighbourIndex != -1) {
                        try {
                            adjacencyMatrix[i][neighbourIndex] = 1;
                            adjacencyMatrix[neighbourIndex][i] = 1;
                            adjacencyLineMatrix[i][neighbourIndex] = new Line(dt1.getBarycenter().getX(), dt1.getBarycenter().getY(), triangles.get(neighbourIndex).getBarycenter().getX(), triangles.get(neighbourIndex).getBarycenter().getY());
                            adjacencyLineMatrix[neighbourIndex][i] = new Line(triangles.get(neighbourIndex).getBarycenter().getX(), triangles.get(neighbourIndex).getBarycenter().getY(), dt1.getBarycenter().getX(), dt1.getBarycenter().getY());
                            adjacencyEdgeMatrix[i][neighbourIndex] = de;
                            adjacencyEdgeMatrix[neighbourIndex][i] = de;
                        } catch (DelaunayError e) {
                            e.printStackTrace();
                        }
                    }
                    checkedEdges.add(de);
                }
            }
        }
    }

    public ArrayList<DTriangle> getSeparatingTriangles() {
        return separatingTriangles;
    }

    public ArrayList<ArrayList<DTriangle>> getComponents() {
        return components;
    }

    public ArrayList<DTriangle> getTriangles() {
        return nodess;
    }

    public int[][] getAdjacencyMatrix() {
        return adjacencyMatrix;
    }

    public ShortestPathRoadMap getRestrictedShortestPathRoadMap() {
        return restrictedShortestPathRoadMap;
    }

    public void restrictToPocket(ArrayList<DTriangle> pocketComponent, int[][] pocketAdjacencyMatrix) {
        restrictToPocket = true;
        this.pocketComponent = pocketComponent;
        this.pocketAdjacencyMatrix = pocketAdjacencyMatrix;
        pocketShortestPathRoadMap = restrictedShortestPathRoadMap;
        // TODO: also change this to have a separate shortest path map, otherwise it will search in the right pocket but take weird detours
        // could do this by also having the remaining separating lines as input as well
    }

    public void restrictToPocket(ArrayList<DTriangle> pocketComponent, int[][] pocketAdjacencyMatrix, MapRepresentation map, Line crossedSeparatingLine) {
        restrictToPocket = true;
        this.pocketComponent = pocketComponent;
        this.pocketAdjacencyMatrix = pocketAdjacencyMatrix;
        if (crossedSeparatingLine != null) {
            ArrayList<Line> temp = new ArrayList<>();
            for (Line l : separatingLines) {
                if (!l.equals(crossedSeparatingLine)) {
                    temp.add(l);
                }
            }
            pocketShortestPathRoadMap = new ShortestPathRoadMap(temp, map);
        } else {
            pocketShortestPathRoadMap = restrictedShortestPathRoadMap;
        }
    }

    public void removeRestriction() {
        restrictToPocket = false;
    }

    public TGNode getLeaf() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            int count = 0;
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                if (adjacencyMatrix[i][j] == 1) {
                    count++;
                }
            }
            if (count == 1) {
                /*try {
                    System.out.println("\nReturning leaf of simply connected tree - Index: " + i + ", Middle: (" + nodes.get(i).getTriangle().getBarycenter().getEstX() + "|" + nodes.get(i).getTriangle().getBarycenter().getEstY() + ")");
                } catch (DelaunayError e) {
                    e.printStackTrace();
                }*/
                return nodes.get(i);
            }
        }
        return null;
    }

    public boolean isLeaf(TGNode node) {
        return isLeaf(nodes.indexOf(node));
    }

    public boolean isLeaf(int index) {
        int count = 0;
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            if ((restrictToPocket ? pocketAdjacencyMatrix : adjacencyMatrix)[index][i] == 1) {
                count++;
            }
        }
        return count == 1;
    }

    public TGNode getLeafNeighbour(TGNode leaf) {
        if (isLeaf(leaf)) {
            for (int i = 0; i < adjacencyMatrix[0].length; i++) {
                if (adjacencyMatrix[nodes.indexOf(leaf)][i] == 1) {
                    return nodes.get(i);
                }
            }
        }
        return null;
    }

    public DTriangle getNode(double xCoord, double yCoord) {
        try {
            for (DTriangle dt : nodess) {
                if (dt.contains(new DPoint(xCoord, yCoord, 0))) {
                    return dt;
                }
            }
        } catch (DelaunayError e) {
            System.out.println(xCoord + " " + yCoord);
            e.printStackTrace();
        }
        return null;
    }

    public int getNodeIndex(double xCoord, double yCoord) {
        return nodess.indexOf(getNode(xCoord, yCoord));
    }

    public ArrayList<TGNode> getNodes() {
        return getNodes();
    }

    public PlannedPath getRandomTraversal(double xPos, double yPos) throws DelaunayError {
        graphics.getChildren().clear();
        // 1. pick one of the simply-connected components to make a run/traversal in
        //    (could do that uniformly or by size or something like that)
        // 2. move to a randomly chosen leaf node of that component
        // 3. get normal random traversal from that leaf not to a different leaf node
        // 4. run is over, repeat

        // for now chosen uniformly at random
        ArrayList<DTriangle> currentComponent = components == null ? nodess : components.get((int) (Math.random() * components.size()));
        currentComponent = restrictToPocket ? pocketComponent : currentComponent;
        // check whether we're already in that component, then the movement to one of its leaves can be skipped
        boolean inComponentLeaf = false;

//        Point point = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(xPos, yPos)}), GeometryOperations.factory);
//        double min = Double.MAX_VALUE;
//        DTriangle minTriangle = null;
//
//        for (DTriangle dt: currentComponent) {
//            Coordinate[] coordinates = new Coordinate[]{
//                    new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY()),
//                    new Coordinate(dt.getPoint(1).getX(), dt.getPoint(1).getY()),
//                    new Coordinate(dt.getPoint(2).getX(), dt.getPoint(2).getY()),
//                    new Coordinate(dt.getPoint(0).getX(), dt.getPoint(0).getY())
//            };
//
//            LinearRing lr = new LinearRing(new CoordinateArraySequence(coordinates), GeometryOperations.factory);
//            System.err.println("check: " + lr.distance(point));
//
//            if (lr.distance(point) < min) {
//                min = lr.distance(point);
//                minTriangle = dt;
//            }
//
//            System.err.println(dt);
//        }

        for (DTriangle dt : currentComponent) {
            if (dt.contains(new DPoint(xPos, yPos, 0)) && isLeaf(nodess.indexOf(dt))) {
                inComponentLeaf = true;
                break;
            }
//            if (dt.equals(minTriangle) && isLeaf(nodess.indexOf(dt))) {
//                inComponentLeaf = true;
//            }
        }

        PlannedPath moveToLeaf = null;
        ArrayList<Integer> childIndeces = new ArrayList<>();

        double[] discreteProbabilities;
        int[] indecesToGenerate;
        int totalLeafSum, currentLeafCount;
        int chosenLeafIndex = -1;

        if (!inComponentLeaf) {
            // only in this case is it necessary to first move to a leaf

            // get all the leaves in that component
            for (int i = 0; i < currentComponent.size(); i++) {
                if (isLeaf(nodess.indexOf(currentComponent.get(i)))) {
                    childIndeces.add(i);
                    Label l = new Label("leaf");
                    l.setTranslateX(currentComponent.get(i).getBarycenter().getX());
                    l.setTranslateY(currentComponent.get(i).getBarycenter().getY());
                    //Main.pane.getChildren().add(l);
                } else {
                    Label l = new Label("not leaf");
                    l.setTranslateX(currentComponent.get(i).getBarycenter().getX());
                    l.setTranslateY(currentComponent.get(i).getBarycenter().getY());
                    //Main.pane.getChildren().add(l);
                }
            }

            // choose one of the leaves uniformly at random (because all subtrees would have the same number of leaves anyway)
            chosenLeafIndex = childIndeces.get((int) (Math.random() * childIndeces.size()));
            moveToLeaf = shortestPathRoadMap.getShortestPath(xPos, yPos, currentComponent.get(chosenLeafIndex).getBarycenter().getX(), currentComponent.get(chosenLeafIndex).getBarycenter().getY());
            //System.err.println("moveToLeaf: " + moveToLeaf);
            //System.err.println("moveToLeaf positions: " + xPos + ", " + yPos + "; " + currentComponent.get(chosenLeafIndex).getBarycenter().getX() + ", " + currentComponent.get(chosenLeafIndex).getBarycenter().getY());
            childIndeces.clear();
            //Main.pane.getChildren().add(new Circle(currentComponent.get(chosenLeafIndex).getBarycenter().getX(), currentComponent.get(chosenLeafIndex).getBarycenter().getY(), 7, Color.BROWN));
        }

        // chooses a path through the tree/map according to the random selection described in the paper
        int startIndex = inComponentLeaf ? getNodeIndex(xPos, yPos) : nodess.indexOf(currentComponent.get(chosenLeafIndex));
        int currentIndex = startIndex;
        int lastIndex = currentIndex;
        int counter = 1;
        while (currentIndex == startIndex || !isLeaf(currentIndex)) {
            // collect the children (not going back)
            for (int i = 0; i < adjacencyMatrix[0].length; i++) {
                if (i != currentIndex && i != lastIndex && (restrictToPocket ? pocketAdjacencyMatrix : adjacencyMatrix)[currentIndex][i] == 1) {
                    childIndeces.add(i);
                }
            }
            if (childIndeces.size() == 0) {
                break;
            }
            if (PRINT_PATH_CONSTRUCT) {
                System.out.println("\nChildren on iteration " + counter++);
                for (Integer i : childIndeces) {
                    System.out.print("Index: " + i + ", ");
                    //nodes.get(i).print();
                }
            }
            //System.out.println("Leaf nodes from root " + currentIndex + " (with parent node " + lastIndex + "): " + subTreeLeafNodes(currentIndex, lastIndex));

            // get new node to visit
            lastIndex = currentIndex;
            discreteProbabilities = new double[childIndeces.size()];
            indecesToGenerate = new int[childIndeces.size()];
            totalLeafSum = 0;
            for (int i = 0; i < childIndeces.size(); i++) {
                currentLeafCount = subTreeLeafNodes(childIndeces.get(i), lastIndex);
                discreteProbabilities[i] = currentLeafCount;
                indecesToGenerate[i] = childIndeces.get(i);
                totalLeafSum += currentLeafCount;
            }
            //System.out.println("Child probabilities (root " + currentIndex + "):");
            for (int i = 0; i < indecesToGenerate.length; i++) {
                //System.out.print(indecesToGenerate[i] + " | " + discreteProbabilities[i] + " | ");
                discreteProbabilities[i] /= totalLeafSum;
                //System.out.println(discreteProbabilities[i]);
            }
            try {
                rng = new EnumeratedIntegerDistribution(indecesToGenerate, discreteProbabilities);
            } catch (MathArithmeticException e) {
                /*System.err.println("Adjacency matrix:");
                for (int i = 0; i < adjacencyMatrix.length; i++) {
                    for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                        if ((restrictToPocket ? pocketAdjacencyMatrix : adjacencyMatrix)[i][j] == 1) {
                            System.out.println("Adjacent: i=" + i + ", j=" + j);
                        }
                    }
                }
                System.err.println("Discrete probabilities:");
                for (int i = 0; i < discreteProbabilities.length; i++) {
                    System.err.println(indecesToGenerate[i] + " | " + discreteProbabilities[i]);
                }*/
                ExperimentConfiguration.interruptCurrentRun();
                //e.printStackTrace();
            }
            currentIndex = rng.sample(); // needs proper probability distribution
            childIndeces.clear();
        }
        //PlannedPath plannedPath = shortestPathRoadMap.getShortestPath(new Point2D(xPos, yPos), new Point2D(nodess.get(currentIndex).getBarycenter().getEstX(), nodess.get(currentIndex).getBarycenter().getEstY()));
        PlannedPath plannedPath;
        if (!inComponentLeaf) {
            plannedPath = (restrictToPocket ? pocketShortestPathRoadMap : restrictedShortestPathRoadMap).getShortestPath(new Point2D(nodess.get(startIndex).getBarycenter().getX(), nodess.get(startIndex).getBarycenter().getY()), new Point2D(nodes.get(currentIndex).getTriangle().getBarycenter().getX(), nodes.get(currentIndex).getTriangle().getBarycenter().getY()));
            if (plannedPath == null) {
                //return getRandomTraversal(xPos, yPos);
                System.err.println("First case (startIndex: " + startIndex + ", currentIndex: " + currentIndex + " " + isLeaf(currentIndex) + ")");
                return null;
            }
            moveToLeaf.addPathToEnd(plannedPath);
            plannedPath = moveToLeaf;
        } else {
            plannedPath = (restrictToPocket ? pocketShortestPathRoadMap : restrictedShortestPathRoadMap).getShortestPath(new Point2D(xPos, yPos), new Point2D(nodess.get(currentIndex).getBarycenter().getX(), nodess.get(currentIndex).getBarycenter().getY()));
            if (plannedPath == null) {
                //return getRandomTraversal(xPos, yPos);
                System.err.println("Second case (startIndex: " + startIndex + " " + isLeaf(startIndex) + ", currentIndex: " + currentIndex + " " + isLeaf(currentIndex) + ")");
                return null;
            }
            //graphics.getChildren().add(new Circle(nodess.get(currentIndex).getBarycenter().getX(), nodess.get(currentIndex).getBarycenter().getY(), 7, Color.BLACK));
        }
        plannedPath.setStartIndex(startIndex);
        plannedPath.setEndIndex(currentIndex);
        return plannedPath;
    }

    public PlannedPath getRandomTraversal(TGNode startLeaf) {
        return getRandomTraversal(nodes.indexOf(startLeaf));
    }

    public PlannedPath getRandomTraversal(int startInd) {
        // chooses a path through the tree/map according to the random selection described in the paper
        ArrayList<Integer> path = new ArrayList<>();
        PlannedPath plannedPath = new PlannedPath();
        plannedPath.setStartIndex(startInd);
        int startIndex = startInd;
        int currentIndex = startIndex;
        int lastIndex = currentIndex;
        ArrayList<Integer> childIndeces = new ArrayList<>();
        path.add(startIndex);
        int counter = 1;
        while (currentIndex == startIndex || !isLeaf(currentIndex)) {
            // collect the children (not going back)
            for (int i = 0; i < adjacencyMatrix[0].length; i++) {
                if (i != currentIndex && i != lastIndex && adjacencyMatrix[currentIndex][i] == 1) {
                    childIndeces.add(i);
                }
            }
            if (PRINT_PATH_CONSTRUCT) {
                System.out.println("\nChildren on iteration " + counter++);
                for (Integer i : childIndeces) {
                    System.out.print("Index: " + i + ", ");
                    nodes.get(i).print();
                }
            }
            // get new node to visit
            lastIndex = currentIndex;
            currentIndex = childIndeces.get((int) (Math.random() * childIndeces.size())); // needs proper probability distribution
            //plannedPath.addLine(adjacencyLineMatrix[lastIndex][currentIndex]);
            PathLine l1 = null, l2 = null;
            try {
                l1 = new PathLine(nodes.get(lastIndex).getTriangle().getBarycenter().getX(), nodes.get(lastIndex).getTriangle().getBarycenter().getY(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getX(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getY());
                l2 = new PathLine(adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getX(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getY(), nodes.get(currentIndex).getTriangle().getBarycenter().getX(), nodes.get(currentIndex).getTriangle().getBarycenter().getY());
            } catch (DelaunayError delaunayError) {
                delaunayError.printStackTrace();
            }
            plannedPath.addLine(l1);
            plannedPath.addLine(l2);
            path.add(currentIndex);
            childIndeces.clear();
        }
        plannedPath.setEndIndex(currentIndex);
        plannedPath.getPathLines().remove(0);
        if (PRINT_PATH_CONSTRUCT) {
            System.out.println("\nFinal path:");
            for (Integer i : path) {
                System.out.print("Index: " + i + ", ");
                nodes.get(i).print();
            }
        }
        plannedPath = shortestPathRoadMap.getShortestPath(new Point2D(plannedPath.getStartX(), plannedPath.getStartY()), new Point2D(plannedPath.getEndX(), plannedPath.getEndY()));
        plannedPath.setStartIndex(startIndex);
        plannedPath.setEndIndex(currentIndex);
        return plannedPath;
    }

    private int subTreeLeafNodes(int rootNodeIndex, int blockedNodeIndex) {
        boolean unexploredLeft = true;
        ArrayList<Integer> currentLayer = new ArrayList<>();
        currentLayer.add(rootNodeIndex);
        ArrayList<Integer> nextLayer;

        boolean[] visitedNodes = new boolean[adjacencyMatrix.length];
        int[] parentNodes = new int[adjacencyMatrix.length];
        parentNodes[rootNodeIndex] = blockedNodeIndex;

        int nrLeafNodes = 0;
        while (unexploredLeft) {
            nextLayer = new ArrayList<>();
            for (int i : currentLayer) {
                visitedNodes[i] = true;
                int childrenCount = 0;
                for (int j = 0; j < adjacencyMatrix.length; j++) {
                    // TODO: also account for the case where its not a pocket but it is a line separated map
                    if ((restrictToPocket ? pocketAdjacencyMatrix : adjacencyMatrix)[i][j] == 1 && j != parentNodes[i] && !visitedNodes[j]) {
                        nextLayer.add(j);
                        parentNodes[j] = i;
                        visitedNodes[j] = true;
                        childrenCount++;
                    }
                }
                if (childrenCount == 0) {
                    nrLeafNodes++;
                }
            }
            currentLayer = nextLayer;
            if (nextLayer.size() == 0) {
                unexploredLeft = false;
            }
        }
        return nrLeafNodes;
    }

    public void printAdjacencyMatrix() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                System.out.print(adjacencyMatrix[i][j] == 1 ? "1  " : "0  ");
            }
            System.out.println();
        }
    }

}

