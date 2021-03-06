package shadowPursuit;

import entities.Tree;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.*;

/**
 * Created by jonty on 16/06/2017.
 */
public class PursuitTree {

    MapRepresentation map;
    ArrayList<Point2D> agents;
    ArrayList<WayPoint> graph;
    ArrayList<Line> raystopasson;
    TreeNode root;
    ArrayList<TreeNode> children;
    TreeNode Parent;
    double oldBest;

    ArrayList<Line> envEdges;

    private int depthLevel;


    ArrayList<Line> connections;
    TreeNode bestNode;
    ShadowGraph shadowGraph;
    ArrayList<TreeNode> nodes;


    public PursuitTree(MapRepresentation map, ArrayList<Point2D> agents) {
        this.map = map;
        this.agents = agents;

        envEdges = new ArrayList<>();

        for (int i = 0; i < map.getPolygonEdges().size(); i++) {
            envEdges.add(map.getPolygonEdges().get(i));
        }
        bestNode = null;


       /* for(int i=0;i<agents.size();i++) {

            this.graph.add(new WayPoint(agents.get(i)));

        }
        root = new TreeNode(this.graph);
        connectRoottoChildren(root.agentPositions);
        */
        calculateNodes();
        shadowGraph = new ShadowGraph(map, agents);
        depthLevel = graph.size();
        nodes = new ArrayList<>();


        //System.arraycopy(map.getPolygonEdges(), 0, envEdges, 0, map.getPolygonEdges().size());
        // buildTree();
    }

    public void buildTree() {
        double mindDist;
        WayPoint tempPoint;
        ///System.out.println(11111111);
        ArrayList<WayPoint> startPoints = new ArrayList<>();

        for (Point2D agent : agents) {
            mindDist = Double.MAX_VALUE;
            tempPoint = null;


            for (WayPoint wayP : graph) {
                if (distance(agent, wayP.getCoord()) < mindDist && isVisible(agent.getX(), agent.getY(), wayP.getCoord().getX(), wayP.getCoord().getY(), envEdges)) {
                    mindDist = distance(agent, wayP.getCoord());
                    tempPoint = wayP;
                }
            }

            startPoints.add(tempPoint);
        }


        root = new TreeNode(startPoints, map, new ShadowGraph(map, startPoints, true).getShadows());
        //System.out.println(22222);
    }


    public void sortArray(ArrayList<TreeNode> tosort) {//go through the array and sort from smallest to highest
        for (int i = 1; i < tosort.size(); i++) {
            TreeNode temp = null;
            if (tosort.get(i - 1).score > tosort.get(i).score) {
                temp = tosort.get(i - 1);
                tosort.set(i - 1, tosort.get(i));
                tosort.set(i, temp);
            }
        }
    }

