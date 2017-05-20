package shadowPursuit;

import javafx.geometry.Point2D;

import javafx.scene.Node;
import javafx.scene.effect.Light;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.omg.CORBA.DoubleHolder;
import pathfinding.PathVertex;
import simulation.Agent;
import simulation.MapRepresentation;

import javax.sound.midi.SysexMessage;
import java.lang.reflect.Array;
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

        for(Point2D point : polyToPoints(environment))  {
            System.out.println(point);
        }


        this.agents = agents;

        Nodes = new ArrayList<>();















        generateType1();


        //generateT1Connections();
        //circleDetect();
        //calcT2Points();
        //printGraph();

        //calculateType3();
        printGraph();
        //printGraph();
        //getType2();

        //printGraph();
    }


    //@TODO problem for objects that are entierly in shadow
    public void calcT2Points()    {
        ArrayList<Point2D> temp, T2Points;
        T2Points = new ArrayList<>();
        Point2D tmpPoint;
        ShadowNode tmpNode, tmp2Node, tNode;
        ArrayList<Point2D> pointy = findReflex(environment,allPolygons, obstacles);
        System.out.println("pointy points :D = " + pointy);


        for(int i = 0; i < Nodes.size(); i++)    {
            tNode = Nodes.get(i);
            //System.out.println("For Node = " + tNode);


            temp = new ArrayList<>();
            if((tNode.left == null || tNode.right == null) && tNode.getType() == 1)  {
                temp = getAdjacentPoints(tNode.getPosition(), allPolygons);
                //System.out.println("For = " + tNode.getPosition() + "\tAdjacent: " + temp);
                if(tNode.right == null)   {
                    //false
                    //System.out.println("tNode = " + tNode);
                    tmpPoint = tNode.left.getPosition();
                    if(temp.get(0) == tmpPoint) {
                        if(pointy.contains(temp.get(0))) {
                            Nodes.add(new ShadowNode(temp.get(0), tNode));
                        }
                    }
                    else    {
                        if(pointy.contains(temp.get(1))) {
                            Nodes.add(new ShadowNode(temp.get(1), tNode));
                        }
                    }
                }
                else if(tNode.left == null)   {
                    //Correct
                    tmpPoint = tNode.right.getPosition();
                    if(temp.get(0) == tmpPoint) {
                        if(pointy.contains(temp.get(1))) {
                            Nodes.add(new ShadowNode(temp.get(1), tNode));
                        }
                    }
                    else    {
                        if(pointy.contains(temp.get(0))) {
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
        ArrayList<Point2D> pointy = findReflex(environment,allPolygons, obstacles);

        ShadowNode tmp, tmp2, newNode, t2Shad;
        Point2D tmpPoint, tmpPoint2, shadPoint;

        ArrayList<Point2D> adj;

        System.out.println("\n");

        boolean failed = false;

        for(Point2D point : t1) {
            newNode = new ShadowNode(point);
            Nodes.add(newNode);
        }

        for(int i = 0; i < Nodes.size(); i++) {
            tmp = Nodes.get(i);
            tmpPoint = tmp.getPosition();
            if(tmp.getType() == 1) {
                adj = getAdjacentPoints(tmpPoint, allPolygons);

                //Left side
                if (t1.contains(adj.get(0))) {
                    tmpPoint2 = adj.get(0);
                    for (int j = 0; j < Nodes.size(); j++) {
                        tmp2 = Nodes.get(j);
                        if (tmp2.getType() == 1) {
                            shadPoint = tmp2.getPosition();
                            if (tmpPoint2.getX() == shadPoint.getX() && tmpPoint2.getY() == shadPoint.getY()) {
                                tmp.setLeft(tmp2);
                                break;
                            }
                        }
                    }
                }
                else    {
                    if (pointy.contains(adj.get(0)) && tmp.left == null) {
                        t2Shad = new ShadowNode(adj.get(0), true);
                        tmp.setLeft(t2Shad);
                        Nodes.add(t2Shad);
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
                            break;
                        }
                    }
                }
                else {
                    if (pointy.contains(adj.get(1)) && tmp.right == null) {
                        t2Shad = new ShadowNode(adj.get(1), true);
                        tmp.setRight(t2Shad);
                        Nodes.add(t2Shad);
                    }
                }
            }


        }








    }


    public void printGraph()    {
        ArrayList<ShadowNode> printed = new ArrayList<>();

        System.out.println("Number of Nodes = " + Nodes.size());

        ShadowNode start,start2, tmp, tmp2;

        //for(int i = 0; i < copied.size())
        //ShadowNode temp = copied.get(0);
        double sX, sY, s2X, s2Y, tmpX, tmpY, tmp2X, tmp2Y;


        for(int i = 0; i < Nodes.size(); i++)  {
            tmp = Nodes.get(i);
            tmpX = tmp.getPosition().getX();
            tmpY = tmp.getPosition().getY();


            if(printed.size() == 0 || !printed.contains(tmp))   {
                tmp2 = tmp;
                if(tmp.left != null)    {
                    tmp2 = tmp.getLeft();
                    tmp2X = tmp2.getPosition().getX();
                    tmp2Y = tmp2.getPosition().getY();
                    while(tmp2.getLeft() != null && (tmp2X != tmpX && tmp2Y != tmpY)) {
                        tmp2 = tmp2.getLeft();
                        tmp2X = tmp2.getPosition().getX();
                        tmp2Y = tmp2.getPosition().getY();
                    }
                }

                //We are now at the start
                while(tmp2 != null) {
                    System.out.println(tmp2);
                    printed.add(tmp2);
                    tmp2 = tmp2.getRight();
                }
                System.out.println("\n");
            }
        }
    }



    public void circleDetect()   {
        ArrayList<ShadowNode> printed = new ArrayList<>();


        ShadowNode start,start2, tmp;
        //for(int i = 0; i < copied.size())
        //ShadowNode temp = copied.get(0);
        double sX, sY, s2X, s2Y, tmpX, tmpY;


        for(int i = 0; i < Nodes.size(); i++)  {
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

            if(printed.size() == 0 || !printed.contains(start)) {

                if(start.right != null || start.left != null)   {
                    //get to start
                    tmp = start.left;
                    boolean cycle = false;

                    if(tmp != null) {
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
                    }
                    else    {
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


                    while((tmpX != s2X || tmpY != s2Y) && tmp != null)    {

                        printed.add(tmp);
                        if(cycle && printed.get(printed.size()-1).right == null) {
                            start2.left.setRight(start2);
                        }
                        //System.out.println(tmp);
                        tmp = tmp.right;
                        if(tmp != null) {
                            tmpX = tmp.getPosition().getX();
                            tmpY = tmp.getPosition().getY();
                        }
                    }


                    if(tmpX == s2X && tmpY == s2Y)   {
                        //System.out.println(tmp);
                        printed.add(tmp);
                    }


                }
                else    {
                    System.out.println(start);
                    printed.add(start);
                    System.out.println("\n");
                }
            }
        }
    }

    //TODO @Rob - keep working here
    public void calculateType3()    {
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
        for(int i =0; i < Nodes.size(); i++)    {
            tmp = Nodes.get(i);
            if(tmp.getType() == 2){
                //System.out.println(tmp.getPosition());
                newL = new Line(tmp.getPosition().getX(), tmp.getPosition().getY(), agents.get(0).getX(), agents.get(0).getY());
                //System.out.println("Visible? => " + isVisible(tmp.getPosition().getX(), tmp.getPosition().getY(), agents.get(0).getX(), agents.get(0).getY(),polygonEdges));
            }

            if(tmp.getPosition().getX() < minX) {
                minX = tmp.getPosition().getX();
                maxXDist = maxX - minX;
            }
            else if(tmp.getPosition().getX() > maxX) {
                maxX = tmp.getPosition().getX();
                maxXDist = maxX - minX;
            }

            if(tmp.getPosition().getY() < minY) {
                minX = tmp.getPosition().getY();
                maxYDist = maxY - minY;
            }
            else if(tmp.getPosition().getY() > maxY) {
                minX = tmp.getPosition().getY();
                maxYDist = maxY - minY;
            }

        }

        if(maxYDist < maxXDist) {
            rayLength = maxXDist * 2;
        }
        else    {
            rayLength = maxYDist * 2;
        }



        for(Point2D agent : agents) {

            agentX = agent.getX();
            agentY = agent.getY();

            //For every point of Type2
            for(int i =0; i < Nodes.size(); i++) {
                tmp = Nodes.get(i);
                if (tmp.getType() == 2) {

                    pointX = tmp.getPosition().getX();
                    pointY = tmp.getPosition().getY();

                    if(isVisible(agentX, agentY, pointX, pointY, polygonEdges)) {
                        //Create occlusion Ray
                        //System.out.println("For Agent = " + agent + " and Point = " + tmp.getPosition());
                        Ray = scaleRay(agent, tmp, rayLength);
                        //System.out.println("RAY = " + Ray);

                        Line original = new Line(agentX, agentY, tmp.getPosition().getX(), tmp.getPosition().getY());
                        //System.out.println("Gradient1 = " + gradient(original) + "\tGradient2 = " + gradient(Ray));

                        Point2D posT3 = getT3Intersect(Ray);

                        System.out.println("");
                        addT3ToGraph(tmp,posT3);
                        Type3.add(posT3);
                    }
                }
            }

        }






    }


    public void addT3ToGraph(ShadowNode t2, Point2D t3)   {



        ShadowNode pT2, newNode, tmp;
        //Line tLine;

        Point2D start, end, tmpPoint;
        double startX, startY, endX, endY;



        for(Line tLine : polygonEdges)  {
            if(onLine(t3, tLine))   {


                startX = tLine.getStartX();
                startY = tLine.getStartY();
                start = new Point2D(startX, startY);

                endX = tLine.getEndX();
                endY = tLine.getEndY();
                end = new Point2D(endX, endY);

                if(t1.size() == 0 || (t1.contains(start) && t1.contains(end)))  {
                    System.out.println("t1Size = " + t1.size());
                    System.exit(33211);
                }


                if(t1.contains(start) || t1.contains(end))  {

                    for(int i = 0; i < Nodes.size(); i++)    {
                        tmp = Nodes.get(i);
                        tmpPoint = tmp.getPosition();

                        if(tmpPoint.getX() == startX && tmpPoint.getY() == startY)    {
                            System.out.println("Entered first IF for = " + tmp);

                            if(tmp.getLeft() == null && tmp.getRight() == null)
                                System.exit(666161);

                            if(tmp.getLeft() == null && (tmp.getRight().getType() == 1 || tmp.getRight().getType() == 2))   {
                                System.out.println("Entered 1-1");
                                newNode = new ShadowNode(t3,t2, tmp, true);
                                Nodes.add(newNode);
                            }
                            else if(tmp.getLeft().getType() == 3 && (tmp.getRight().getType() == 1 || tmp.getRight().getType() == 2))     {
                                System.out.println("possible Update for Type3 - case 1 - entered!!!");
                                if(distance(t3, tmpPoint) < distance(tmp.getLeft().getPosition(), tmpPoint))    {
                                    System.out.println("Type3 update entered!!!");
                                    newNode = new ShadowNode(t3,t2, tmp, true);
                                    Nodes.add(newNode);
                                }
                            }

                        }
                        else if(tmpPoint.getX() == endX && tmpPoint.getY() == endY) {
                            System.out.println("Entered second IF for = " + tmp);

                            if(tmp.getLeft() == null && tmp.getRight() == null)
                                System.exit(666162);


                            if(tmp.getRight() == null && (tmp.getLeft().getType() == 1 || tmp.getLeft().getType() == 2))   {
                                System.out.println("Entered 2-1");
                                newNode = new ShadowNode(t3,tmp, t2, true);
                                Nodes.add(newNode);
                            }
                            else if(tmp.getRight().getType() == 3 && (tmp.getLeft().getType() == 1 || tmp.getLeft().getType() == 2))     {
                                System.out.println("possible Update for Type3 - case 2 - entered!!!");
                                if(distance(t3, tmpPoint) < distance(tmp.getLeft().getPosition(), tmpPoint))    {
                                    System.out.println("Type3 update entered!!!");
                                    newNode = new ShadowNode(t3,tmp, t2, true);
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


    public void getType2()    {
        ArrayList<ShadowNode> t2 = new ArrayList<>();

        for(ShadowNode node : Nodes)    {
            if(node.getType() == 2) {
                t2.add(node);
            }
        }

        for(ShadowNode node: t2)    {
            System.out.println(node);
        }
    }

    public Point2D getT3Intersect(Line ray)    {
        //System.out.print("Passed ray = " + ray);

        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for(Line inLine : polygonEdges)    {
             if(lineIntersect(inLine, ray)) {
                System.out.println("INTERSECT DETECTED");
                intersectPoints.add(FindIntersection(inLine,ray));
                System.out.println("AT = " + intersectPoints.get(intersectPoints.size()-1) + "\tWITH = " + inLine + "\n");
            }
        }

        double min = Double.MAX_VALUE;
        for(int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            tmpLine = new Line(ray.getStartX(), ray.getStartY(), tmpPoint.getX(), tmpPoint.getY());
            dist = Math.sqrt(Math.pow((tmpLine.getEndX() - tmpLine.getStartX()), 2) + Math.pow((tmpLine.getEndY() - tmpLine.getStartY()), 2));

            if(dist < min)  {
                minPos = i;
                min=dist;
            }

        }

        if(intersectPoints.size() > 0)
            return intersectPoints.get(minPos);
        else
            return null;
    }


}
