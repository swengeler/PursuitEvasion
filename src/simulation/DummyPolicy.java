package simulation;

import com.sun.xml.internal.bind.v2.TODO;
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
            currentPath = tree.getRandomTraversal(tree.getNode(agent.getXPos(), agent.getYPos()));
            currentPath.addInitLine(new Line(agent.getXPos(), agent.getYPos(), currentPath.getStartX(), currentPath.getStartY()));
            test = currentPath.getPathLines();
            testCounter = 0;
            //return new Move(currentPath.getStartX() - agent.getXPos(), currentPath.getStartY() - agent.getYPos(), 0);
        }

        if (tree.getNodeIndex(agent.getXPos(), agent.getYPos()) == currentPath.getEndIndex()) {
            // TODO: there should probably be a better check that takes into account that an entire branch might be cleared if there is vision of it
            // end of path reached, compute new path
            currentPath = tree.getRandomTraversal(tree.getNode(agent.getXPos(), agent.getYPos()));
            currentPath.addInitLine(new Line(agent.getXPos(), agent.getYPos(), currentPath.getStartX(), currentPath.getStartY()));
            test = currentPath.getPathLines();
            testCounter = 0;
        }

        // move along path
        Move result = new Move(test.get(testCounter).getEndX() - agent.getXPos(), test.get(testCounter).getEndY() - agent.getYPos(), 0);
        testCounter++;
        return result;
        //return new Move(0, 0, 0);
    }

    private void initTree(MapRepresentation map) {
        try {
            ArrayList<DEdge> constraintEdges = new ArrayList<>();
            ArrayList<Polygon> polygons = map.getAllPolygons();
            for (Polygon p : polygons) {
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
