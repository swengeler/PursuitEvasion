package simulation;

import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.*;

import java.util.ArrayList;
import java.util.List;

public class DummyPolicy extends MovePolicy {

    private SimplyConnectedTree tree;
    private PlannedPath currentPath;

    ArrayList<Line> test;
    int testCounter;

    public DummyPolicy(Agent agent, boolean pursuing) {
        super(agent, pursuing);
        // construct tree from (simple) map
    }

    @Override
    public Move getNextMove(MapRepresentation map, ArrayList<Agent> agents) {
        // if there is no "fixed plan" currently (for the round), compute a new one, i.e. a path to follow trough the map (tree)
        // the current path to follow changes if a) it is finished or b) an evader is spotted

        if (tree == null) {
            // initialise the tree and get an initial path to move to a leaf node
            initTree(map);
            currentPath = tree.getRandomTraversal(tree.getNode(getSingleAgent().getXPos(), getSingleAgent().getYPos()));
            currentPath.addInitLine(new Line(getSingleAgent().getXPos(), getSingleAgent().getYPos(), currentPath.getStartX(), currentPath.getStartY()));
            test = currentPath.getPathLines();
            testCounter = 0;
            //return new Move(currentPath.getStartX() - getSingleAgent().getXPos(), currentPath.getStartY() - getSingleAgent().getYPos(), 0);
        }

        if (tree == null) {
            System.exit(-1);
        }
        if (getSingleAgent() == null) {
            System.exit(-2);
        }
        if (currentPath == null) {
            System.exit(-3);
        }
        if (tree.getNodeIndex(getSingleAgent().getXPos(), getSingleAgent().getYPos()) == currentPath.getEndIndex()) {
            // TODO: there should probably be a better check that takes into account that an entire branch might be cleared if there is vision of it
            // end of path reached, compute new path
            currentPath = tree.getRandomTraversal(tree.getNode(getSingleAgent().getXPos(), getSingleAgent().getYPos()));
            currentPath.addInitLine(new Line(getSingleAgent().getXPos(), getSingleAgent().getYPos(), currentPath.getStartX(), currentPath.getStartY()));
            test = currentPath.getPathLines();
            testCounter = 0;
        }

        boolean agentVisible = false;
        for (Agent a : agents) {
            if (a != getSingleAgent() && a.isEvader() && map.isVisible(getSingleAgent().getXPos(), getSingleAgent().getYPos(), a.getXPos(), a.getYPos())) {
                System.out.println("Evader is visible!");
                agentVisible = true;
                break;
            }
        }
        if (!agentVisible) {
            System.out.println("Evader is not visible.");
        }

        // move along path
        Move result;
        double length = Math.sqrt(Math.pow(test.get(testCounter).getEndX() - test.get(testCounter).getStartX(), 2) + Math.pow(test.get(testCounter).getEndY() - test.get(testCounter).getStartY(), 2));
        double deltaX = (test.get(testCounter).getEndX() - test.get(testCounter).getStartX()) / length * getSingleAgent().getSpeed() / 50;
        double deltaY = (test.get(testCounter).getEndY() - test.get(testCounter).getStartY()) / length * getSingleAgent().getSpeed() / 50;
        if (test.get(testCounter).contains(getSingleAgent().getXPos() + deltaX, getSingleAgent().getYPos() + deltaY)) {
            // move along line
            result = new Move(deltaX, deltaY, 0);
        } else {
            result = new Move(test.get(testCounter).getEndX() - getSingleAgent().getXPos(), test.get(testCounter).getEndY() - getSingleAgent().getYPos(), 0);
            testCounter++;
        }
        return result;
        //return new Move(0, 0, 0);
    }

    private void initTree(MapRepresentation map) {
        try {
            ArrayList<DEdge> constraintEdges = new ArrayList<>();
            ArrayList<Line> polygonEdges = map.getPolygonEdges();
            ArrayList<Polygon> polygons = map.getAllPolygons();
            for (Line l : polygonEdges) {
                constraintEdges.add(new DEdge(new DPoint(l.getStartX(), l.getStartY(), 0), new DPoint(l.getEndX(), l.getEndY(), 0)));
            }

            ConstrainedMesh mesh = new ConstrainedMesh();
            mesh.setConstraintEdges(constraintEdges);
            mesh.processDelaunay();
            List<DTriangle> triangles = mesh.getTriangleList();
            List<DTriangle> includedTriangles = new ArrayList<>();

            for (DTriangle dt : triangles) {
                // check if triangle in polygon
                double centerX = dt.getBarycenter().getX();
                double centerY = dt.getBarycenter().getY();
                boolean inPolygon = true;
                if (!polygons.get(0).contains(centerX, centerY)) {
                    inPolygon = false;
                }
                for (int i = 1; inPolygon && i < polygons.size() - 1; i++) {
                    if (polygons.get(i).contains(centerX, centerY)) {
                        inPolygon = false;
                    }
                }
                if (inPolygon) {
                    includedTriangles.add(dt);
                }
            }

            tree = new SimplyConnectedTree((ArrayList<DTriangle>) includedTriangles);
        } catch (DelaunayError e) {
            e.printStackTrace();
        }
    }

}
