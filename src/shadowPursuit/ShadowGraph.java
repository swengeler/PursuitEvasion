package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.*;

/**
 * Created by Robins on 30.04.2017.
 */
public class ShadowGraph {


    private ArrayList<ShadowNode> Nodes;
    private ArrayList<Line> polygonEdges;

    private Polygon environment;
    private ArrayList<Polygon> obstacles;
    private ArrayList<Polygon> allPolygons;

    private ArrayList<Point2D> agents;

    private ArrayList<Point2D> t1 = new ArrayList<>();
    private ArrayList<ShadowNode> checkT3 = new ArrayList<>();
    private ArrayList<ShadowNode> checkT4 = new ArrayList<>();
    private ArrayList<ArrayList<ShadowNode>> shadows = new ArrayList<>();

    private ArrayList<Point2D> pointy;

    public ShadowGraph(MapRepresentation map, ArrayList<WayPoint> points, boolean tr) {
        if (map == null) {
            System.exit(9999);
        }
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();

        allPolygons = map.getAllPolygons();
        agents = new ArrayList<>();

        for(WayPoint wayP : points) {
            agents.add(wayP.getCoord());
        }



        polygonEdges = map.getPolygonEdges();
        Nodes = new ArrayList<>();

        generateType1();
        calculateType3();
        addT3AndT4ToGraph();

        correctPath();
        printShadows();

    }


    public ShadowGraph(MapRepresentation map, ArrayList<Point2D> agents) {
        if (map == null) {
            System.exit(9999);
        }
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();

        allPolygons = map.getAllPolygons();
        this.agents = agents;

        polygonEdges = map.getPolygonEdges();
        Nodes = new ArrayList<>();

        generateType1();
        calculateType3();
        addT3AndT4ToGraph();
        System.out.println("------------------BEFORE--------------");
        printNodes();

        correctPath();
        printShadows();
    }

    //used for testpurpoes
    public ShadowGraph(Polygon environment, ArrayList<Polygon> Obstacles, ArrayList<Point2D> agents) {
        this.environment = environment;
        this.obstacles = Obstacles;

        allPolygons = new ArrayList<>();
        allPolygons.add(environment);
        allPolygons.addAll(Obstacles);
        polygonEdges = new ArrayList<>();

        Line newLine;
        for (Polygon p : allPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {

                newLine = new Line(p.getPoints().get(i), p.getPoints().get(i + 1), (p.getPoints().get((i + 2) % p.getPoints().size())), (p.getPoints().get((i + 3) % p.getPoints().size())));
                //System.out.println("new Line added = " + newLine);
                polygonEdges.add(newLine);
            }
        }

        for (Point2D point : polyToPoints(environment)) {
            System.out.println(point);
        }

        System.out.println("\n");
        this.agents = agents;

        Nodes = new ArrayList<>();


        //generateT1Connections();
        //circleDetect();
        //calcT2Points();
        //printGraph();

        generateType1();
        calculateType3();
        addT3AndT4ToGraph();
        System.out.println("------------------BEFORE--------------");
        printNodes();

        correctPath();
        printShadows();

    }


    //@TODO problem for objects that are entierly in shadow
    public void calcT2Points() {
        ArrayList<Point2D> temp, T2Points;
        T2Points = new ArrayList<>();
        Point2D tmpPoint, tmpPoint2;
        ShadowNode tmpNode, tmp2Node, tNode;
        pointy = findReflex(environment, allPolygons, obstacles);
        System.out.println("pointy points :D = " + pointy);


        for (int i = 0; i < Nodes.size(); i++) {
            tNode = Nodes.get(i);
            //System.out.println("For Node = " + tNode);


            temp = new ArrayList<>();
            if ((tNode.left == null || tNode.right == null) && tNode.getType() == 1) {
                temp = getAdjacentPoints(tNode.getPosition(), allPolygons);
                //System.out.println("For = " + tNode.getPosition() + "\tAdjacent: " + temp);
                if (tNode.right == null) {
                    //false
                    //System.out.println("tNode = " + tNode);
                    tmpPoint = tNode.left.getPosition();
                    if (temp.get(0) == tmpPoint) {
                        if (pointy.contains(temp.get(0))) {
                            Nodes.add(new ShadowNode(temp.get(0), tNode));
                        }
                    } else {
                        if (pointy.contains(temp.get(1))) {
                            Nodes.add(new ShadowNode(temp.get(1), tNode));
                        }
                    }
                } else if (tNode.left == null) {
                    //Correct
                    tmpPoint = tNode.right.getPosition();
                    if (temp.get(0) == tmpPoint) {
                        if (pointy.contains(temp.get(1))) {
                            Nodes.add(new ShadowNode(temp.get(1), tNode));
                        }
                    } else {
                        if (pointy.contains(temp.get(0))) {
                            Nodes.add(new ShadowNode(temp.get(0), tNode));
                        }
                    }


                    /*
                    if(temp.get(0) == tmpPoint && pointy.contains(temp.get(1))) {
                        Nodes.add(new ShadowNode(temp.get(1), tNode));
                    }
                    else if(temp.get(1) == tmpPoint && pointy.contains(temp.get(0)))
                        Nodes.add(new ShadowNode(temp.get(0), tNode));
                    }
                     */
                }

            }
            //System.out.println("i = " + i);
        }
    }

    public void generateType1() {

        t1 = getX1Points(environment, obstacles, agents);
        ArrayList<Point2D> pointy = findReflex(environment, allPolygons, obstacles);
        ArrayList<ShadowNode> temporary2 = new ArrayList<>();
        System.out.println("Pointy points = " + pointy);

        double agentX, agentY, pointX, pointY;

        ShadowNode tmp, tmp2, newNode, t2Shad, tmpObj;
        Point2D tmpPoint, tmpPoint2, shadPoint;

        ArrayList<Point2D> adj;

        System.out.println("\n");

        boolean failed = false;

        for (Point2D point : t1) {
            newNode = new ShadowNode(point);
            Nodes.add(newNode);
        }


        //Adding Type2s top Graph
        for (Point2D point : pointy) {
            if (nodePresent(point) == null) {
                Nodes.add(new ShadowNode(point, true));
            }
        }


        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpPoint = tmp.getPosition();
            adj = getAdjacentPoints(tmpPoint, allPolygons);

            if (tmp.getType() == 1) {
                //System.out.println("Connection of T1 => " + tmp);
                //Left
                if (nodePresent(adj.get(0)) != null && tmp.getLeft() == null) {
                    tmp.left = nodePresent(adj.get(0));
                    nodePresent(adj.get(0)).right = tmp;
                }

                //Right
                if (nodePresent(adj.get(1)) != null && tmp.getRight() == null) {
                    tmp.right = nodePresent(adj.get(1));
                    nodePresent(adj.get(1)).left = tmp;
                }
            }
        }

        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpPoint = tmp.getPosition();
            adj = getAdjacentPoints(tmpPoint, allPolygons);
            boolean left = false;