    public void test2() {
        buildTree();
        //System.out.println(3333333);
        //root.addChildren();
        nodes.add(root);
        //System.out.println(44444444);


        //TreeNode child = null;
        TreeNode current = root;
        nodes.add(root);


        //System.out.println("Lowest => " + getLowestVal().getScore());
        //System.out.println("root => " + root);

        int curdepth = 1;
        int count = 0;

        int counter;
        ArrayList<TreeNode> temp = new ArrayList<>();
        boolean expanding = true;

        bestNode = getLowestVal();


        /*
        while (bestNode.score > 0.5) {
            //sortArray(bestToContinue);
            for (int i = 0; i < bestToContinue.size(); i++) {

                //System.out.println("doing shit");
                if (bestToContinue.size() > i) {
                    //  System.out.println("doing shit");
                    bestToContinue.get(i);
                    if (bestToContinue.get(i).getchildren() == null) {
                        if (bestToContinue.get(i).getParent().getParent() != null) {
                            if (bestToContinue.get(i).score <= bestToContinue.get(i).getParent().getParent().getScore()) {

                                bestToContinue.get(i).addChildren();

                            } else expanding = false;
                        } else bestToContinue.get(i).addChildren();
                    }
                    if (expanding) {
                        for (TreeNode node : bestToContinue.get(i).getchildren()) {
                            if (!temp.contains(node)) {
                                temp.add(node);
                            }
                            if (!nodes.contains(node)) {
                                nodes.add(node);
                            }
                        }
                    }else
                        expanding=true;


                }
            }
            curdepth++;

            bestToContinue.clear();
            for (TreeNode node : temp) {
                bestToContinue.add(node);
                count++;
            }

            System.out.println("count:" + count);
            count=0;
            System.out.println("Best:\n" + getLowestVal(bestToContinue));
            temp.clear();

            bestNode = getLowestVal(bestToContinue);
        }
        */
        current = root;
        oldBest = Double.MAX_VALUE;
        double breakCounter = 60000;
        while(getLowestVal().getScore()  > 0 && nodes.size() < breakCounter) {
            counter = 0;
            for(int i = 0; i < nodes.size() && nodes.size() < breakCounter; i++) {
                current = nodes.get(i);
                if (current.getDepth() == curdepth && (current.getchildren() == null || current.getchildren().size() == 0)) {
                    counter++;

                    current.addChildren();
                    for (TreeNode child : current.getchildren()) {
                        nodes.add(child);
                    }
                    System.out.println("\t" + nodes.size());

                }
            }
            System.out.println("For Depth: " + curdepth);
            System.out.println("Number of Nodes at this depth: " + counter + "\n");
            curdepth++;

            if(bestNode.getScore() < oldBest)   {
                System.out.println("New best Score at Depth: " + (curdepth-1));
                System.out.println("Best Node: " + bestNode);
                oldBest = bestNode.getScore();
            }
        }


        System.out.println("Best Node: " + bestNode);
        if(bestNode.getchildren() != null)  {
            for(int i = 0; i < bestNode.getchildren().size(); i++)    {
                System.out.println("Child number: " + (i+1));
                System.out.println(bestNode.getchildren().get(i));
            }
        }






    }

    public TreeNode getLowestVal() {
        double minValue = Double.MAX_VALUE;
        TreeNode tmp = null;
        for (TreeNode node : nodes) {
            if (node.getScore() < minValue) {
                minValue = node.getScore();
                tmp = node;
            }
        }
        bestNode = tmp;

        return tmp;
    }

