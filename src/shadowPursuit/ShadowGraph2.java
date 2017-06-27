package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;
import simulation.Agent;

import java.util.ArrayList;

import static shadowPursuit.shadowOperations.*;
import static shadowPursuit.shadowOperations.findIntersect2;

/**
 * Created by I6106804 on 27-6-2017.
 */
public class ShadowGraph2 {

    private Polygon environment;
    private ArrayList<Polygon> obstacles;
    private ArrayList<Polygon> allPolygons;
    public ArrayList<Line> polyEdges;

    private ArrayList<Point2D> agents;

    private ArrayList<Point2D> t1;
    private ArrayList<Point2D> t2;
    private ArrayList<Point2D> checkT3;
    private ArrayList<Point2D> checkT4;

    private ArrayList<Point2D> pointy;


    private ArrayList<Line> T3Rays = new ArrayList<>();
    private ArrayList<Line> T4Rays = new ArrayList<>();

    public ShadowGraph2(MapRepresentation map, ArrayList<Point2D> agents)   {
        environment = map.getBorderPolygon();
        obstacles = map.getObstaclePolygons();
        allPolygons = map.getAllPolygons();
        polyEdges = map.getPolygonEdges();

        this.agents = agents;

        t1 = new ArrayList<>();
        t2 = new ArrayList<>();
        checkT3 = new ArrayList<>();
        checkT4 = new ArrayList<>();

        pointy = new ArrayList<>();

        System.out.println("Amount of Polyedges" + polyEdges.size());
        for(Line l : polyEdges) {
            System.out.println("Line: " + l);
        }

        computeT1();
        computeT2();
        computeT3();
        computeT4();
    }

    public void computeT1() {
        t1 = getX1Points(environment, obstacles, agents);
    }

    public void computeT2() {
        t2 = findReflex(environment, allPolygons, obstacles);
        for(int i = 0; i < t2.size(); i++) {
            Point2D tmp = t2.get(i);
            if(t1.contains(tmp)) {
                t2.remove(i);
                i--;
            }
        }
    }

    public void computeT3() {

        ArrayList<Point2D> allPoints = polyToPoints(allPolygons);
        Point2D tmpPoint, posIntersect;

        double maxYDist, maxXDist, maxY, maxX, minX, minY, rayLength;

        maxYDist = 0;
        maxXDist = 0;

        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;

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
        //System.out.println("checking maxy= " + maxYDist);
        //System.out.println("checking maxx= " + maxXDist);
        if (maxYDist < maxXDist) {
            rayLength = maxXDist * 2;
        } else {
            rayLength = maxYDist * 2;
        }

        for(Point2D tmp : t2)   {
            for(Point2D agent : agents) {
                if (isVisible(tmp.getX(), tmp.getY(), agent.getX(), agent.getY(), polyEdges))   {
                    Line tmpLine = scaleRay(agent, tmp, rayLength);
                    posIntersect = getT3Intersect(tmpLine);
                    checkT3.add(posIntersect);
                    T3Rays.add(tmpLine);
                }
            }
        }

        Point2D tempPoint;
        int counter;

        for(int i = 0; i < checkT3.size(); i++ )    {
            counter = 0;
            tempPoint = checkT3.get(i);
            for(Point2D agent : agents) {
                if(i < checkT3.size() && checkT3.get(i) != null) {
                    if (isVisible(agent.getX(), agent.getY(), tempPoint.getX(), tempPoint.getY(), polyEdges)) {
                        counter++;
                    }
                    if (counter >= 2) {
                        //Also remove the corresponding ray

                        checkT3.remove(i);
                        i--;
                        counter = 0;
                        break;
                    }
                }
            }
        }

    }

    public void computeT4() {
        Line ray1, ray2;
        if(T3Rays != null && T3Rays.size() > 0) {
            for(int i = 0; i < T3Rays.size(); i++)  {
                ray1 = T3Rays.get(i);
                for(int j = 0; j < T3Rays.size(); j++)  {
                    if(i != j)  {
                        ray2 = T3Rays.get(j);
                        if(lineIntersect(ray1, ray2))   {
                            checkT4.add(findIntersect2(ray1, ray2));
                            T4Rays.add(ray1);
                            T4Rays.add(ray2);
                        }
                    }
                }
            }
        }
    }



    public Point2D getT3Intersect(Line ray) {
        //System.out.println("Passed ray = " + ray);

        ArrayList<Point2D> intersectPoints = new ArrayList<>();
        Line tmpLine;
        Point2D tmpPoint;
        double dist = 0;
        int minPos = 0;

        for (Line inLine : polyEdges) {
            if (lineIntersect(inLine, ray)) {
                //System.out.println("INTERSECT DETECTED at = " + findIntersect2(inLine, ray));
                if (findIntersect2(inLine, ray) != null) {

                    checkT3.add(findIntersect2(inLine, ray));
                }
            }
        }

        int count = 0;

        double min = Double.MAX_VALUE;
        for (int i = 0; i < intersectPoints.size(); i++) {
            tmpPoint = intersectPoints.get(i);
            //TODO @ROBIN IF STUFF FUCKS UP CHECK THIS CONDITION AGAIN
            if (tmpPoint != null) {
                //System.out.println("ray x= " + ray.getStartX() + " y= " + ray.getStartY());
                //System.out.println("tmp x= " + tmpPoint.getX() + " y= " + tmpPoint.getY());
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


    public ArrayList<Point2D> getT1()   {
        return t1;
    }

    public ArrayList<Point2D> getT2()   {
        return t2;
    }

    public ArrayList<Point2D> getT3()   {
        return checkT3;
    }

    public ArrayList<Point2D> getT4()   {
        return checkT4;
    }

    public ArrayList<Line> getT4Rays()   {
        return T4Rays;
    }
}
