package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.MapRepresentation;

import java.util.ArrayList;

import static additionalOperations.GeometryOperations.*;
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


    public ShadowGraph(MapRepresentation map, ArrayList<Point2D> agents) {
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();

        allPolygons = map.getAllPolygons();
        this.agents = agents;

        generateType1();
        //generateT1Connections();
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


        generateType1();


        //generateT1Connections();
        //circleDetect();
        //calcT2Points();
        //printGraph();

        calculateType3();
        //printGraph();
        //printGraph();
        printGraph();

    }


    //@TODO problem for objects that are entierly in shadow
    public void calcT2Points() {
        ArrayList<Point2D> temp, T2Points;
        T2Points = new ArrayList<>();
        Point2D tmpPoint;
        ShadowNode tmpNode, tmp2Node, tNode;
        ArrayList<Point2D> pointy = findReflex(environment, allPolygons, obstacles);
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


        ShadowNode tmp, tmp2, newNode, t2Shad;
        Point2D tmpPoint, tmpPoint2, shadPoint;

        ArrayList<Point2D> adj;

        System.out.println("\n");

        boolean failed = false;

        for (Point2D point : t1) {
            newNode = new ShadowNode(point);
            Nodes.add(newNode);
        }

        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpPoint = tmp.getPosition();
            if (tmp.getType() == 1) {
                adj = getAdjacentPoints(tmpPoint, allPolygons);

                /*
                if((tmpPoint.getX() == 450 && tmpPoint.getY() == 632) || (tmpPoint.getX() == 177 && tmpPoint.getY() == 619))    {
                    System.out.println("For: " + tmpPoint + "\tAdjacent: " + adj);
                }
                */


                //Left side
                if (t1.contains(adj.get(0))) {
                    tmpPoint2 = adj.get(0);
                    for (int j = 0; j < Nodes.size(); j++) {
                        tmp2 = Nodes.get(j);
                        if (tmp2.getType() == 1) {
                            shadPoint = tmp2.getPosition();
                            if (tmpPoint2.getX() == shadPoint.getX() && tmpPoint2.getY() == shadPoint.getY()) {
                                tmp.setLeft(tmp2);
                            }
                        }
                    }
                } else {
                    if (pointy.contains(adj.get(0)) && tmp.left == null) {
                        if (nodePresent(adj.get(0)) == null) {
                            t2Shad = new ShadowNode(adj.get(0), true);
                            tmp.setLeft(t2Shad);
                            temporary2.add(t2Shad);
                        } else {
                            t2Shad = nodePresent(adj.get(0));
                            tmp.setLeft(t2Shad);
                        }
                    }
                }


                //Right side
                if (t1.contains(adj.get(1))) {
                    tmpPoint2 = adj.get(1);
                    for (int j = 0; j < Nodes.size(); j++) {
                        tmp2 = Nodes.get(j);
                        shadPoint = tmp2.getPosition();
                        if (tmpPoint2.getX() == shadPoint.getX() && tmpPoint2.getY() == shadPoint.getY()) {
                            tmp.setRight(tmp2);
                        }
                    }
                } else {
                    if (pointy.contains(adj.get(1)) && tmp.right == null) {
                        if (nodePresent(adj.get(1)) == null) {
                            t2Shad = new ShadowNode(adj.get(1), true);
                            tmp.setRight(t2Shad);
                            temporary2.add(t2Shad);
                        } else {
                            t2Shad = nodePresent(adj.get(1));
                            tmp.setRight(t2Shad);
                        }
                    }
                }
            }


        }

        for (int i = 0; i < temporary2.size(); i++) {
            if (nodePresent(temporary2.get(i).getPosition()) == null) {
                Nodes.add(temporary2.get(i));
            }
        }
    }


    public ShadowNode nodePresent(Point2D point) {
        Point2D tmp;
        double x1, y1, x2, y2;
        for (ShadowNode node : Nodes) {
            tmp = node.getPosition();

            if (tmp.getX() == point.getX() && tmp.getY() == point.getY()) {
                return node;
            }

            if (node.getType() == 2 || node.getType() == 3 || node.getType() == 4) {
                x1 = Math.round(node.getPosition().getX());
                y1 = Math.round(node.getPosition().getY());

                x2 = Math.round(point.getX());
                y2 = Math.round(point.getY());

                if (x1 == x2 && y1 == y2)
                    return node;

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

                if (x1 == x2 && y1 == y2)
                    return node;

            }
        }
        return null;
    }


    public void printGraph() {
        ArrayList<ShadowNode> printed = new ArrayList<>();

        System.out.println("\n----------Graph is printed----------");
        System.out.println("Number of Nodes = " + Nodes.size());

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


    public void circleDetect() {
        ArrayList<ShadowNode> printed = new ArrayList<>();


        ShadowNode start, start2, tmp;
        //for(int i = 0; i < copied.size())
        //ShadowNode temp = copied.get(0);
        double sX, sY, s2X, s2Y, tmpX, tmpY;


        for (int i = 0; i < Nodes.size(); i++) {
            start = Nodes.get(i);
            sX = start.getPosition().getX();
            sY = start.getPosition().getY();

            //System.out.println("Start Node = " + start);

            /*for(int j = 0; j < printed.size(); j++) {
                System.out.println(printed.get(j));
            }
            System.out.println("i = " + i + "\tPrinted countains " + start + " = " + printed.contains(start));
            if(sX == 766 && sY == 213)  {
                System.out.println("Prev = " + start.left + "\tNext = " + start.right);
                System.out.println("Link left = " + start.left.right);
            }*/

            if (printed.size() == 0 || !printed.contains(start)) {

                if (start.right != null || start.left != null) {
                    //get to start
                    tmp = start.left;
                    boolean cycle = false;

                    if (tmp != null) {
                        tmpX = tmp.getPosition().getX();
                        tmpY = tmp.getPosition().getY();
                        while (tmp.left != null) {
                            if (tmpX == sX && tmpY == sY) {
                                cycle = true;
                                break;
                            }
                            tmp = tmp.left;
                            tmpX = tmp.getPosition().getX();
                            tmpY = tmp.getPosition().getY();
                        }
                    } else {
                        tmp = start;
                        tmpX = tmp.getPosition().getX();
                        tmpY = tmp.getPosition().getY();
                    }

                    //At this point we assume either we are in a cycle or we are at the beginning
                    int j = 1;
                    start2 = tmp;

                    s2X = start2.getPosition().getX();
                    s2Y = start2.getPosition().getY();

                    printed.add(start2);

                    tmp = start2.right;
                    tmpX = tmp.getPosition().getX();
                    tmpY = tmp.getPosition().getY();


                    while ((tmpX != s2X || tmpY != s2Y) && tmp != null) {

                        printed.add(tmp);
                        if (cycle && printed.get(printed.size() - 1).right == null) {
                            start2.left.setRight(start2);
                        }
                        //System.out.println(tmp);
                        tmp = tmp.right;
                        if (tmp != null) {
                            tmpX = tmp.getPosition().getX();
                            tmpY = tmp.getPosition().getY();
                        }
                    }


                    if (tmpX == s2X && tmpY == s2Y) {
                        //System.out.println(tmp);
                        printed.add(tmp);
                    }


                } else {
                    System.out.println(start);
                    printed.add(start);
                    System.out.println("\n");
                }
            }
        }
    }

    //TODO @Rob - keep working here
    public void calculateType3() {
        ArrayList<Point2D> Type3 = new ArrayList<>();
        ArrayList<Point2D> tempList;
        Point2D tempPoint;

        double agentX, agentY, pointX, pointY;
        shadowPursuit.ShadowNode tmp;
        Line tmpLine, Ray;


        double maxYDist, maxXDist, maxY, maxX, minX, minY, rayLength;

        maxYDist = 0;
        maxXDist = 0;

        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;

        Line newL;


        //System.out.println("Before calculation: ");
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 2) {
                //System.out.println(tmp.getPosition());
                newL = new Line(tmp.getPosition().getX(), tmp.getPosition().getY(), agents.get(0).getX(), agents.get(0).getY());
                //System.out.println("Visible? => " + isVisible(tmp.getPosition().getX(), tmp.getPosition().getY(), agents.get(0).getX(), agents.get(0).getY(),polygonEdges));
            }

            if (tmp.getPosition().getX() < minX) {
                minX = tmp.getPosition().getX();
                maxXDist = maxX - minX;
            }
            if (tmp.getPosition().getX() > maxX) {
                maxX = tmp.getPosition().getX();
                maxXDist = maxX - minX;
            }

            if (tmp.getPosition().getY() < minY) {
                minY = tmp.getPosition().getY();
                maxYDist = maxY - minY;
            }
            if (tmp.getPosition().getY() > maxY) {
                minY = tmp.getPosition().getY();
                maxYDist = maxY - minY;
            }

        }

        if (maxYDist < maxXDist) {
            rayLength = maxXDist * 2;
        } else {
            rayLength = maxYDist * 2;
        }


        for (Point2D agent : agents) {

            agentX = agent.getX();
            agentY = agent.getY();
            //For every point of Type2
            for (int i = 0; i < Nodes.size(); i++) {
                tmp = Nodes.get(i);
                if (tmp.getType() == 2) {
                    //System.out.println("TYPE 2 ENTERED FOR = " + tmp.getPosition());

                    pointX = tmp.getPosition().getX();
                    pointY = tmp.getPosition().getY();

                    if (isVisible(agentX, agentY, pointX, pointY, polygonEdges)) {
                        //Create occlusion Ray
                        System.out.println("---------------------------------------------------");
                        System.out.println("For Agent = " + agent + " and Point = " + tmp.getPosition());
                        Ray = scaleRay(agent, tmp, rayLength);
                        System.out.println("RAY = " + Ray);

                        Line original = new Line(agentX, agentY, tmp.getPosition().getX(), tmp.getPosition().getY());
                        //System.out.println("Gradient1 = " + gradient(original) + "\tGradient2 = " + gradient(Ray));

                        Point2D posT3 = getT3Intersect(Ray);
                        System.out.println("INTERSECT DETECTED");
                        System.out.println("AT = " + posT3);

                        System.out.println("");
                        System.out.println("---------------------------------------------------");
                        if (posT3 != null) {
                            double midx = (posT3.getX() + pointX) / 2;
                            double midy = (posT3.getY() + pointY) / 2;


                            if (legalPosition(environment, obstacles, midx, midy)) {
                                Line t3Line = new Line(Ray.getStartX(), Ray.getStartY(), posT3.getX(), posT3.getY());
                                ShadowNode newNode = new ShadowNode(posT3, tmp, t3Line);
                                System.out.println("Adding => " + newNode);


                                checkT3.add(newNode);
                                //System.out.println("woooooooooh good intersection");

                            }
                        }

                    }
                }
            }

            //System.out.println("Amount of Type 3 => " + checkT3.size());

        }


        ArrayList<ShadowNode> replacedT3 = new ArrayList<>();

        for (int i = 0; i < checkT3.size(); i++) {

            ShadowNode current = checkT3.get(i);

            for (int j = 0; j < checkT3.size(); j++) {
                if (i != j) {

                    ShadowNode checkInt = checkT3.get(j);
                    //  System.out.println("\nFOR => " + current.getRay() + "\tAND => " + checkInt.getRay());

                    if ((checkT3.get(j).getRight() != null && checkT3.get(i).getLeft() != null) ||(checkT3.get(i).getRight() != null && checkT3.get(i).getLeft() != null) && (checkT3.get(i).getRight() != checkT3.get(j).getLeft()||checkT3.get(j).getRight() != checkT3.get(i).getLeft())) {


                            if (lineIntersect(current.getRay(), checkInt.getRay())) {
                                if (nodePresent(FindIntersection(current.getRay(), checkInt.getRay())) == null) {
                                    if (nodePresent(checkT3.get(i).getPosition(), replacedT3) == null) {
                                        replacedT3.add(checkT3.get(i));
                                    }
                                    if (nodePresent(checkT3.get(j).getPosition(), replacedT3) == null) {
                                        replacedT3.add(checkT3.get(j));
                                    }
                                }
                                Point2D intersect = FindIntersection(current.getRay(), checkInt.getRay());
                                //TODO
                                //figure out which t2 and pursuer corresponds to each t3
                                ShadowNode correspondingT2left, correspondingT2Right;

                                Point2D agentLeft, agentRight;

                                if (checkT3.get(i).getLeft() != null && checkT3.get(j).getRight() != null && checkT3.get(i).getLeft().getType() == 2 && checkT3.get(j).getRight().getType() == 2) {
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

                                } else if (checkT3.get(i).getRight() != null && checkT3.get(j).getLeft() != null && checkT3.get(i).getRight().getType() == 2 && checkT3.get(j).getLeft().getType() == 2) {
                                    correspondingT2Right = checkT3.get(i).getRight();
                                    correspondingT2left = checkT3.get(j).getLeft();

                                    double correspondingAgentLeftX = checkT3.get(j).getRay().getStartX();
                                    double correspondingAgentLeftY = checkT3.get(j).getRay().getStartY();
                                    double correspondingAgentRightX = checkT3.get(i).getRay().getStartX();
                                    double correspondingAgentRightY = checkT3.get(i).getRay().getStartY();

                                    agentLeft = new Point2D(correspondingAgentLeftX, correspondingAgentLeftY);
                                    agentRight = new Point2D(correspondingAgentRightX, correspondingAgentRightY);
                                    ShadowNode posT4 = new ShadowNode(intersect, correspondingT2left, correspondingT2Right, agentLeft, agentRight);
                                    checkT4.add(posT4);

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



        }
        ShadowNode tmpLeft, tmpRight;

        ArrayList<ShadowNode> toRemove = t4overseen(checkT4);
        System.out.println(" ok here we go when it comes to adding t3 and t4");
        System.out.println("checkt3 size= " + checkT3.size() + " replacet3 size= " + replacedT3.size());
        System.out.println("-------------------------------------------------------");
        System.out.println("fuck it here is checkt3 = " + checkT3);
        System.out.println("-------------------------------------------------------");


        for (int i = 0; i < replacedT3.size(); i++) {
            // System.out.println("removing t3= " + checkT3.get(i));
            checkT3.remove(replacedT3.get(i));
        }

        for (int i = 0; i < checkT3.size(); i++) {
            if (nodePresent(checkT3.get(i).getPosition()) == null) {
                System.out.println("adding t3= " + checkT3.get(i));

                Nodes.add(checkT3.get(i));
            }
        }


        for (int i = 0; i < toRemove.size(); i++) {
            System.out.println("removing t4= " + checkT4.get(i));
            System.out.println("Size before => " + checkT4.size());
            checkT4 = removeFrom(toRemove.get(i), checkT4);
            System.out.println("Size AFTER => " + checkT4.size());
            //checkT4.remove(toRemove.get(i));
        }

        System.out.println("checkt4= " + checkT4);
        for (int i = 0; i < checkT4.size(); i++) {
            if (nodePresent(checkT4.get(i).getPosition()) == null) {
                System.out.println("adding t4= " + checkT4.get(i));
                Nodes.add(checkT4.get(i));
            }
        }


    }


    public void addT3ToGraph(ShadowNode posT3) {

        ShadowNode tmpNode, adj1, adj2;

        Point2D tmp = posT3.getPosition();
        Point2D start, end, p1, p2;
        double x1, x2, y1, y2;



        //we loop through to the type1 that is without a neighbor
        //Here we assume that we can keep going left
        if(posT3.left.getType() == 2)   {
            tmpNode = posT3.left;
            while(tmpNode.left != null) {
                tmpNode = tmpNode.left;
            }


            //TODO Continue here Rob
            if(tmpNode.left.getPosition() == getAdjacentPoints(tmpNode.getPosition(), allPolygons).get(0))   {

            }


        }


        /*

        Line line = getLineOn(tmp);

        x1 = line.getStartX();
        y1 = line.getStartY();
        start = new Point2D(x1, y1);

        x2 = line.getEndX();
        y2 = line.getEndY();
        end = new Point2D(x2, y2);

        if (nodePresent(start) != null || nodePresent(end) == null) {
            if (nodePresent(start) != null) {
                tmpNode = nodePresent(start);
            } else {
                tmpNode = nodePresent(end);
            }

            //We assume there already exists at least one Type3 that we want to overwrite
            if (tmpNode.getAdjT3() != null) {
                if (tmpNode.getAdjT3().length < 2) {
                    adj1 = tmpNode.getAdjT3()[0];
                    if (distance(adj1.getPosition(), tmp) > distance(posT3.getPosition(), tmp)) {
                        tmpNode.overwriteT3(posT3);
                    }
                } else if (tmpNode.getAdjT3().length == 2) {
                    adj1 = tmpNode.getAdjT3()[0];
                    adj2 = tmpNode.getAdjT3()[1];

                    if (onLine(adj1.getPosition(), line)) {
                        if (distance(adj1.getPosition(), tmp) > distance(posT3.getPosition(), tmp)) {
                            tmpNode.overwriteT3(posT3, adj1);
                        }
                    } else if (onLine(adj2.getPosition(), line)) {
                        if (distance(adj2.getPosition(), tmp) > distance(posT3.getPosition(), tmp)) {
                            tmpNode.overwriteT3(posT3, adj2);
                        }
                    } else {
                        System.exit(114);
                    }
                }
            } else {
                //We assume so far no Type3 has been added to this node

                //We assume to both sides there is nth
                if (tmpNode.getLeft() == null && tmpNode.getRight() == null) {
                    ArrayList<Point2D> adj = getAdjacentPoints(tmpNode.getPosition(), allPolygons);
                    p1 = adj.get(0);
                    p2 = adj.get(1);


                    if (onLine(p1, line)) {

                    }

                }


            }
        }

        */






    }


    public Line getLineOn(ShadowNode node) {
        return getLineOn(node.getPosition());
    }

    public Line getLineOn(Point2D point) {
        for (Line line : polygonEdges) {
            if (onLine(point, line)) {
                return line;
            }
        }
        return null;
    }


    public ArrayList<ShadowNode> t4overseen(ArrayList<ShadowNode> allPosT4) {
        System.out.println("Hello BITCHESSSSSSSSSS" + allPosT4.size());
        // take the possible t4, create a shape that has it and its two corresponding t2. check if any type 4s are contained in this shape. if yes then remove from list.
        ArrayList<ShadowNode> toRemove = new ArrayList<>();
        ShadowNode current;
        Point2D currentPoint;
        for (int i = 0; i < allPosT4.size(); i++) {
            current = allPosT4.get(i);
            currentPoint = allPosT4.get(i).getPosition();
            double l1, l2, r1, r2;
            l1 = 622;
            l2 = 238;
            r1 = 359;
            r2 = 232;
            Polygon polygon = new Polygon(currentPoint.getX(), currentPoint.getY(), l1, l2, r1, r2);
            // Polygon polygon = new Polygon(currentPoint.getX(), currentPoint.getY(), current.getLeft().getPosition().getX(), current.getLeft().getPosition().getY(), current.getRight().getPosition().getX(), current.getRight().getPosition().getY());

            for (int j = 0; j < allPosT4.size(); j++) {
                if (j != i) {
                    if (polygon.contains(allPosT4.get(j).getPosition())) {
                        if (nodePresent(allPosT4.get(i).getPosition(), toRemove) == null) {
                            toRemove.add(allPosT4.get(i));
                        }
                    }
                }
            }

        }
        System.out.println("to remove = " + toRemove);
        return toRemove;
    }


    public void addT3ToGraph(ShadowNode t2, Point2D t3) {


        ShadowNode pT2, newNode, tmp;
        //Line tLine;

        Point2D start, end, tmpPoint;
        double startX, startY, endX, endY;


        for (Line tLine : polygonEdges) {
            if (onLine(t3, tLine)) {


                startX = tLine.getStartX();
                startY = tLine.getStartY();
                start = new Point2D(startX, startY);

                endX = tLine.getEndX();
                endY = tLine.getEndY();
                end = new Point2D(endX, endY);

                if (t1.size() == 0 || (t1.contains(start) && t1.contains(end))) {
                    System.out.println("t1Size = " + t1.size());
                    System.exit(33211);
                }


                if (t1.contains(start) || t1.contains(end)) {

                    for (int i = 0; i < Nodes.size(); i++) {
                        tmp = Nodes.get(i);
                        tmpPoint = tmp.getPosition();

                        if (tmpPoint.getX() == startX && tmpPoint.getY() == startY) {
                            System.out.println("Entered first IF for = " + tmp);

                            if (tmp.getLeft() == null && tmp.getRight() == null) {
                                System.exit(666161);
                            }

                            if (tmp.getLeft() == null && (tmp.getRight().getType() == 1 || tmp.getRight().getType() == 2)) {
                                System.out.println("Entered 1-1");
                                newNode = new ShadowNode(t3, t2, tmp, true);
                                Nodes.add(newNode);
                            } else if (tmp.getLeft().getType() == 3 && (tmp.getRight().getType() == 1 || tmp.getRight().getType() == 2)) {
                                System.out.println("possible Update for Type3 - case 1 - entered!!!");
                                if (distance(t3, tmpPoint) < distance(tmp.getLeft().getPosition(), tmpPoint)) {
                                    System.out.println("Type3 update entered!!!");
                                    newNode = new ShadowNode(t3, t2, tmp, true);
                                    Nodes.add(newNode);
                                }
                            }

                        } else if (tmpPoint.getX() == endX && tmpPoint.getY() == endY) {
                            System.out.println("Entered second IF for = " + tmp);

                            if (tmp.getLeft() == null && tmp.getRight() == null) {
                                System.exit(666162);
                            }


                            if (tmp.getRight() == null && (tmp.getLeft().getType() == 1 || tmp.getLeft().getType() == 2)) {
                                System.out.println("Entered 2-1");
                                newNode = new ShadowNode(t3, tmp, t2, true);
                                Nodes.add(newNode);
                            } else if (tmp.getRight().getType() == 3 && (tmp.getLeft().getType() == 1 || tmp.getLeft().getType() == 2)) {
                                System.out.println("possible Update for Type3 - case 2 - entered!!!");
                                if (distance(t3, tmpPoint) < distance(tmp.getLeft().getPosition(), tmpPoint)) {
                                    System.out.println("Type3 update entered!!!");
                                    newNode = new ShadowNode(t3, tmp, t2, true);
                                    Nodes.add(newNode);
                                }
                            }
                        }
                    }
                }


            }
        }


        /*
        for(int i = 0; i < Nodes.size() - 1; i++)   {
            tNode = Nodes.get(i);

            if(tNode.getRight() != null && tNode.getRight().getType() == 2)   {
                tLine = new Line(tNode.getPosition().getX(),tNode.getPosition().getY(), tNode.getRight().getPosition().getX(),tNode.getRight().getPosition().getY());
                if(onLine(t3, tLine))   {
                    System.out.println("Entered 1\n T3 = " + t3 + "\nPREV: " + tNode + "\nNEXT: " + t2 + "\n");
                    newNode = new ShadowNode(t3, t2, tNode, true);
                    Nodes.add(newNode);
                }
            }
            else if(tNode.getLeft() != null && tNode.getLeft().getType() == 2)    {

                tLine = new Line(tNode.getPosition().getX(),tNode.getPosition().getY(), tNode.getLeft().getPosition().getX(),tNode.getLeft().getPosition().getY());
                if(onLine(t3, tLine))   {
                    System.out.println("Entered 2");
                    newNode = new ShadowNode(t3, tNode, t2, true);
                    Nodes.add(newNode);
                }
            }



        }

        */

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
        //System.out.print("Passed ray = " + ray);

        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for (Line inLine : polygonEdges) {
            if (lineIntersect(inLine, ray)) {
                //System.out.println("INTERSECT DETECTED");
                if (nodePresent(FindIntersection(inLine, ray)) == null) {
                    intersectPoints.add(FindIntersection(inLine, ray));
                }
                //System.out.println("AT = " + intersectPoints.get(intersectPoints.size()-1) + "\tWITH = " + inLine + "\n");
            }
        }

        double min = Double.MAX_VALUE;
        for (int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            tmpLine = new Line(ray.getStartX(), ray.getStartY(), tmpPoint.getX(), tmpPoint.getY());
            dist = Math.sqrt(Math.pow((tmpLine.getEndX() - tmpLine.getStartX()), 2) + Math.pow((tmpLine.getEndY() - tmpLine.getStartY()), 2));

            if (dist < min) {
                minPos = i;
                min = dist;
            }

        }

        if (intersectPoints.size() > 0) {
            return intersectPoints.get(minPos);
        } else {
            return null;
        }
    }


}