    public void getBestAtDepth(int depth, int numOfNodes) {
        ArrayList<TreeNode> best = new ArrayList<>();
        double maxVal = Double.MIN_NORMAL;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getDepth() == depth) {
            }
            //if()
        }
    }

    public TreeNode getLowestVal(ArrayList<TreeNode> checking) {
        double minValue = Double.MAX_VALUE;
        TreeNode tmp = null;
        for (TreeNode node : checking) {
            if (node.getScore() < minValue) {
                minValue = node.getScore();
                tmp = node;
            }
        }

        return tmp;
    }


    public ArrayList<Point2D> getStart() {
        ArrayList<Point2D> temp = new ArrayList<>();
        for (WayPoint wayP : root.getWayPoints()) {
            temp.add(wayP.getCoord());
        }
        return temp;
    }

    public ArrayList<ArrayList<WayPoint>> getPath() {
        if (bestNode != null) {
            ArrayList<ArrayList<WayPoint>> paths = new ArrayList<>();
            //From Goal to start, meaning => in reversed order
            TreeNode tmp = bestNode;
            paths.add(tmp.getWayPoints());
            while (tmp.getParent() != null) {
                tmp = tmp.getParent();
                paths.add(tmp.getWayPoints());
            }
            return paths;
        } else {
            return null;
        }
    }


    /*
    public void test() {



        ArrayList<WayPoint> startPoints = new ArrayList<>();
        WayPoint tmpPoint;


        for (Point2D agent : agents) {
            tmpPoint = new WayPoint(agent);

            for (int j = 0; j < graph.size(); j++) {
                if (isVisible(agent.getX(), agent.getY(), graph.get(j).getX(), graph.get(j).getY(), map.getPolygonEdges())) {

                    boolean crossed = false;
                    Line crossOcc = new Line(agent.getX(), agent.getY(), graph.get(j).getX(), graph.get(j).getY());
                    for (int q = 0; q < raystopasson.size(); q++) {
                        if (lineIntersect(crossOcc, raystopasson.get(q))) {
                            if (!onLine(graph.get(j).getX(), graph.get(j).getY(), raystopasson.get(q))) {
                                crossed = true;
                            }
                        }
                    }
                    if (crossed == false) {

                        tmpPoint.addConnection(graph.get(j));
                        System.out.println("tmpoint= " + tmpPoint.getCoord() + " graph= " + graph.get(j).coordinate);
                       // System.exit(1234);
                    }


                }


            }
            startPoints.add(tmpPoint);
        }


        root = new TreeNode(startPoints, shadowGraph.getShadows());

        /*
        for (int i = 0; i < root.getWayPoints().size(); i++) {
            System.out.print("Waypoint => ");
            root.getWayPoints().get(i).printWayPoint();
            System.out.println();
        }

        contaminationFor(root.getWayPoints().get(0));
        System.out.println("REACHED!!!");


       System.out.println("number of contaminated shadows= " +  root.contShadows.size());


        int childrenSize = 1;
        for (WayPoint wayP : root.getWayPoints()) {
            childrenSize *= wayP.connected.size();
        }

        ArrayList<int[]>  permuations = new ArrayList<>();
        int curPos = 0;
        WayPoint tmp;

        int entrySize = root.getWayPoints().size();

        while(permuations.size() < childrenSize)    {
            int[] perm = new int[entrySize];

            for(int i = 0; i < root.getWayPoints().size(); i++) {
                curPos = 0;
                tmp = root.getWayPoints().get(i);

                for(int j = 0;j < tmp.connected.size() && curPos < entrySize; j++)    {
                    perm[curPos] = j+1;
                    curPos++;
                }

                if(!permuations.contains(perm)) {
                    permuations.add(perm);
                }


            }
        }



    }

*/


    public void contaminationFor(WayPoint wayP) {
        boolean directlyCon = false;

        for (WayPoint p : getWayPoints()) {
            for (WayPoint p2 : p.connected) {
                if (p2 == wayP) {
                    directlyCon = true;
                    break;
                }
            }
            if (directlyCon) {
                break;
            }
        }

        if (directlyCon) {
            ArrayList<Point2D> points = new ArrayList<>();
            points.add(wayP.getCoord());
            root.addChild(new TreeNode(root, wayP.connected, new ShadowGraph(map, points).getShadows()));
            //System.out.println("NEW TREENODE => " + root.children.get(0));
        }
    }


    public void calculateNodes() {
        ArrayList<Point2D> pointy = findReflex(map.getBorderPolygon(), map.getAllPolygons(), map.getObstaclePolygons());
        ArrayList<Line> rays = new ArrayList<>();
        raystopasson = new ArrayList<>();
        graph = new ArrayList<>();

        ArrayList<Line> raystoremove = new ArrayList<>();
        ArrayList<Point2D> locationsOfPursuers = new ArrayList<>();


        for (int i = 0; i < pointy.size(); i++) {
            ArrayList<Point2D> adj = getAdjacentPoints(pointy.get(i), map.getAllPolygons());
            //   Line left= new Line(adj.get(0).getX(),adj.get(0).getY(),pointy.get(i).getX(),pointy.get(i).getY());
            //   Line right= new Line(adj.get(1).getX(),adj.get(1).getY(),pointy.get(i).getX(),pointy.get(i).getY());

            Line left = scaleRay(new Point2D(adj.get(0).getX(), adj.get(0).getY()), new Point2D(pointy.get(i).getX(), pointy.get(i).getY()), 1000);
            Line right = scaleRay(new Point2D(adj.get(1).getX(), adj.get(1).getY()), new Point2D(pointy.get(i).getX(), pointy.get(i).getY()), 1000);

            Point2D l = getClosestIntersect(left, map.getPolygonEdges());
            Point2D r = getClosestIntersect(right, map.getPolygonEdges());
            left = new Line(pointy.get(i).getX(), pointy.get(i).getY(), l.getX(), l.getY());
            right = new Line(pointy.get(i).getX(), pointy.get(i).getY(), r.getX(), r.getY());

            rays.add(left);
            rays.add(right);
            //find its neighbours left and right.
            //create ray from neigbour to it and extend
            // Line ray= new Line(pointy.get(i).getX(),pointy.get(i).getY(),)
            // rays.add(ray)
            //check any intersections and set them as points
            //then find any midpoints between the pointy and first intersection
            //set that as a point

        }


        for (int i = 0; i < rays.size(); i++) {
            raystopasson.add(rays.get(i));
        }

        for (int i = 0; i < rays.size(); i++) {
            for (int j = 0; j < rays.size(); j++) {
                if (i != j) {
                    if (lineIntersect(rays.get(i), rays.get(j)) == true) {
                        Point2D a = findIntersect2(rays.get(i), rays.get(j));
                        raystoremove.add(rays.get(i));
                        raystoremove.add(rays.get(j));

                        ArrayList<Line> b = new ArrayList<>();
                        b.add(rays.get(i));
                        b.add(rays.get(j));
                        // graph.add(new WayPoint(a));
                        if (j > i) {
                            graph.add(new WayPoint(a, b));
                            locationsOfPursuers.add(a);
                        }
                    }
                }
            }
        }
        // System.out.println("removing this many rays= " + raystoremove.size());
        for (int i = 0; i < raystoremove.size(); i++) {
            if (rays.contains(raystoremove.get(i))) {
                rays.remove(raystoremove.get(i));
            }
        }
        for (int i = 0; i < rays.size(); i++) {
            Point2D a = getLineMiddle(rays.get(i));
            ArrayList<Line> b = new ArrayList<>();
            b.add(rays.get(i));
            //graph.add(new WayPoint(a));
            graph.add(new WayPoint(a, b));
            locationsOfPursuers.add(a);
        }

        // connectNodes(locationsOfPursuers, raystopasson);
        connectNodes(raystopasson);
    }

    public void connectNodes(ArrayList<Line> rays) {

       /* graph = new ArrayList<>();
         for (int i = 0; i < locations.size(); i++) {
            graph.add(new WayPoint(locations.get(i)));
        }*/

        int count = 0;

        int count2 = 0;
        int count3 = 0;


        connections = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            for (int j = 0; j < graph.size(); j++) {
                if (i != j) {
                    if (isVisible(graph.get(i).getX(), graph.get(i).getY(), graph.get(j).getX(), graph.get(j).getY(), map.getPolygonEdges())) {
                        count3++;
                        boolean crossed = false;
                        Line crossocc = new Line(graph.get(i).getX(), graph.get(i).getY(), graph.get(j).getX(), graph.get(j).getY());
                        for (int q = 0; q < rays.size(); q++) {
                            if (lineIntersect(crossocc, rays.get(q))) {
                                if (!graph.get(i).jontyRays.contains(rays.get(q)) && !graph.get(j).jontyRays.contains(rays.get(q))) {
                                    // if (!onLine(graph.get(i).getX(), graph.get(i).getY(), rays.get(q)) && !onLine(graph.get(j).getX(), graph.get(j).getY(), rays.get(q))){
                                    crossed = true;

                                }
                            }

                        }
                        if (crossed == false) {
                            Line newLine = new Line(graph.get(j).getX(), graph.get(j).getY(), graph.get(i).getX(), graph.get(i).getY());
                            if (!connections.contains(newLine)) {
                                connections.add(newLine);

                                // System.out.println(newLine);
                                count2++;
                            }


                        } else count++;
                    }
                }
            }
        }
        //System.out.println("count= " + count);
        // System.out.println("count2= " + count2);
        // System.out.println("count3= " + count3);

        int count4 = 0;
        int count5 = 0;
        for (int i = 0; i < graph.size(); i++) {
            for (int j = 0; j < graph.get(i).connected.size(); j++) {
                //  System.out.println("this= " + graph.get(i).getCoord() + " is connected to that= " + graph.get(i).connected.get(j).getCoord());
                count4++;
                Line newLine = new Line(graph.get(i).connected.get(j).getX(), graph.get(i).connected.get(j).getY(), graph.get(i).getX(), graph.get(i).getY());
                if (!connections.contains(newLine)) {
                    connections.add(newLine);

                    count5++;
                }
            }
        }
        //  System.out.println("count4= " + count4);
        //  System.out.println("count5= " + count5);

        createConnections();

    }

    public void createConnections() {
        for (int i = 0; i < graph.size(); i++) {
            //  System.out.println(graph.get(i));
        }
        for (Line line : connections) {
            //System.out.println("line start x= " + line.getStartX() );
            //System.out.println("line start y= " + line.getStartY() );
            //System.out.println("line end x= " + line.getEndX() );
            //System.out.println("line end y= " + line.getEndY() );


            if (getWayPointFromGraph(new Point2D(line.getStartX(), line.getStartY())) != null && getWayPointFromGraph(new Point2D(line.getEndX(), line.getEndY())) != null) {
                //System.out.println("Works for => " + new Point2D(line.getStartX(), line.getStartY())  + "\nAND => " + new Point2D(line.getEndX(), line.getEndY()));
                WayPoint wayP1 = getWayPointFromGraph(new Point2D(line.getStartX(), line.getStartY()));
                WayPoint wayP2 = getWayPointFromGraph(new Point2D(line.getEndX(), line.getEndY()));

                wayP1.addConnection(wayP2);
                wayP2.addConnection(wayP1);
            } else {
                for (int i = 0; i < graph.size(); i++) {
                    // System.out.println(graph.get(i));
                }
                //System.out.println("Doesn't work for  => " + new Point2D(line.getStartX(), line.getStartY())  + "\nOR => " + new Point2D(line.getEndX(), line.getEndY()));
                System.exit(888888);
            }
        }


    }

    public WayPoint getWayPointFromGraph(Point2D point) {
        for (WayPoint wayP : graph) {
            if (wayP.getX() == point.getX() && wayP.getY() == point.getY()) {
                return wayP;
            }
        }
        return null;

    }

    public ArrayList<Point2D> getPoints() {
        ArrayList<Point2D> points = new ArrayList<>();
        for (WayPoint way : graph) {
            //way.printWayPoint();
            points.add(way.getCoord());
        }
        return points;
    }

    public ArrayList<Point2D> getPointsClosest(Point2D position) {
        ArrayList<Point2D> points = new ArrayList<>();

        //Find closest one
        double midDist = Double.MAX_VALUE;
        WayPoint closest = null;

        for (WayPoint way : graph) {
            if (distance(position, way.getCoord()) < midDist) {
                closest = way;
                midDist = distance(position, way.getCoord());
            }
        }
        points.add(closest.getCoord());

        int breakCounter = 5;
        int i = 0;

        while (i < breakCounter) {
            for (int j = 0; j < closest.connected.size(); j++) {
                if (!points.contains(closest.connected.get(j).getCoord())) {
                    points.add(closest.connected.get(j).getCoord());
                }
            }
            if (closest.connected.size() > 1) {
                closest.connected.get(1);
            }
            i++;
        }

        if (points.size() != graph.size()) {
            for (int k = 0; k < graph.size(); k++) {
                if (!points.contains(graph.get(k).getCoord())) {
                    points.add(graph.get(k).getCoord());
                }
            }
        }
        return points;
    }

    public ArrayList<WayPoint> getWayPoints() {
        return graph;
    }


    public ArrayList<Polygon> getShadows(ArrayList<Point2D> positions) {
        this.shadowGraph = new ShadowGraph(map, positions);
        return shadowGraph.getShadows();
    }

    public ArrayList<Polygon> getShadows() {
        this.shadowGraph = new ShadowGraph(map, agents);
        return shadowGraph.getShadows();
    }

    public ArrayList<Polygon> getContShadows(TreeNode node) {
        node.getContShadows();
        return shadowGraph.getShadows();
    }

    public void printShadows() {
        this.shadowGraph.printShadows();
    }

    public ArrayList<Line> getRays() {
        return raystopasson;
    }

    public ArrayList<Line> getConnections() {
        return connections;
    }


}
