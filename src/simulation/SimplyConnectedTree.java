package simulation;

import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;

import java.util.ArrayList;

public class SimplyConnectedTree {

    private ArrayList<TGNode> nodes;
    private boolean[][] adjacencyMatrix;

    private TPLine[][] adjacencyLineMatrix;

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
        adjacencyLineMatrix = new TPLine[nodes.size()][nodes.size()];

        // checking for adjacency between nodes
        ArrayList<DEdge> checkedEdges = new ArrayList<>();
        DTriangle dt1, dt2;
        DEdge de1, de2;
        for (int i = 0; i < triangles.size(); i++) {
            dt1 = triangles.get(i);
            // go through the edges of each triangle
            for (int j = 0; j < 3; j++) {
                de1 = dt1.getEdge(j);
                if (!checkedEdges.contains(de1)) {
                    int neighbourIndex = -1;
                    for (int k = 0; neighbourIndex == -1 && k < triangles.size(); k++) {
                        dt2 = triangles.get(k);
                        if (k != i) {
                            for (int l = 0; neighbourIndex == -1 && l < 3; l++) {
                                de2 = dt2.getEdge(l);
                                // if the current triangle shares an edge with another triangle, they are neighbours in the graph
                                if (de1 == de2) {
                                    neighbourIndex = k;
                                }
                            }
                        }
                    }
                    if (neighbourIndex != -1) {
                        try {
                            adjacencyMatrix[i][neighbourIndex] = true;
                            adjacencyMatrix[neighbourIndex][i] = true;
                            adjacencyLineMatrix[i][neighbourIndex] = new TPLine(dt1.getBarycenter().getX(), dt1.getBarycenter().getY(), triangles.get(neighbourIndex).getBarycenter().getX(), triangles.get(neighbourIndex).getBarycenter().getY());
                            adjacencyLineMatrix[neighbourIndex][i] = new TPLine(triangles.get(neighbourIndex).getBarycenter().getX(), triangles.get(neighbourIndex).getBarycenter().getY(), dt1.getBarycenter().getX(), dt1.getBarycenter().getY());
                        } catch (DelaunayError e) {
                            e.printStackTrace();
                        }
                    }
                    checkedEdges.add(de1);
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
                try {
                    System.out.println("Returning leaf of simply connected tree - Index: " + i + ", Middle: (" + nodes.get(i).getTriangle().getBarycenter().getX() + "|" + nodes.get(i).getTriangle().getBarycenter().getY() + ")");
                } catch (DelaunayError e) {
                    e.printStackTrace();
                }
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

    public Object getRandomTraversal(TGNode startLeaf) {
        // chooses a path through the tree/map according to the random selection described in the paper
        ArrayList<Integer> path = new ArrayList<>();
        int startIndex = nodes.indexOf(startLeaf);
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
            System.out.println("\nChildren on iteration " + counter++);
            for (Integer i : childIndeces) {
                System.out.print("Index: " + i + ", ");
                nodes.get(i).print();
            }
            // get new node to visit
            lastIndex = currentIndex;
            currentIndex = childIndeces.get((int) (Math.random() * childIndeces.size()));
            path.add(currentIndex);
            childIndeces.clear();
        }
        System.out.println("\nFinal path:");
        for (Integer i : path) {
            System.out.print("Index: " + i + ", ");
            nodes.get(i).print();
        }
        return null;
    }

    public void printAdjacencyMatrix() {
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = 0; j < adjacencyMatrix[0].length; j++) {
                System.out.print(adjacencyMatrix[i][j] ? "1  " : "0  ");
            }
            System.out.println();
        }
    }

    class TPLine {

        double startX, startY, endX, endY;

        TPLine(double startX, double startY, double endX, double endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

    }

}

