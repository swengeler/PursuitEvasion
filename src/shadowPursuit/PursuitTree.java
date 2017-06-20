package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.MapRepresentation;

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

    private int depthLevel;


    ArrayList<Line> connections;
    TreeNode treeNode;
    ShadowGraph shadowGraph;


    public PursuitTree(MapRepresentation map, ArrayList<Point2D> agents) {
        this.map = map;
        this.agents = agents;
       /* for(int i=0;i<agents.size();i++) {

            this.graph.add(new WayPoint(agents.get(i)));

        }
        root = new TreeNode(this.graph);
        connectRoottoChildren(root.agentPositions);
        */
        calculateNodes();
        shadowGraph = new ShadowGraph(map, agents);
        depthLevel = graph.size();
       // buildTree();
    }


    public void buildTree2() {
        double mindDist;
        WayPoint tempPoint;
        ArrayList<WayPoint> startPoints = new ArrayList<>();

        for (Point2D agent : agents) {
            mindDist = Double.MAX_VALUE;
            tempPoint = null;

            for (WayPoint wayP : graph) {
                if (distance(agent, wayP.getCoord()) < mindDist) {
                    mindDist = distance(agent, wayP.getCoord());
                    tempPoint = wayP;
                }
            }

            startPoints.add(tempPoint);
        }


    }

    public void buildTree() {
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
                            // if (!onLine(graph.get(i).getX(), graph.get(i).getY(), rays.get(q)) && !onLine(graph.get(j).getX(), graph.get(j).getY(), rays.get(q))){
                            crossed = true;

                        }
                    }
                    if (crossed == false) {
                        tmpPoint.addConnection(graph.get(j));
                    }


                }


            }
            startPoints.add(tmpPoint);
        }


        root = new TreeNode(startPoints, shadowGraph.getShadows());

        //Now lets start building from the root
        int depth = 0;
        int childrenSize;
        TreeNode tmpNode = root;
        ShadowGraph tmpGraph;
        int numberofbacktracks = 0;
        //while (numberofbacktracks == graph.size() * linestopasson.size()) {
        while (depth < graph.size()) {
            numberofbacktracks++;

            childrenSize = 1;
            for (WayPoint wayP : tmpNode.getWayPoints()) {
                childrenSize *= wayP.connected.size();
            }


            for (WayPoint wayP : tmpNode.getWayPoints()) {
                for (int i = 0; i < wayP.connected.size(); i++) {
                    ArrayList<Point2D> loc = new ArrayList<>();
                    loc.add(wayP.connected.get(i).getCoord());

                    ArrayList<WayPoint> wpp = new ArrayList<>();
                    wpp.add(wayP.connected.get(i));

                    tmpGraph = new ShadowGraph(map, loc);


                    tmpNode.addChild(new TreeNode(tmpNode, wpp, tmpGraph.getShadows()));
                }
            }

            TreeNode best = null;
            if (tmpNode.children != null) {
                for (int i = 0; i < tmpNode.children.size(); i++) {
                    if (i == 0) {

                        best = tmpNode.children.get(i);
                    } else if (best.score < tmpNode.children.get(i).score) {

                        best = tmpNode.children.get(i);
                    }else if (best.score== tmpNode.children.get(i).score){

                    }
                }
            }
            /*

            for(int j = 0; j < childrenSize; j++)   {
                try {
                    tmpGraph = new ShadowGraph(map, )
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("IndexOutOfBoundsException: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("Caught IOException: " + e.getMessage());
                }
            }
            */
            if (best != null && best.contShadows == null) {

                System.out.println("\nbest = " + best);
                int i = 0;
                while (best.parent != null) {
                    System.out.println("\nbest = " + best.parent + " the number it is is= " + i);
                    best = best.parent;
                }
                System.exit(824);
                // return the path to this point
            } else if (best != null) {
                tmpNode = best;
                System.out.println("this is the best one =" + best);

                depth++;
            } else {
                System.exit(666);
            }
            System.out.println("Current Depth => " + depth + "\t allowed => " + graph.size());
        }


        //for(WayPoint )


    }


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
        */

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

    public ArrayList<ArrayList<WayPoint>> getPermut(TreeNode node)   {
        int childrenSize = 1;
        for (WayPoint wayP : node.getWayPoints()) {
            childrenSize *= wayP.connected.size();
        }

        System.out.println("possible number of children");

        //Get first permutation
        int count = 0;
        int i = 0;
        TreeNode tmpNode;
        while(count < childrenSize)  {
            while(i <=3)    {

            }
        }
        return null;
    }

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
            System.out.println("NEW TREENODE => " + root.children.get(0));
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
                        graph.add(new WayPoint(a, b));
                        locationsOfPursuers.add(a);
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
                            graph.get(i).addConnection(graph.get(j));

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

    }

    public ArrayList<Point2D> getPoints() {
        ArrayList<Point2D> points = new ArrayList<>();
        for (WayPoint way : graph) {
            way.printWayPoint();
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

    public ArrayList<Line> getRays() {
        return raystopasson;
    }

    public ArrayList<Line> getConnections() {
        return connections;
    }


}
