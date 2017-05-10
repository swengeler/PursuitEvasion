package shadowPursuit;

import javafx.geometry.Point2D;

import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import simulation.Agent;
import simulation.MapRepresentation;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static additionalOperations.GeometryOperations.lineIntersect;
import static additionalOperations.GeometryOperations.occRay;
import static additionalOperations.GeometryOperations.pointIntersect;
import static shadowPursuit.shadowOperations.getAdjacentPoints;
import static shadowPursuit.shadowOperations.getX1Points;
import static shadowPursuit.shadowOperations.isVisible;

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


    public ShadowGraph(MapRepresentation map, ArrayList<Point2D> agents) {
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();

        allPolygons = map.getAllPolygons();
        this.agents = agents;

        generateType1();
        generateT1Connections();
    }

    //used for testpurpoes
    public ShadowGraph(Polygon environment, ArrayList<Polygon> Obstacles, ArrayList<Point2D> agents) {
        this.environment = environment;
        this.obstacles = Obstacles;

        allPolygons = new ArrayList<>();
        allPolygons.add(environment);
        allPolygons.addAll(Obstacles);

        polygonEdges = new ArrayList<>();
        for (Polygon p : allPolygons) {
            for (int i = 0; i < p.getPoints().size(); i += 2) {
                polygonEdges.add(new Line(p.getPoints().get(i), p.getPoints().get(i + 1), (p.getPoints().get((i + 2) % p.getPoints().size())), (p.getPoints().get((i + 3) % p.getPoints().size()))));
            }
        }


        this.agents = agents;

        Nodes = new ArrayList<>();
        generateType1();

        generateT1Connections();
        calcT2Points();
        printGraph();
    }


    //@TODO problem for objects taht are entierly in shadow
    public void calcT2Points()    {
        ArrayList<Point2D> temp, T2Points;
        T2Points = new ArrayList<>();
        Point2D tmpPoint;
        ShadowNode tmpNode, tmp2Node, tNode;




        for(int i = 0; i < Nodes.size(); i++)    {
            tNode = Nodes.get(i);
            //System.out.println("For Node = " + tNode);


            temp = new ArrayList<>();
            if((tNode.prev == null || tNode.next == null) && tNode.getType() == 1)  {
                temp = getAdjacentPoints(tNode.getPosition(), allPolygons);
                if(tNode.next == null)   {
                    tmpPoint = tNode.prev.getPosition();
                    if(temp.get(0) == tmpPoint) {
                        Nodes.add(new ShadowNode(temp.get(1), tNode));
                    }
                    else    {
                       Nodes.add(new ShadowNode(temp.get(0), tNode));
                    }
                }
                else if(tNode.prev == null)   {
                    tmpPoint = tNode.next.getPosition();
                    if(temp.get(0) == tmpPoint) {
                        Nodes.add(new ShadowNode(temp.get(1), tNode));
                    }
                    else    {
                        Nodes.add(new ShadowNode(temp.get(0), tNode));
                    }
                }

            }
            //System.out.println("i = " + i);
        }
    }

    public void generateType1() {
        ArrayList<Point2D> t1 = getX1Points(environment, obstacles, agents);

        ShadowNode temp;
        for (Point2D point : t1) {


            temp = new ShadowNode(point);
            Nodes.add(temp);
        }


    }


    public void generateT1Connections() {
        ShadowNode start, left, right;
        Polygon tempPoly;
        Point2D leftP, rightP;
        ArrayList<Point2D> tempPoints;




        for (int i = 0; i < Nodes.size(); i++) {
            start = Nodes.get(i);


            if (start.prev == null || start.next == null) {
                //System.out.println("Entered");
                tempPoints = getAdjacentPoints(start.getPosition(), allPolygons);
                if (tempPoints.size() == 0) {
                    System.exit(234567);
                } else {
                    leftP = tempPoints.get(0);
                    rightP = tempPoints.get(1);
                    for (int j = 0; j < Nodes.size(); j++) {
                        ShadowNode node = Nodes.get(j);
                        if (node.getPosition().getX() == leftP.getX() && node.getPosition().getY() == leftP.getY()) {
                            start.prev = node;
                        } else if (node.getPosition() == rightP) {
                            node.next = start;
                        }
                        else if(node.getPosition().getX() == rightP.getX() && node.getPosition().getY() == rightP.getY())   {
                            start.next = node;
                            node.prev = start;
                        }
                    }

                }
            }

        }


    }


    public void printGraph()    {
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
                System.out.println("Prev = " + start.prev + "\tNext = " + start.next);
                System.out.println("Link left = " + start.prev.next);
            }*/

            if(printed.size() == 0 || !printed.contains(start)) {

                if(start.next != null || start.prev != null)   {
                    //get to start
                    tmp = start.prev;
                    boolean cycle = false;

                    if(tmp != null) {
                        tmpX = tmp.getPosition().getX();
                        tmpY = tmp.getPosition().getY();
                        while (tmp.prev != null) {
                            if (tmpX == sX && tmpY == sY) {
                                System.out.println("Cycle detected - aka entire Obstacle in Shadow");
                                cycle = true;
                                break;
                            }
                            tmp = tmp.prev;
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

                    System.out.println("Beginning: " + start2);
                    printed.add(start2);

                    tmp = start2.next;
                    tmpX = tmp.getPosition().getX();
                    tmpY = tmp.getPosition().getY();


                    while((tmpX != s2X || tmpY != s2Y) && tmp != null)    {

                        printed.add(tmp);
                        if(cycle && printed.get(printed.size()-1).next == null) {
                            start2.prev.setNext(start2);
                        }
                        System.out.println(tmp);
                        tmp = tmp.next;
                        if(tmp != null) {
                            tmpX = tmp.getPosition().getX();
                            tmpY = tmp.getPosition().getY();
                        }
                    }


                    if(tmpX == s2X && tmpY == s2Y)   {
                        //System.out.println(tmp);
                        printed.add(tmp);
                    }

                    System.out.println("End hit");
                    System.out.println("\n");

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
    public ArrayList<ShadowNode> calcualteType3()    {

        double agentX, agentY, pointX, pointY;
        shadowPursuit.ShadowNode tmp;
        Line tmpLine, Ray;

        //For every agent that  has a straightöline visibility to a point
        for(Point2D agent : agents)   {
            agentX = agent.getX();
            agentY = agent.getY();

            //For every point of Type2
            for(int i =0; i < Nodes.size(); i++)    {
                tmp = Nodes.get(i);
                if(tmp.getType() == 2) {
                    pointX = tmp.getPosition().getX();
                    pointY = tmp.getPosition().getY();
                    tmpLine = new Line(agentX, agentY, pointX, pointY);

                    if(isVisible(agentX, agentY, pointX, pointY, polygonEdges)) {

                        //Create occlusion Ray
                        Ray = occRay(agent, tmp);



                    }


                }
            }

        }
        return null;


    }

    public Point2D getT3Intersect(Line ray)    {
        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for(Line intLine : polygonEdges)    {
            if(lineIntersect(intLine, ray)) {
                intersectPoints.add(pointIntersect(intLine, ray));
            }
        }

        double min = Double.MAX_VALUE;
        for(int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            tmpLine = new Line(ray.getStartX(), ray.getStartY(), tmpPoint.getX(), tmpPoint.getY());
            dist = Math.sqrt(Math.pow((tmpLine.getEndX() - tmpLine.getStartX()), 2) + Math.pow((tmpLine.getEndY() - tmpLine.getStartY()), 2));

            if(dist < min)  {
                minPos = i;
            }

        }
        return intersectPoints.get(minPos);
    }


}
