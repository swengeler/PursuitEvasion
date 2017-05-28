package shadowPursuit;

import additionalOperations.GeometryOperations;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.Shadow;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.apache.commons.math3.ode.sampling.StepNormalizerMode;
import org.apache.log4j.net.SyslogAppender;
import simulation.MapRepresentation;

import javax.swing.plaf.synth.SynthUI;
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
    private ArrayList<ArrayList<ShadowNode>> shadows = new ArrayList<>();

    private ArrayList<Point2D> pointy;


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





        //generateT1Connections();
        //circleDetect();
        //calcT2Points();
        //printGraph();

        generateType1();
        calculateType3();
        addT3AndT4ToGraph();

        printNodes();
        printShadows();

    }


    //@TODO problem for objects that are entierly in shadow
    public void calcT2Points() {
        ArrayList<Point2D> temp, T2Points;
        T2Points = new ArrayList<>();
        Point2D tmpPoint;
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
        if(point == null)   {
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


    public void printNodes()    {
        int cunt = 0;
        for(int i = 0; i < Nodes.size(); i++)   {

            if(Nodes.get(i).numberOfLinks() == 2)
                cunt++;
        }
        System.out.println("\n------------Nodes are printed------------");
        System.out.println("Number of Nodes = " + Nodes.size() + "\nNumber of double = " + cunt);

        for(int i = 0; i < Nodes.size(); i++) {
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
                                //System.out.println("Adding => " + newNode);


                                checkT3.add(newNode);
                                //System.out.println("woooooooooh good intersection");


                                //TODO JONTY: if a type 4 is inside another but they creatr a shadow due to their intersecting shadows. because they dont share the same type 2's

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

                    if ((checkT3.get(j).getRight() != null && checkT3.get(i).getLeft() != null) || (checkT3.get(i).getRight() != null && checkT3.get(i).getLeft() != null) && (checkT3.get(i).getRight() != checkT3.get(j).getLeft() || checkT3.get(j).getRight() != checkT3.get(i).getLeft())) {

                            Line checkingi= new Line(current.getConnectedType2().getPosition().getX(),current.getConnectedType2().getPosition().getY(), current.getRay().getEndX(),current.getRay().getEndY());
                            Line checkingj= new Line( checkInt.getConnectedType2().getPosition().getX(),checkInt.getConnectedType2().getPosition().getY(), checkInt.getRay().getEndX(),checkInt.getRay().getEndY());
                        if (lineIntersect(checkingi, checkingj)) {
                            // fucking jontyif (nodePresent(FindIntersection(current.getRay(), checkInt.getRay())) == null) {

                           //fucking jonty Point2D intersect = FindIntersection(current.getRay(), checkInt.getRay());
                            Point2D intersect = findIntersect2(current.getRay(), checkInt.getRay());

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
                                checkT4.get(checkT4.size()-1).connectT2();

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
            int seenby = 0;
            for (int q = 0; q < agents.size(); q++) {








                System.out.println("\n\nagent = " + agents.get(q));
                System.out.println("Checking possible T3 =  " + checkT3.get(i));
                System.out.println("checking for this agent= " + agents.get(q));
                System.out.println("Is visible = " + isVisible(agents.get(q).getX(),agents.get(q).getY(),checkT3.get(i).getPosition().getX(),checkT3.get(i).getPosition().getY(),polygonEdges));





                if(agents.get(q).getX()==checkT3.get(i).getRay().getStartX() && agents.get(q).getY()==checkT3.get(i).getRay().getStartY() ){
                    seenby++;
                    System.out.println("lookey here");
                }
                if (isVisible(agents.get(q).getX(),agents.get(q).getY(),checkT3.get(i).getPosition().getX(),checkT3.get(i).getPosition().getY(),polygonEdges)) {
                    if(agents.get(q).getX()==checkT3.get(i).getRay().getStartX() && agents.get(q).getY()==checkT3.get(i).getRay().getStartY() ){
                        System.out.println("muahahahahaha ");
                    }else
                    seenby++;
                }
            }
            if (seenby > 1) {
                replacedT3.add(checkT3.get(i));
                //System.out.println("wooh one works");
            }


        }
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

        for (int i = 0; i < replacedT3.size(); i++) {
            //
            //System.out.println("testing jontys sanity test = 1" );
            //printGraph();
            checkT3.remove(replacedT3.get(i));
        }

        printGraph();
        for (int i = 0; i < checkT3.size(); i++) {
            if (nodePresent(checkT3.get(i).getPosition()) == null) {
                System.out.println("adding t3= " + checkT3.get(i));

                Nodes.add(checkT3.get(i));
            }
        }

        //printGraph();
        //System.out.println("number of t4's= " + checkT4.size());
        for (int i = 0; i < toRemove.size(); i++) {
            //System.out.println("removing t4= " + checkT4.get(i));
            //System.out.println("Size before => " + checkT4.size());
            checkT4.remove(toRemove.get(i));
            //System.out.println("Size AFTER => " + checkT4.size());
            //checkT4.remove(toRemove.get(i));
        }

        //printGraph();
        //System.out.println("checkt4= " + checkT4.size());
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

        //clearingUpConnections(Nodes);
    }

    public void addT3AndT4ToGraph() {
        //System.out.println("----------------------------------------------");
        //printGraph();
        //System.out.println("----------------------------------------------");



        ShadowNode tmp, tmp2, tmp3, tmp4, tmp5, tmp6;
        Point2D tmpPoint, tmpPoint2, tmpPoint3, tmpStart, tmpEnd;
        Line tmpLine, tmpLine2;
        double mionDist;

        pointy = findReflex(environment, allPolygons, obstacles);
        //Connect Type2s to one another if there are on the same polyedge
        ArrayList<Point2D> adjacentP;
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 2 && (tmp.getLeft() == null || tmp.getRight() == null)) {
                adjacentP = getAdjacentPoints(tmp.getPosition(), allPolygons);

                if (pointy.contains(adjacentP.get(0)) || pointy.contains(adjacentP.get(1))) {
                    if (tmp.getLeft() == null && pointy.contains(adjacentP.get(0)) && nodePresent(adjacentP.get(0)) != null) {
                        System.out.println("Problem = " + adjacentP.get(0));
                        tmp2 = nodePresent(adjacentP.get(0));
                        tmp.left = tmp2;
                        System.out.println(adjacentP.get(0));
                        if (tmp.getRight() != null) {
                            tmp2.right = tmp;
                        } else {
                            System.exit(9898);
                        }
                    } else if (tmp.getRight() == null && pointy.contains(adjacentP.get(1)) && nodePresent(adjacentP.get(1)) != null) {
                        tmp2 = nodePresent(adjacentP.get(1));
                        tmp.right = tmp2;
                        if (tmp.getLeft() != null) {
                            tmp2.left = tmp;
                        } else {
                            System.exit(8989);
                        }
                    }
                }
            }
        }


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
                                System.out.println("PROBLEM Type3=> " + tmp +"\nLEFT => " + tmp.getLeft() + "\nRIGHT => " + tmp.getRight());
                                if (tmp.getLeft() != null && tmp.getLeft().getType() == 2) {
                                    System.out.println("Enter 1");
                                    tmp.getLeft().right = null;
                                    tmp.left = tmp2;
                                    if (tmp2.getRight() == null) {
                                        System.exit(686);
                                    }
                                    System.out.println("PRINT => " + tmp2);
                                    tmp2.right = tmp;
                                } else if (tmp.getRight() != null && tmp.getRight().getType() == 2) {
                                    System.out.println("Enter 2");
                                    tmp.getRight().left = null;
                                    tmp.right = tmp2;
                                    if (tmp2.getLeft() == null) {
                                        System.exit(868);
                                    }
                                    tmp2.left = tmp;
                                }
                                System.out.println("AFTER T4 connect => " + tmp +"\nLEFT => " + tmp.getLeft() + "\nRIGHT => " + tmp.getRight()+ "\n\n");
                            }
                        }

                    }
                }
            }
        }


        ArrayList<ShadowNode> sameLine;
        //Connect Type3 to next Type3 or Type1
        for (int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            if (tmp.getType() == 3) {
                tmpLine = getLineOn(tmp);
                sameLine = new ArrayList<>();
                sameLine.add(tmp);
                for (int j = 0; j < Nodes.size(); j++) {
                    if (i != j && Nodes.get(j).getType() == 3) {
                        tmp2 = Nodes.get(j);
                        tmpPoint = tmp2.getPosition();
                        if (onLine(tmpPoint, tmpLine)) {
                            sameLine.add(tmp2);
                        }
                    }
                }

                tmpStart = new Point2D(tmpLine.getStartX(), tmpLine.getStartY());
                if (sameLine.size() > 1) {
                    ArrayList<ShadowNode> areOnLine = orderByClosestToPoint(sameLine, tmpStart);
                    for (int j = 0; j < areOnLine.size() - 1; j++) {
                        tmp3 = areOnLine.get(j);
                        tmpPoint3 = tmp3.getPosition();
                        tmp4 = areOnLine.get(j + 1);
                        tmpEnd = tmp4.getPosition();
                        tmpLine2 = new Line(tmpPoint3.getX(), tmpPoint3.getY(), tmpEnd.getX(), tmpEnd.getY());
                        tmpPoint3 = getLineMiddle(tmpLine2);

                        if (!isVisible2(tmpPoint3))    {
                            tmp3.connect(tmp4);
                            tmp3.connectT2();
                            tmp4.connectT2();
                            j++;
                        }
                        else    {
                            tmp5 = nodePresent(new Point2D(tmpLine.getEndX(), tmpLine.getEndY()));
                            tmp3.connect(tmp5);
                            tmp3.connectT2();

                            if(nodePresent(new Point2D(tmpLine.getStartX(), tmpLine.getStartY())) != null)  {
                                tmp6 = nodePresent(new Point2D(tmpLine.getStartX(), tmpLine.getStartY()));
                                tmp4.connect(tmp6);
                                tmp4.connectT2();
                            }

                            break;
                        }

                    }
                }
                else {
                    System.out.println("FOR T3-NODE => " + tmp);

                    if(nodePresent(new Point2D(tmpLine.getStartX(), tmpLine.getStartY())) != null && nodePresent(new Point2D(tmpLine.getStartX(), tmpLine.getStartY())).getType() == 1) {
                        tmp3 = nodePresent(new Point2D(tmpLine.getStartX(), tmpLine.getStartY()));
                        tmp.connect(tmp3);
                    }
                    else if(nodePresent(new Point2D(tmpLine.getEndX(), tmpLine.getEndY())) != null && nodePresent(new Point2D(tmpLine.getEndX(), tmpLine.getEndY())).getType() == 1) {
                        tmp3 = nodePresent(new Point2D(tmpLine.getEndX(), tmpLine.getEndY()));
                        tmp.connect(tmp3);
                    }
                    else {
                        System.out.println("You might wanna check that T3 again ....\nTO CHECK => " + tmp);
                        System.exit(989898);
                    }
                    System.out.println("NOW..... => " + tmp);

                }

            }
        }

        for(ShadowNode node : Nodes)    {
            if(node.getType() == 3 && (node.getRight().getType() == 2 || node.getLeft().getType() == 2))    {
                node.connectT2();
            }
        }



    }


    public boolean isVisible2(Point2D point) {
        boolean visble = false;
        int count = 0;
        for (Point2D agent : agents) {
            for (Line line : polygonEdges) {
                if (!GeometryOperations.lineIntersect(line, agent.getX(), agent.getY(), point.getX(), point.getY())) {
                    count++;
                }
            }
            if (count == 0) {
                return true;
            } else count = 0;

        }
        return false;
    }


    public ShadowNode getClosestT1(ShadowNode node) {
        return getClosestT1(node.getPosition());
    }


    public ShadowNode getClosestT1(Point2D point) {
        Line line = getLineOn(point);
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


    //
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
                    if (polygon.contains(allPosT4.get(j).getPosition()) &&(
                            (
                                    allPosT4.get(j).getLeft() == allPosT4.get(i).getRight() &&
                                    allPosT4.get(j).getRight() == allPosT4.get(i).getLeft()) ||
                            (allPosT4.get(i).getLeft() == allPosT4.get(j).getLeft() &&
                            allPosT4.get(j).getRight() == allPosT4.get(i).getRight()))){

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
                            pointsInShadow.add(new ShadowNode(findIntersect2(ray1,ray3)));
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


                    }else {
                        //System.out.println("fuck you jonty");
                    }
                }
            }

        }
        //System.out.println("to remove = " + toRemove);
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
//     fucking jonty           if (nodePresent(FindIntersection(inLine, ray)) == null) {
               //fucking jonty intersectPoints.add(FindIntersection(inLine, ray));
                    if (nodePresent(findIntersect2(inLine, ray)) == null && findIntersect2(inLine, ray) != null) {
                        intersectPoints.add(findIntersect2(inLine, ray));
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



    public void calculateNodes(MapRepresentation map){
        ArrayList<Point2D> pointy= findReflex(map.getBorderPolygon(),map.getAllPolygons(),map.getObstaclePolygons());
        ArrayList<Line> rays= new ArrayList<>();

        for(int i=0; i<pointy.size();i++){
            //find its neighbours left and right.
            //create ray from neigbour to it and extend
           // Line ray= new Line(pointy.get(i).getX(),pointy.get(i).getY(),)
            // rays.add(ray)
            //check any intersections and set them as points
            //then find any midpoints between the pointy and first intersection
            //set that as a point

        }

    }

    public void connectNodes(MapRepresentation map, ArrayList<Point2D> nodes,ArrayList<Line> rays ){

        for(int i=0; i<nodes.size();i++){
            for(int j=0; j<nodes.size(); j++){
                if(isVisible(nodes.get(i).getX(),nodes.get(i).getY(), nodes.get(j).getX(),nodes.get(j).getY(),rays)){
                    boolean crossed= false;
                    Line crossocc= new Line(nodes.get(i).getX(),nodes.get(i).getY(), nodes.get(j).getX(),nodes.get(j).getY());
                    for(int q=0;q<rays.size();q++){
                        if(lineIntersect(crossocc,rays.get(q))){
                            crossed=true;
                        }

                    }
                    if(crossed==false){
                       // nodes.get(i).addChild(nodes.get(j));
                      //  nodes.get(j).addChild(nodes.get(i));
                    }
                }
            }
        }

    }


    public void clearingUpConnections(ArrayList<ShadowNode> nodes){
        for(int i=0;i<nodes.size();i++){
            for(int j=0;j<nodes.size();j++){
                if(i!=j){
                    if(nodes.get(j).getRight()==nodes.get(i).getRight()){
                        nodes.get(j).setLeft(nodes.get(i));
                        nodes.get(i).setRight(nodes.get(j));

                    }
                    if(nodes.get(j).getLeft()==nodes.get(i).getLeft()){
                        nodes.get(j).setRight(nodes.get(i));
                        nodes.get(i).setLeft(nodes.get(j));
                    }
                }
            }

            }

    }

    public void printShadows()  {
        //First find cycle
        ArrayList<ShadowNode> doublyConnested = new ArrayList<>();
        ShadowNode tmp, start, next, prev;

        for(int i = 0; i < Nodes.size(); i++)   {
            if(Nodes.get(i).numberOfLinks() == 2)   {
                doublyConnested.add(Nodes.get(i));
            }
        }
        ArrayList<ShadowNode> visited = new ArrayList<>();
        ArrayList<ArrayList<ShadowNode>> shadows = new ArrayList<>();
        ArrayList<ShadowNode> shadow;
        int counter = 0;

        while(visited != doublyConnested && visited.size() < doublyConnested.size())   {
            for(int i = 0; i < Nodes.size(); i++)   {
                tmp = Nodes.get(i);
                if(tmp.numberOfLinks() == 2 && !visited.contains(tmp))  {
                    start = tmp;
                    shadow = new ArrayList<>();
                    visited.add(start);
                    shadow.add(start);


                    prev = start;
                    next = start.getRight();
                    int c=0;
                    //while(next.getPosition() != start.getPosition() )    {
                      while(!visited.contains(next))    {
                        System.out.println(c);
                        visited.add(next);
                        shadow.add(next);
                        if(next.getRight() != prev && next.getRight().numberOfLinks() == 2) {
                            next = next.getRight();
                            prev = next.getLeft();
                        }
                        else if(next.getLeft() != prev && next.getLeft().numberOfLinks() == 2) {
                            next = next.getLeft();
                            prev = next.getRight();
                            prev = next.getRight();
                        }
                        c++;
                    }
                    shadows.add(shadow);

                }
                counter++;
                if(visited == doublyConnested || visited.size() >= doublyConnested.size() ) {
                    break;
                }
            }
        }

        for(int i=0; i<shadows.size();i++){
            System.out.println("------------------------new shadow starting now ------------------------");
            for(int j=0; j<shadows.get(i).size();j++){
                System.out.println("Shadow node= " + shadows.get(i).get(j));
            }
            System.out.println("------------------------------------------------");

        }


    }
}
