package simulation;

import javafx.scene.shape.Line;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;

import java.util.ArrayList;

public class SimplyConnectedTree {

    private static final boolean PRINT_PATH_CONSTRUCT = false;

    private ArrayList<TGNode> nodes;
    private boolean[][] adjacencyMatrix;

    private Line[][] adjacencyLineMatrix;
    private DEdge[][] adjacencyEdgeMatrix;

    public SimplyConnectedTree(ArrayList<DTriangle> triangles) {
        init(triangles);
    }

    private void init(ArrayList<DTriangle> triangles) {
        // initialising list of nodes and adjacency matrix
        nodes = new ArrayList<>(triangles.size());
        for (DTriangle dt : triangles) {
            nodes.add(new TGNode(dt));
        }
        adjacencyMatrix = new boolean[nodes.size()][nodes.size()];
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
                            adjacencyMatrix[i][neighbourIndex] = true;
                            adjacencyMatrix[neighbourIndex][i] = true;
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

    public TGNode getLeaf() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            int count = 0;
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                if (adjacencyMatrix[i][j]) {
                    count++;
                }
            }
            if (count == 1) {
                /*try {
                    System.out.println("\nReturning leaf of simply connected tree - Index: " + i + ", Middle: (" + nodes.get(i).getTriangle().getBarycenter().getX() + "|" + nodes.get(i).getTriangle().getBarycenter().getY() + ")");
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
            if (adjacencyMatrix[index][i]) {
                count++;
            }
        }
        return count == 1;
    }

    public TGNode getLeafNeighbour(TGNode leaf) {
        if (isLeaf(leaf)) {
            for (int i = 0; i < adjacencyMatrix[0].length; i++) {
                if (adjacencyMatrix[nodes.indexOf(leaf)][i]) {
                    return nodes.get(i);
                }
            }
        }
        return null;
    }

    public TGNode getNode(double xCoord, double yCoord) {
        try {
            for (TGNode n : nodes) {
                if (n.getTriangle().contains(new DPoint(xCoord, yCoord, 0))) {
                    return n;
                }
            }
        } catch (DelaunayError e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getNodeIndex(double xCoord, double yCoord) {
        return nodes.indexOf(getNode(xCoord, yCoord));
    }

    public ArrayList<TGNode> getNodes() {
        return getNodes();
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
                if (i != currentIndex && i != lastIndex && adjacencyMatrix[currentIndex][i]) {
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
            Line l1 = null, l2 = null;
            try {
                l1 = new Line(nodes.get(lastIndex).getTriangle().getBarycenter().getX(), nodes.get(lastIndex).getTriangle().getBarycenter().getY(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getX(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getY());
                l2 = new Line(adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getX(), adjacencyEdgeMatrix[lastIndex][currentIndex].getBarycenter().getY(), nodes.get(currentIndex).getTriangle().getBarycenter().getX(), nodes.get(currentIndex).getTriangle().getBarycenter().getY());
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
        return plannedPath;
    }

    public void printAdjacencyMatrix() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                System.out.print(adjacencyMatrix[i][j] ? "1  " : "0  ");
            }
            System.out.println();
        }
    }

}