            if (tmp.getType() == 2) {
                //System.out.println("Connection of T2 => " + tmp);
                //Left
                tmpPoint2 = null;
                if (nodePresent(adj.get(0)) != null && nodePresent(adj.get(0)).getLeft() == null && nodePresent(adj.get(0)).getType() == 2) {
                    tmp.left = nodePresent(adj.get(0));
                    nodePresent(adj.get(0)).right = tmp;

                    left = true;

                    tmpPoint2 = nodePresent(adj.get(0)).getPosition();
                } else if (nodePresent(adj.get(1)) != null && nodePresent(adj.get(1)).getRight() == null && nodePresent(adj.get(1)).getType() == 2) {
                    tmp.right = nodePresent(adj.get(1));
                    nodePresent(adj.get(1)).left = tmp;

                    left = false;

                    tmpPoint2 = nodePresent(adj.get(1)).getPosition();
                }

                if (tmpPoint2 != null) {
                    for (Point2D agent : agents) {
                        agentX = agent.getX();
                        agentY = agent.getY();

                        if (isVisible(agentX, agentY, tmpPoint2.getX(), tmpPoint2.getY(), polygonEdges) && isVisible(agentX, agentY, tmpPoint.getX(), tmpPoint.getY(), polygonEdges)) {
                            if (left) {
                                tmp.left = null;
                                nodePresent(adj.get(0)).right = null;
                            } else {
                                tmp.right = null;
                                nodePresent(adj.get(1)).left = null;
                            }
                            break;
                        }
                    }
                }

            }
        }


    }


    public ShadowNode nodePresent(Point2D point) {
        if (point == null) {
            return null;
        }
        Point2D tmp;
        double x1, y1, x2, y2;
        for (ShadowNode node : Nodes) {
            tmp = node.getPosition();
            //System.out.println("NODE = " + tmp);
            //System.out.println("POINT = " + point);
            if (tmp.getX() == point.getX() && tmp.getY() == point.getY()) {
                return node;
            }

            if (node.getType() == 2 || node.getType() == 3 || node.getType() == 4) {
                x1 = Math.round(node.getPosition().getX());
                y1 = Math.round(node.getPosition().getY());

                x2 = Math.round(point.getX());
                y2 = Math.round(point.getY());

                if (x1 == x2 && y1 == y2) {
                    return node;
                }

            }
        }
        return null;
    }

    public ShadowNode nodePresent(Point2D point, ArrayList<ShadowNode> list) {
        Point2D tmp;
        double x1, y1, x2, y2;
        for (ShadowNode node : list) {
            tmp = node.getPosition();

            if (tmp.getX() == point.getX() && tmp.getY() == point.getY()) {
                return node;
            }

            if (node.getType() == 3 || node.getType() == 4) {
                x1 = Math.round(node.getPosition().getX());
                y1 = Math.round(node.getPosition().getY());

                x2 = Math.round(point.getX());
                y2 = Math.round(point.getY());

                if (x1 == x2 && y1 == y2) {
                    return node;
                }

            }
        }
        return null;
    }


    public void printNodes() {
        int cunt = 0;
        for (int i = 0; i < Nodes.size(); i++) {

            if (Nodes.get(i).numberOfLinks() == 2) {
                cunt++;
            }
        }
        System.out.println("\n------------Nodes are printed------------");
        System.out.println("Number of Nodes = " + Nodes.size() + "\nNumber of double = " + cunt);

        for (int i = 0; i < Nodes.size(); i++) {
            System.out.println(Nodes.get(i));
        }
    }

    public void printGraph() {
        ArrayList<ShadowNode> printed = new ArrayList<>();


        ShadowNode start, start2, tmp, tmp2;

        //for(int i = 0; i < copied.size())
        //ShadowNode temp = copied.get(0);
        double sX, sY, s2X, s2Y, tmpX, tmpY, tmp2X, tmp2Y;
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpX = tmp.getPosition().getX();
            tmpY = tmp.getPosition().getY();

            if (tmp.getType() != 1 && tmp.getType() != 2) {
                printed.add(tmp);
                System.out.println(tmp);
            }
        }

        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpX = tmp.getPosition().getX();
            tmpY = tmp.getPosition().getY();

            //System.out.println(tmp);


            if (printed.size() == 0 || !printed.contains(tmp)) {
                tmp2 = tmp;
                if (tmp.left != null) {
                    tmp2 = tmp.getLeft();
                    tmp2X = tmp2.getPosition().getX();
                    tmp2Y = tmp2.getPosition().getY();
                    while (tmp2.getLeft() != null && (tmp2X != tmpX && tmp2Y != tmpY)) {
                        tmp2 = tmp2.getLeft();
                        tmp2X = tmp2.getPosition().getX();
                        tmp2Y = tmp2.getPosition().getY();
                    }
                }

                //We are now at the start
                while (tmp2 != null && printed.size() < Nodes.size()) {
                    System.out.println(tmp2);
                    printed.add(tmp2);
                    tmp2 = tmp2.getRight();
                }
                System.out.println("\n");
            }
        }

        System.out.println("----------Print finished----------\n\n");


    }


    public boolean circleDetect() {
        ShadowNode start, tmp;
        int breakCounter;
        for (int i = 0; i < Nodes.size(); i++) {
            if ((Nodes.get(i).getType() == 1 || Nodes.get(i).getType() == 2 || Nodes.get(i).getType() == 4) && Nodes.get(i).inCircle == false) {
                breakCounter = 0;
                start = Nodes.get(i);
                if (start.right != null) {
                    tmp = start;
                    while (tmp.right != start && breakCounter < (Nodes.size() + 1)) {
                        tmp = tmp.right;
                        breakCounter++;
                    }
                    if (tmp.right == start) {
                        start.isInCircle();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void calculateType3() {
        ArrayList<Point2D> Type3 = new ArrayList<>();
        ArrayList<Point2D> tempList;
        Point2D tempPoint, tempPoint2;

        double agentX, agentY, pointX, pointY;
        ShadowNode tmp, tmp2;
        Line tmpLine, Ray;


        double maxYDist, maxXDist, maxY, maxX, minX, minY, rayLength;

        maxYDist = 0;
        maxXDist = 0;

        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;

        Line newL;

        Point2D tmpPoint;
        ArrayList<Point2D> allPoints = new ArrayList();
        allPoints.addAll(polyToPoints(allPolygons));

        //System.out.println("Before calculation: ");
        for (int i = 0; i < allPoints.size(); i++) {
            tmpPoint = allPoints.get(i);


            if (tmpPoint.getX() < minX) {
                minX = tmpPoint.getX();
                maxXDist = maxX - minX;
            }
            if (tmpPoint.getX() > maxX) {
                maxX = tmpPoint.getX();
                maxXDist = maxX - minX;
            }

            if (tmpPoint.getY() < minY) {
                minY = tmpPoint.getY();
                maxYDist = maxY - minY;
            }
            if (tmpPoint.getY() > maxY) {
                maxY = tmpPoint.getY();
                maxYDist = maxY - minY;
            }

        }
        System.out.println("checking maxy= " + maxYDist);
        System.out.println("checking maxx= " + maxXDist);
        if (maxYDist < maxXDist) {
            rayLength = maxXDist * 2;
        } else {
            rayLength = maxYDist * 2;
        }


        ArrayList<ShadowNode> tmpList = new ArrayList<>();
        ArrayList<Point2D> pointiPoints = findReflex(environment, allPolygons, obstacles);
        ArrayList<Point2D> adjPoints;
        ShadowNode tmpNode, tmpNode2;


        //Create T3 for each agent which can see a T2
        for (Point2D agent : agents) {
            agentX = agent.getX();
            agentY = agent.getY();
            for (int i = 0; i < Nodes.size(); i++) {

                if (Nodes.get(i).getType() == 2) {
                    tmp = Nodes.get(i);
                    tempPoint = tmp.getPosition();
                    System.out.println("\nChecking for Agent => " + agent + " and Type2 => " + tmp);
                    if (isVisible(tempPoint.getX(), tempPoint.getY(), agentX, agentY, polygonEdges)) {
                        System.out.println(tempPoint + " is visible for Agent => " + agent);
                        tmpLine = scaleRay(agent, tempPoint, rayLength);
                        Point2D intersect = getT3Intersect(tmpLine);

                        if (intersect == null) {
                            System.out.println("this lines agent = " + agent);
                            System.out.println("this lines tempPoint = " + tempPoint);
                            System.out.println("this lines rayLength= " + rayLength);

                            System.out.println("this line= " + tmpLine);

                            System.out.println("INTERSECT ERROR");
                            //printNodes();

                            //System.exit(123);
                            //Nodes.remove(i--);
                        } else {


                            tmp2 = new ShadowNode(intersect, tmp, tmpLine, agent);
                            Line b = new Line(intersect.getX(), intersect.getY(), tempPoint.getX(), tempPoint.getY());
                            Point2D a = getLineMiddle(b);

                            if (legalPosition(environment, obstacles, a.getX(), a.getY())) {
                                checkT3.add(tmp2);
                            }
                        }

                    }
                }
            }
        }



        System.out.println("----Checking T3----");
        for (ShadowNode t3 : checkT3) {
            System.out.println(t3);
        }
        System.out.println("-------------------");


        ArrayList<ShadowNode> replacedT3 = new ArrayList<>();

        int count = 0;

        for (int i = 0; i < checkT3.size(); i++) {

            ShadowNode current = checkT3.get(i);


            for (int j = 0; j < checkT3.size(); j++) {
                if (i != j) {
                    //System.out.println("j= " + j);
                    //System.out.println("i= " + i);
                    ShadowNode checkInt = checkT3.get(j);
                    if(checkInt.getConnectedType2() != null && current.getConnectedType2() != null) {
                        System.out.println("\nNode i => " + current + "\tNode j => " + checkInt);
                        System.out.println("Line1 => " + current.getRay() + "\t and Line2 => " + checkInt.getRay());


     /*               if ((checkT3.get(j).getRight() != null && checkT3.get(i).getLeft() != null) ||
                            (checkT3.get(i).getRight() != null && checkT3.get(j).getLeft() != null)
                                    && (checkT3.get(i).getRight() != checkT3.get(j).getLeft() || checkT3.get(j).getRight() != checkT3.get(i).getLeft()))
                        {

    */
                        System.out.println("current i type2= " + current.getConnectedType2());
                        System.out.println("current j type2= " + checkInt.getConnectedType2());

                        System.out.println("Number 1 => " + current);
                        System.out.println("Number 2 => " + checkInt);

                        Line checkingi = new Line(current.getConnectedType2().getPosition().getX(), current.getConnectedType2().getPosition().getY(), current.getPosition().getX(), current.getPosition().getY());
                        Line checkingj = new Line(checkInt.getConnectedType2().getPosition().getX(), checkInt.getConnectedType2().getPosition().getY(), checkInt.getPosition().getX(), checkInt.getPosition().getY());


                       // Line checkingi = new Line(current.getType2Creating().getPosition().getX(), current.getType2Creating().getPosition().getY(), current.getPosition().getX(), current.getPosition().getY());
                       // Line checkingj = new Line(checkInt.getType2Creating().getPosition().getX(), checkInt.getType2Creating().getPosition().getY(), checkInt.getPosition().getX(), checkInt.getPosition().getY());


                        //Line checkingi = new Line(current.getRay().getStartX(), current.getRay().getStartY(), current.getPosition().getX(), current.getPosition().getY());
                        //   Line checkingj = new Line(checkInt.getRay().getStartX(), checkInt.getRay().getStartY(), checkInt.getPosition().getX(), checkInt.getPosition().getY());


                        if (lineIntersect(checkingi, checkingj)) {

                            System.out.println("yes");
                            // fucking jontyif (nodePresent(FindIntersection(current.getRay(), checkInt.getRay())) == null) {
                            //    System.out.println("bbbbbbbbbbbbbbb");
                            //fucking jonty Point2D intersect = FindIntersection(current.getRay(), checkInt.getRay());
                            Point2D intersect = findIntersect2(current.getRay(), checkInt.getRay());

                            //TODO
                            //figure out which t2 and pursuer corresponds to each t3
                            ShadowNode correspondingT2left, correspondingT2Right;

                            Point2D agentLeft, agentRight;
                            printNodes();
                            // System.exit(240696);

                            if (checkT3.get(i).getLeft() != null && checkT3.get(j).getRight() != null && checkT3.get(i).getLeft().getType() == 2 && checkT3.get(j).getRight().getType() == 2) {
                                //  System.out.println("cccccccccccccccccccccccccc");
                                count++;
                                correspondingT2left = checkT3.get(i).getLeft();
                                correspondingT2Right = checkT3.get(j).getRight();

                                double correspondingAgentLeftX = checkT3.get(i).getRay().getStartX();
                                double correspondingAgentLeftY = checkT3.get(i).getRay().getStartY();
                                double correspondingAgentRightX = checkT3.get(j).getRay().getStartX();
                                double correspondingAgentRightY = checkT3.get(j).getRay().getStartY();

                                agentLeft = new Point2D(correspondingAgentLeftX, correspondingAgentLeftY);
                                agentRight = new Point2D(correspondingAgentRightX, correspondingAgentRightY);
                                ShadowNode posT4 = new ShadowNode(intersect, correspondingT2left, correspondingT2Right, agentLeft, agentRight);

                                checkT4.add(posT4);
                                //checkT4.get(checkT4.size() - 1).connectT2();

                            } else if (checkT3.get(i).getRight() != null && checkT3.get(j).getLeft() != null
                                    && checkT3.get(i).getRight().getType() == 2 && checkT3.get(j).getLeft().getType() == 2) {
                                count++;
                                correspondingT2Right = checkT3.get(i).getRight();
                                correspondingT2left = checkT3.get(j).getLeft();

                                double correspondingAgentLeftX = checkT3.get(j).getRay().getStartX();
                                double correspondingAgentLeftY = checkT3.get(j).getRay().getStartY();
                                double correspondingAgentRightX = checkT3.get(i).getRay().getStartX();
                                double correspondingAgentRightY = checkT3.get(i).getRay().getStartY();

                                agentLeft = new Point2D(correspondingAgentLeftX, correspondingAgentLeftY);
                                agentRight = new Point2D(correspondingAgentRightX, correspondingAgentRightY);
                                ShadowNode posT4 = new ShadowNode(intersect, correspondingT2left, correspondingT2Right, agentLeft, agentRight);

                                if (nodePresent(posT4.getPosition()) == null) {
                                    checkT4.add(posT4);
                                }

                            } else if (checkT3.get(i).getRight() != null && checkT3.get(j).getRight() != null
                                    && checkT3.get(i).getRight().getType() == 2 && checkT3.get(j).getRight().getType() == 2) {
                                correspondingT2Right = checkT3.get(i).getRight();
                                correspondingT2left = checkT3.get(j).getRight();

                                double correspondingAgentLeftX = checkT3.get(j).getRay().getStartX();
                                double correspondingAgentLeftY = checkT3.get(j).getRay().getStartY();
                                double correspondingAgentRightX = checkT3.get(i).getRay().getStartX();
                                double correspondingAgentRightY = checkT3.get(i).getRay().getStartY();

                                agentLeft = new Point2D(correspondingAgentLeftX, correspondingAgentLeftY);
                                agentRight = new Point2D(correspondingAgentRightX, correspondingAgentRightY);
                                ShadowNode posT4 = new ShadowNode(intersect, correspondingT2left, correspondingT2Right, agentLeft, agentRight);


                                if (nodePresent(posT4.getPosition()) == null) {
                                    checkT4.add(posT4);
                                }


                            } else if (checkT3.get(i).getLeft() != null && checkT3.get(j).getLeft() != null
                                    && checkT3.get(i).getLeft().getType() == 2 && checkT3.get(j).getLeft().getType() == 2) {

                                correspondingT2Right = checkT3.get(i).getLeft();
                                correspondingT2left = checkT3.get(j).getLeft();

                                double correspondingAgentLeftX = checkT3.get(j).getRay().getStartX();
                                double correspondingAgentLeftY = checkT3.get(j).getRay().getStartY();
                                double correspondingAgentRightX = checkT3.get(i).getRay().getStartX();
                                double correspondingAgentRightY = checkT3.get(i).getRay().getStartY();

                                agentLeft = new Point2D(correspondingAgentLeftX, correspondingAgentLeftY);
                                agentRight = new Point2D(correspondingAgentRightX, correspondingAgentRightY);
                                ShadowNode posT4 = new ShadowNode(intersect, correspondingT2left, correspondingT2Right, agentLeft, agentRight);


                                if (nodePresent(posT4.getPosition()) == null) {
                                    checkT4.add(posT4);
                                }


                            } else {
                                agentLeft = null;
                                agentRight = null;
                                correspondingT2left = null;
                                correspondingT2Right = null;

                                System.out.println("------PROBLEM ANALYSIS------\n'i' =>" + checkT3.get(i) + "\nLEFT = " + checkT3.get(i).getLeft() + "\nRIGHT = " + checkT3.get(i).getRight() + "\n\n'j' =>" + checkT3.get(j) + "\nLEFT = " + checkT3.get(j).getLeft() + "\nRIGHT = " + checkT3.get(j).getRight() + "\n------------------------\n");

                                //System.exit(6666);
                            }


                        }
                    }
                }
            }


            int seenby = 0;
            for (int q = 0; q < agents.size(); q++) {


                System.out.println("\n\nagent = " + agents.get(q));
                System.out.println("Checking possible T3 =  " + checkT3.get(i));
                System.out.println("checking for this agent= " + agents.get(q));
                System.out.println("Is visible = " + isVisible(agents.get(q).getX(), agents.get(q).getY(), checkT3.get(i).getPosition().getX(), checkT3.get(i).getPosition().getY(), polygonEdges));


                if (agents.get(q).getX() == checkT3.get(i).getLeftAgent().getX() && agents.get(q).getY() == checkT3.get(i).getLeftAgent().getY()) {
                    seenby++;
                    System.out.println("lookey here");
                }

                if (isVisible(agents.get(q).getX(), agents.get(q).getY(), checkT3.get(i).getPosition().getX(), checkT3.get(i).getPosition().getY(), polygonEdges)) {
                    if (agents.get(q).getX() == checkT3.get(i).getRay().getStartX() && agents.get(q).getY() == checkT3.get(i).getRay().getStartY()) {
                        System.out.println("muahahahahaha ");
                    } else if (agents.get(q).getX() == checkT3.get(i).getLeftAgent().getX() && agents.get(q).getY() == checkT3.get(i).getLeftAgent().getY()) {
                        System.out.println("muahahahahahahaha");
                    } else {
                        seenby++;
                    }
                }

            }
            if (seenby > 1) {
                System.out.println("seen by= " + seenby + " and it is this t3= " + checkT3.get(i) + " this guys corresponding agent is + " + checkT3.get(i).getLeftAgent());
                replacedT3.add(checkT3.get(i));
                //System.out.println("wooh one works");
            } else {
                System.out.println("seen by= " + seenby + " and it is this t3= " + checkT3.get(i) + " this guys corresponding agent is + " + checkT3.get(i).getLeftAgent());
            }

        }

        System.out.println("checkt4= " + checkT4.size());
        System.out.println("checkt3= " + checkT3.size());


        ShadowNode tmpLeft, tmpRight;

        ArrayList<ShadowNode> toRemove = t4overseen(checkT4);
        /*
        System.out.println(" ok here we go when it comes to adding t3 and t4");
        System.out.println("checkt3 size= " + checkT3.size() + " replacet3 size= " + replacedT3.size());
        System.out.println("-------------------------------------------------------");
        System.out.println("fuck it here is checkt3 = " + checkT3);
        System.out.println("-------------------------------------------------------");
        */
        //printGraph();


        System.out.println("-------Check T3 Analysis-------");
        for (ShadowNode t3 : checkT3) {
            System.out.println(t3);
        }
        System.out.println("-------------------------------\n");
        System.out.println("-------Check T4 Analysis-------");
        for (ShadowNode t4 : checkT4) {
            System.out.println(t4);
        }
        System.out.println("-------------------------------\n");

        printNodes();
        //System.exit(count);


        for (int i = 0; i < replacedT3.size(); i++) {
            //
            //System.out.println("testing jontys sanity test = 1" );
            //printGraph();
            checkT3.remove(replacedT3.get(i));
        }


        for (int i = 0; i < checkT3.size(); i++) {

            if (nodePresent(checkT3.get(i).getPosition()) == null) {
                System.out.println("adding t3= " + checkT3.get(i));

                Nodes.add(checkT3.get(i));
            }
        }


        for (int i = 0; i < toRemove.size(); i++) {
            //System.out.println("removing t4= " + checkT4.get(i));
            //System.out.println("Size before => " + checkT4.size());
            checkT4.remove(toRemove.get(i));
            //System.out.println("Size AFTER => " + checkT4.size());
            //checkT4.remove(toRemove.get(i));
        }


        for (int i = 0; i < checkT4.size(); i++) {
            if (nodePresent(checkT4.get(i).getPosition()) == null) {
                //System.out.println("adding t4= " + checkT4.get(i));
                //printGraph();


                checkT4.get(i).connectT2();
                /*
                System.out.println("-------Printing CheckT4 AFTER-------");
                for(int k = 0; k < checkT4.size(); k++) {
                    System.out.println(checkT4.get(k));
                }
                System.out.println("------------------------------------");
                System.out.println("--------Printing Nodes AFTER--------");
                for(int k = 0; k < Nodes.size(); k++) {
                    System.out.println(Nodes.get(k));
                }
                System.out.println("------------------------------------\n\n\n");
                */
                //System.out.println("ADDED!!!");
                //printGraph();
                Nodes.add(checkT4.get(i));
            }
        }

    }

    public void addT3AndT4ToGraph() {
        //System.out.println("----------------------------------------------");
        //printGraph();
        //System.out.println("----------------------------------------------");


        ShadowNode tmp, tmp2, tmp3, tmp4, tmp5;
        Point2D tmpPoint, tmpPoint2, tmpPoint3, tmpStart, tmpEnd;
        Line tmpLine, tmpLine2;
        double mionDist;

        pointy = findReflex(environment, allPolygons, obstacles);


        //Overwriting T3 rays that also have a Type4 on them
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 3) {
                //Check if there's another Type4 on the same ray
                for (int j = 0; j < Nodes.size(); j++) {
                    if (i != j) {
                        tmp2 = Nodes.get(j);
                        if (tmp2.getType() == 4) {
                            tmpPoint = tmp2.getPosition();
                            if (onLine(tmpPoint, tmp.getRay())) {
                                System.out.println("PROBLEM Type3=> " + tmp + "\nLEFT => " + tmp.getLeft() + "\nRIGHT => " + tmp.getRight());
                                System.out.println("PROBLEM Type4=> " + tmp2 + "\nLEFT => " + tmp2.getLeft() + "\nRIGHT => " + tmp2.getRight());

                                if (tmp.getLeft() != null && tmp.getLeft().getType() == 2 && tmp2.getRight() == null) {
                                    System.out.println("Enter 1");
                                    tmp.getLeft().right = null;
                                    tmp.left = tmp2;
                                    if (tmp2.getRight() == null) {
                                        System.exit(686);
                                    }
                                    System.out.println("PRINT => " + tmp2);
                                    tmp2.right = tmp;

                                } else if (tmp.getRight() != null && tmp.getRight().getType() == 2 && tmp2.getLeft() == null) {
                                    System.out.println("Enter 2");
                                    tmp.getRight().left = null;
                                    tmp.right = tmp2;
                                    if (tmp2.getLeft() == null) {
                                        System.exit(868);
                                    }
                                    tmp2.left = tmp;
                                }

                                System.out.println("AFTER T4 connect => " + tmp + "\nLEFT => " + tmp.getLeft() + "\nRIGHT => " + tmp.getRight() + "\n\n");
                            }
                        }

                    }
                }
            }
        }


        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 3 && (tmp.getLeft() == null || tmp.getRight() == null)) {
                for (ShadowNode t4 : Nodes) {
                    if (t4.getType() == 4 && onLine(t4.getPosition(), tmp.getRay())) {
                        //Now we look if there might be another Type3 on the same rtay that the Type4 is on
                        for (int j = 0; j < Nodes.size(); j++) {

                            tmp2 = Nodes.get(j);
                            if (i != j) {
                                if (tmp2.getType() == 3 && onLine(t4.getPosition(), tmp2.getRay()) && (tmp2.getConnectedType2() == t4.getLeft() || tmp2.getConnectedType2() == t4.getRight())) {
                                    System.out.println("NOW THIS IS WHERE THE MAGIC HAPPENS");


                                    tmp.overwriteConnectedType2(t4);
                                    tmp2.overwriteConnectedType2(t4);

                                    if (tmp.getRight() == t4 && t4.getRight() == tmp) {
                                        t4.right = t4.getLeft();
                                        t4.left = tmp;
                                    } else if (tmp.getLeft() == t4 && t4.getLeft() == tmp) {
                                        t4.left = t4.getRight();
                                        t4.right = tmp;
                                    }

                                    if (tmp2.getRight() == t4 && t4.getRight() == tmp2) {
                                        t4.right = t4.getLeft();
                                        t4.left = tmp2;
                                    } else if (tmp2.getLeft() == t4 && t4.getLeft() == tmp2) {
                                        t4.left = t4.getRight();
                                        t4.right = tmp2;
                                    }


                                    ShadowNode tempp = t4.getRight();
                                    t4.right = t4.getLeft();
                                    t4.left = tempp;


                                }

                            }
                        }
                    }
                }
            }
        }


        printNodes();
        ArrayList<ShadowNode> sameLine;
        //Connect Type3 to next Type3 or Type1
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 3) {
                tmpLine = getLineOn(tmp, polygonEdges);
                sameLine = new ArrayList<>();
                sameLine.add(tmp);
                for (int j = 0; j < Nodes.size(); j++) {
                    if (i != j && (Nodes.get(j).getType() == 3 || Nodes.get(j).getType() == 1)) {
                        tmp2 = Nodes.get(j);
                        tmpPoint = tmp2.getPosition();
                        if (onLine(tmpPoint, tmpLine)) {
                            sameLine.add(tmp2);
                        }
                    }
                }

                tmpStart = new Point2D(tmpLine.getStartX(), tmpLine.getStartY());

                /*

                System.out.println("RUN => " + i);
                printNodes();
                System.out.println("SIZE: " + sameLine.size());
                for(ShadowNode shad : sameLine) {
                    System.out.println(shad);
                }
                System.out.println("\n");
                */
                if (sameLine.size() > 2) {
                    ArrayList<ShadowNode> areOnLine = orderByClosestToPoint(sameLine, tmpStart);
                    System.out.println("Are on Line => " + areOnLine.size());
                    for (int k = 0; k < areOnLine.size() - 1; k += 2) {
                        System.out.println("K => " + k);
                        if (areOnLine.get(k).getType() == 1) {
                            //    && (areOnLine.get(k).getRight() == null || areOnLine.get(k).getLeft() == null)) {
                            ShadowNode a = areOnLine.get(k);
                            ShadowNode b = areOnLine.get(k + 1);


                            System.out.println("----------------fuck you-------------");
                            System.out.println(a);
                            System.out.println(b);
                            //System.exit(1111);

                            Line c = new Line(a.getPosition().getX(), a.getPosition().getY(), b.getPosition().getX(), b.getPosition().getY());
                            Point2D d = getLineMiddle(c);
                            //     Line d= scaleRay(new Point2D(a.getPosition().getX(),a.getPosition().getY()),new Point2D(b.getPosition().getX(),b.getPosition().getY()),0.5);

                            //  if (! isVisible2(d)) {
                            areOnLine.get(k).connect(areOnLine.get(k + 1));

                            //printNodes();
                            System.out.println("------------------look-----------------");
                            System.out.println("k=" + k + " k+1=" + (k + 1));
                            System.out.println(areOnLine.get(k));
                            System.out.println(areOnLine.get(k + 1));
                            System.out.print(tmpLine);
                            //System.exit(2444);

                            // }
                            ///  areOnLine.get(k).connect(areOnLine.get(k+1));
                            // k++;
                        } else if (areOnLine.get(k).getType() == 3 && (areOnLine.get(k).getRight() == null || areOnLine.get(k).getLeft() == null)) {
                            ShadowNode a = areOnLine.get(k);
                            ShadowNode b = areOnLine.get(k + 1);
                            Line c = new Line(a.getPosition().getX(), a.getPosition().getY(), b.getPosition().getX(), b.getPosition().getY());
                            Point2D d = getLineMiddle(c);
                            //     Line d= scaleRay(new Point2D(a.getPosition().getX(),a.getPosition().getY()),new Point2D(b.getPosition().getX(),b.getPosition().getY()),0.5);
                            //   if (!isVisible2(d)){
                            System.out.println("------------------look-----------------");
                            System.out.println("k=" + k + " k+1=" + (k + 1));
                            System.out.println(areOnLine.get(k));
                            System.out.println(areOnLine.get(k + 1));


                            areOnLine.get(k).connect(areOnLine.get(k + 1));

                            // printNodes();
                            System.out.print(tmpLine);
                            //System.exit(2444);
                            //}

                        }
                    }
                } else if (sameLine.size() == 2) {
                    ArrayList<ShadowNode> areOnLine = orderByClosestToPoint(sameLine, tmpStart);
                    System.out.println("Are on Line => " + areOnLine.size());
                    System.out.println("Entry 0 => " + areOnLine.get(0));
                    System.out.println("Entry 1 => " + areOnLine.get(1));

                    if ((areOnLine.get(0).getType() == 3 && (areOnLine.get(0).getRight() == null || areOnLine.get(0).getLeft() == null)) && (areOnLine.get(1).getType() == 3 && (areOnLine.get(1).getRight() == null || areOnLine.get(1).getLeft() == null))) {
                        areOnLine.get(0).connect(areOnLine.get(1));
                    } else if ((areOnLine.get(0).getType() == 3 && areOnLine.get(1).getType() == 1) || (areOnLine.get(0).getType() == 1 && areOnLine.get(1).getType() == 3)) {
                        if(areOnLine.get(1).getPosition().getX() == 949.0 ||  areOnLine.get(0).getPosition().getX() == 949.0) {
                            System.out.println("I AM HERE!!!!!\n");
                            System.out.println("FIRST => " + areOnLine.get(0));
                            System.out.println("SECOND => " + areOnLine.get(1));
                        }
                        //Disconnect the Nodes from other connections they had prior on the line we want to connect them on now
                        if(areOnLine.get(1).numberOfLinks() == 2)   {
                            if(getLineOn(areOnLine.get(1).getLeft().getPosition(), polygonEdges) == tmpLine)    {
                                areOnLine.get(1).getLeft().right = null;
                                areOnLine.get(1).left = null;
                            }
                            else if(getLineOn(areOnLine.get(1).getRight().getPosition(), polygonEdges) == tmpLine)  {
                                areOnLine.get(1).getRight().left = null;
                                areOnLine.get(1).right = null;
                            }
                        }

                        if(areOnLine.get(0).numberOfLinks() == 2)   {
                            if(getLineOn(areOnLine.get(0).getLeft().getPosition(), polygonEdges) == tmpLine)    {
                                areOnLine.get(0).getLeft().right = null;
                                areOnLine.get(0).left = null;
                            }
                            else if(getLineOn(areOnLine.get(0).getRight().getPosition(), polygonEdges) == tmpLine)  {
                                areOnLine.get(0).getRight().left = null;
                                areOnLine.get(0).right = null;
                            }
                        }

                        areOnLine.get(0).connect(areOnLine.get(1));
                    }
                }

            }


            for(ShadowNode shad: Nodes) {
                if(shad.getType() == 3 && shad.getConnectedType2() == null) {
                    Line ray = shad.getRay();
                    Point2D start = new Point2D(ray.getStartX(), ray.getStartY());
                    if(nodePresent(start) != null)  {
                        System.out.println("Found=> " + nodePresent(start));
                        shad.correctT3(nodePresent(start));
                    }
                    else {
                        System.out.println("Issue found for => " + shad);
                        System.exit(651);
                    }
                }
            }


            for (ShadowNode node : Nodes) {
                if (node.getType() == 3) {
                    node.connectT2();
                }
            }







            /*
            System.out.println("--------------------------------------------------------------");
            for(ShadowNode shad : Nodes)    {
                if(shad.getType() == 3 || (shad.getRight() != null && shad.getRight().getType() == 3) || (shad.getLeft() != null && shad.getLeft().getType() == 3))  {
                    System.out.println(shad);
                }

            }
            System.out.println();
            */

        }


    }


    public boolean isVisible2(Point2D point) {
        boolean visble = false;
        int count = 0;
        for (Point2D agent : agents) {
            for (Line line : polygonEdges) {
                if (line != getLineOn(point, polygonEdges)) {
                    if (lineIntersect(line, agent.getX(), agent.getY(), point.getX(), point.getY())) {
                        count++;
                    }
                }
            }
            if (count == 0) {
                return true;
            } else {
                count = 0;
            }

        }
        return false;
    }


    public ShadowNode getClosestT1(ShadowNode node) {
        return getClosestT1(node.getPosition());
    }


    public ShadowNode getClosestT1(Point2D point) {
        Line line = getLineOn(point, polygonEdges);
        Point2D start = new Point2D(line.getStartX(), line.getStartY());
        Point2D end = new Point2D(line.getEndX(), line.getEndY());

        if (nodePresent(start) != null && nodePresent(end) != null) {
            System.exit(90909);
        } else if (nodePresent(start) != null) {
            return nodePresent(start);
        } else if (nodePresent(end) != null) {
            return nodePresent(end);
        } else {
            System.exit(909);
        }

        return null;
    }


    public ArrayList<ShadowNode> getNodesWithCommonCon(ShadowNode leftOrRight) {
        ArrayList<ShadowNode> retList = new ArrayList<>();
        for (ShadowNode node : Nodes) {
            if (node.getLeft() == leftOrRight || node.getRight() == leftOrRight) {
                retList.add(node);
            }
        }
        return retList;
    }

    public ShadowNode getClosestOnLine(Point2D point, Line onLine) {
        ArrayList<ShadowNode> returnList = new ArrayList<>();

        for (ShadowNode Node : Nodes) {
            if (Node.getType() == 3) {
                if (onLine(Node.getPosition(), onLine)) {
                    returnList.add(Node);
                }
            }
        }

        if (returnList.size() == 0) {
            System.exit(3456);
        }

        double minDist = Double.MAX_VALUE;
        ShadowNode toReturn = returnList.get(0);

        for (ShadowNode Node : returnList) {
            if (distance(point, Node.getPosition()) < minDist) {
                minDist = distance(point, Node.getPosition());
                toReturn = Node;
            }
        }

        return toReturn;
    }


    public ArrayList<ShadowNode> t4overseen(ArrayList<ShadowNode> allPosT4) {
        // take the possible t4, create a shape that has it and its two corresponding t2. check if any type 4s are contained in this shape. if yes then remove from list.
        ArrayList<ShadowNode> toRemove = new ArrayList<>();
        ArrayList<ShadowNode> pointsInShadow = new ArrayList<>();
        Line ray1, ray2, ray3, ray4;
        ShadowNode current;
        Point2D currentPoint;

        for (int i = 0; i < allPosT4.size(); i++) {
            current = allPosT4.get(i);
            currentPoint = allPosT4.get(i).getPosition();
            double l1, l2, r1, r2;
           /* l1 = 622;
            l2 = 238;
            r1 = 359;
            r2 = 232;
            Polygon polygon = new Polygon(currentPoint.getX(), currentPoint.getY(), l1, l2, r1, r2);
            */
            Polygon polygon = new Polygon(currentPoint.getX(),
                    currentPoint.getY(),
                    current.getLeft().getPosition().getX(),
                    current.getLeft().getPosition().getY(),
                    current.getRight().getPosition().getX(),
                    current.getRight().getPosition().getY());

            for (int j = 0; j < allPosT4.size(); j++) {
                if (j != i) {
                    if (polygon.contains(allPosT4.get(j).getPosition()) && (
                            (
                                    allPosT4.get(j).getLeft() == allPosT4.get(i).getRight() &&
                                            allPosT4.get(j).getRight() == allPosT4.get(i).getLeft()) ||
                                    (allPosT4.get(i).getLeft() == allPosT4.get(j).getLeft() &&
                                            allPosT4.get(j).getRight() == allPosT4.get(i).getRight()))) {

                        /*
                        System.out.println();
                        System.out.println();
                        System.out.println();
                        System.out.println();

                        System.out.println("checking the two type 4's = " + allPosT4.get(i));
                        System.out.println("checking the two type 4's = " + allPosT4.get(j));
                        System.out.println();
                        System.out.println();
                        System.out.println();
                        System.out.println();
                        */
                        System.out.println("fuck you11");
                        if (nodePresent(allPosT4.get(i).getPosition(), toRemove) == null) {
                            toRemove.add(allPosT4.get(i));
                        }

                    } else if (polygon.contains(allPosT4.get(j).getPosition())) {
                        System.out.println("this is two type 4s creating a shadow inside them");
                        pointsInShadow.add(allPosT4.get(i));
                        pointsInShadow.add(allPosT4.get(j));
                        ShadowNode rightT2I = allPosT4.get(i).right;
                        ShadowNode leftT2I = allPosT4.get(i).left;
                        Point2D leftAgentI = allPosT4.get(i).getLeftAgent();
                        Point2D rightAgentI = allPosT4.get(i).getRightAgent();

                        ShadowNode rightT2J = allPosT4.get(j).right;
                        ShadowNode leftT2J = allPosT4.get(j).left;
                        Point2D leftAgentJ = allPosT4.get(j).getLeftAgent();
                        Point2D rightAgentJ = allPosT4.get(j).getRightAgent();

                        ray1 = new Line(rightT2I.getPosition().getX(), rightT2I.getPosition().getY(), rightAgentI.getX(), rightAgentI.getY());
                        ray2 = new Line(leftT2I.getPosition().getX(), leftT2I.getPosition().getY(), leftAgentI.getX(), leftAgentI.getY());

                        ray3 = new Line(rightT2J.getPosition().getX(), rightT2J.getPosition().getY(), rightAgentJ.getX(), rightAgentJ.getY());
                        ray4 = new Line(leftT2J.getPosition().getX(), leftT2J.getPosition().getY(), leftAgentJ.getX(), leftAgentJ.getY());

                        if (lineIntersect(ray1, ray3)) {
                            //fucking jonty pointsInShadow.add(new ShadowNode(FindIntersection(ray1, ray3)));
                            pointsInShadow.add(new ShadowNode(findIntersect2(ray1, ray3)));
                        }
                        if (lineIntersect(ray1, ray4)) {
                            //fucking jonty pointsInShadow.add(new ShadowNode(FindIntersection(ray1, ray4)));
                            pointsInShadow.add(new ShadowNode(findIntersect2(ray1, ray3)));
                        }

                        if (lineIntersect(ray2, ray3)) {
                            //fucking jonty pointsInShadow.add(new ShadowNode(FindIntersection(ray2, ray3)));
                            pointsInShadow.add(new ShadowNode(findIntersect2(ray2, ray3)));
                        }
                        if (lineIntersect(ray2, ray4)) {
                            //fucking jonty  pointsInShadow.add(new ShadowNode(FindIntersection(ray2, ray4)));
                            pointsInShadow.add(new ShadowNode(findIntersect2(ray2, ray4)));
                        }
                        shadows.add(pointsInShadow);


                    } else {
                        Line a = new Line(allPosT4.get(j).getConnectedType2().getPosition().getX(), allPosT4.get(j).getConnectedType2().getPosition().getY(), allPosT4.get(j).getPosition().getX(), allPosT4.get(j).getPosition().getY());
                        System.out.println("a= " + a);
                        //System.exit(11111);
                        //System.out.println("fuck you jonty");
                    }
                }
            }

        }
        //System.out.println("to remove = " + toRemove);
        return toRemove;
    }


    public void getType2() {
        ArrayList<ShadowNode> t2 = new ArrayList<>();

        for (ShadowNode node : Nodes) {
            if (node.getType() == 2) {
                t2.add(node);
            }
        }

        for (ShadowNode node : t2) {
            System.out.println(node);
        }
    }


    public Point2D getT3Intersect(Line ray) {
        //System.out.println("Passed ray = " + ray);

        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for (Line inLine : polygonEdges) {
            if (lineIntersect(inLine, ray)) {
                //System.out.println("INTERSECT DETECTED at = " + findIntersect2(inLine, ray));

                if (nodePresent(findIntersect2(inLine, ray)) == null) {
                    intersectPoints.add(findIntersect2(inLine, ray));
                } else {

                }
                //System.out.println("AT = " + intersectPoints.get(intersectPoints.size()-1) + "\tWITH = " + inLine + "\n");
            }

        }

        int count= 0;

        double min = Double.MAX_VALUE;
        for (int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            //TODO @ROBIN IF STUFF FUCKS UP CHECK THIS CONDITION AGAIN
            if(tmpPoint != null) {
                System.out.println("ray x= " + ray.getStartX() + " y= " + ray.getStartY());
                System.out.println("tmp x= " + tmpPoint.getX() + " y= " + tmpPoint.getY());
                tmpLine = new Line(ray.getStartX(), ray.getStartY(), tmpPoint.getX(), tmpPoint.getY());
                dist = Math.sqrt(Math.pow((tmpLine.getEndX() - tmpLine.getStartX()), 2) + Math.pow((tmpLine.getEndY() - tmpLine.getStartY()), 2));

                if (dist < min) {
                    minPos = i;
                    min = dist;
                }
            }
        }

        if (intersectPoints.size() > 0) {
            return intersectPoints.get(minPos);
        } else {
            //System.out.println("hello");
            return null;
        }
    }


    public void clearingUpConnections(ArrayList<ShadowNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (i != j) {
                    if (nodes.get(j).getRight() == nodes.get(i).getRight()) {
                        nodes.get(j).setLeft(nodes.get(i));
                        nodes.get(i).setRight(nodes.get(j));

                    }
                    if (nodes.get(j).getLeft() == nodes.get(i).getLeft()) {
                        nodes.get(j).setRight(nodes.get(i));
                        nodes.get(i).setLeft(nodes.get(j));
                    }
                }
            }

        }

    }

    public void printShadows() {
        //First find cycle
        ArrayList<ShadowNode> doublyConnested = new ArrayList<>();
        ShadowNode tmp, start, next, prev;

        for (int i = 0; i < Nodes.size(); i++) {
            if (Nodes.get(i).numberOfLinks() == 2) {
                doublyConnested.add(Nodes.get(i));
            }
        }
        ArrayList<ShadowNode> visited = new ArrayList<>();
        ArrayList<ArrayList<ShadowNode>> shadows = new ArrayList<>();
        ArrayList<ShadowNode> shadow;
        int counter = 0;

        while (visited != doublyConnested && visited.size() < doublyConnested.size()) {
            for (int i = 0; i < Nodes.size(); i++) {
                tmp = Nodes.get(i);
                if (tmp.numberOfLinks() == 2 && !visited.contains(tmp)) {
                    start = tmp;
                    shadow = new ArrayList<>();
                    visited.add(start);
                    shadow.add(start);


                    prev = start;

                    next = start.getRight();


                    int c = 0;

                    //while(next.getPosition() != start.getPosition() )    {
                    boolean right = true;
                    while (!visited.contains(next)) {

                        System.out.println(c);
                        visited.add(next);
                        shadow.add(next);
                        if (next.getRight() != prev && next.getRight().numberOfLinks() == 2) {
                            next = next.getRight();
                            prev = next.getLeft();
                        } else if (next.getLeft() != prev && next.getLeft().numberOfLinks() == 2) {
                            next = next.getLeft();
                            prev = next.getRight();
                        }
                        c++;

                    }
                    shadows.add(shadow);

                }
                counter++;
                if (visited == doublyConnested || visited.size() >= doublyConnested.size()) {
                    break;
                }
            }
        }

        for (int i = 0; i < shadows.size(); i++) {
            System.out.println("------------------------new shadow starting now ------------------------");
            for (int j = 0; j < shadows.get(i).size(); j++) {
                System.out.println("Shadow node= " + shadows.get(i).get(j));
            }
            System.out.println("------------------------------------------------");

        }

        for (int i = 0; i < Nodes.size(); i++) {
            if (Nodes.get(i).numberOfLinks() < 2) {
                Nodes.remove(i--);
            }
        }


    }

    public ArrayList<Polygon> getShadows() {
        //First find cycle
        ArrayList<ShadowNode> doublyConnested = new ArrayList<>();
        ShadowNode tmp, start, next, prev;

        for (int i = 0; i < Nodes.size(); i++) {
            if (Nodes.get(i).numberOfLinks() == 2) {
                doublyConnested.add(Nodes.get(i));
            }
        }
        ArrayList<ShadowNode> visited = new ArrayList<>();
        ArrayList<ArrayList<ShadowNode>> shadows = new ArrayList<>();
        ArrayList<ShadowNode> shadow;
        int counter = 0;

        while (visited != doublyConnested && visited.size() < doublyConnested.size()) {
            for (int i = 0; i < Nodes.size(); i++) {
                tmp = Nodes.get(i);
                if (tmp.numberOfLinks() == 2 && !visited.contains(tmp)) {
                    start = tmp;
                    shadow = new ArrayList<>();
                    visited.add(start);
                    shadow.add(start);


                    prev = start;
                    next = start.getRight();
                    int c = 0;
                    //while(next.getPosition() != start.getPosition() )    {
                    while (!visited.contains(next)) {
                        System.out.println(c);
                        visited.add(next);
                        shadow.add(next);
                        if (next.getRight() != prev && next.getRight().numberOfLinks() == 2) {
                            next = next.getRight();
                            prev = next.getLeft();
                        } else if (next.getLeft() != prev && next.getLeft().numberOfLinks() == 2) {
                            next = next.getLeft();
                            prev = next.getRight();
                            //prev = next.getRight();
                        }
                        c++;
                    }
                    shadows.add(shadow);

                }
                counter++;
                if (visited == doublyConnested || visited.size() >= doublyConnested.size()) {
                    break;
                }
            }
        }


        ArrayList<Polygon> allShadows = new ArrayList<>();
        double[] list;

        for (int i = 0; i < shadows.size(); i++) {
            list = new double[2 * shadows.get(i).size()];

            System.out.println("Number of enries = " + shadows.get(i).size());
            for (int j = 0; j < shadows.get(i).size(); j++) {
                list[2 * j] = shadows.get(i).get(j).getPosition().getX();
                list[(2 * j) + 1] = shadows.get(i).get(j).getPosition().getY();
            }
            Polygon newPoly = new Polygon(list);
            allShadows.add(newPoly);
        }
        return allShadows;
    }

    public void correctPath() {
        ShadowNode shad, start, tmpNode, prev, next;
        ArrayList<ShadowNode> visited = new ArrayList<>();

        for (int i = 0; i < Nodes.size(); i++) {
            shad = Nodes.get(i);
            if (shad.numberOfLinks() == 2 && !visited.contains(shad)) {
                visited.add(shad);
                start = shad;
                next = shad.getRight();

                while (next.getRight() != start && !visited.contains(next) && next.numberOfLinks() == 2) {
                    visited.add(next);
                    prev = next;
                    next = next.getRight();
                    if (next.getRight() == prev) {
                        tmpNode = next.getLeft();
                        next.left = prev;
                        next.right = tmpNode;
                    }
                }
                if (next.numberOfLinks() != 2) {
                    break;
                }

            }
        }
    }

    public void cleanUp() {
        ShadowNode tmpNode;
        for (int i = 0; i < Nodes.size(); i++) {
            tmpNode = Nodes.get(i);

            if (tmpNode.getRight() == null || tmpNode.getLeft() == null) {
                Nodes.remove(i--);
            } else {
                if (tmpNode.getRight().getLeft() != tmpNode && tmpNode.getLeft().getRight() != tmpNode) {
                    Nodes.remove(i--);
                }
            }
        }
    }
}