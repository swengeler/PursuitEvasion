package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import maps.MapRepresentation;

import java.util.ArrayList;
import java.util.List;

import static shadowPursuit.PolyConverter.polygonArea;
import static shadowPursuit.shadowOperations.polyToPoints;
import static shadowPursuit.shadowOperations.polygonContain;


/**
 * Created by jonty on 16/06/2017.
 */
public class TreeNode {

    ArrayList<WayPoint> currPositions;
    ArrayList<Polygon> contShadows, clearedShad;
    final int penalty = 100;
    final int clearedScore = 1;
    final int multiplyer = 20;
    String retString;
    MapRepresentation map;
    boolean gameover = false;

    ArrayList<int[]> allCom;

    int depth;


    double score;


    ArrayList<TreeNode> children;
    ArrayList<Polygon> currShadows;
    TreeNode parent;


    //root
    public TreeNode(ArrayList<WayPoint> currLocations, MapRepresentation map, ArrayList<Polygon> contaminated) {
        this.parent = null;
        this.children = new ArrayList<>();
        this.currPositions = currLocations;
        this.contShadows = contaminated;
        this.clearedShad = new ArrayList<>();
        depth = 1;
        createString();
        this.map = map;

        double area = 0;
        for (Polygon poly : contShadows) {
            area += polygonArea(polyToPoints(poly));

        }
        score = contShadows.size() * multiplyer + area;
        if (score == 0) {
            score = contaminated.size() * multiplyer + area;
        }

        //System.out.println("I HAVE " + contaminated.size() + " shadows");
    }


    public TreeNode(TreeNode parent, ArrayList<WayPoint> currLocations, ArrayList<Polygon> newShadows) {
        this.parent = parent;
        this.currPositions = currLocations;
        contShadows = new ArrayList<>();

        Polygon oldShad;
        Point2D tmpPoint, tmpPoint2;
        clearedShad = new ArrayList<>();
        this.depth = parent.getDepth() + 1;
        this.map = parent.getMap();

        if (newShadows != null) {
            for (Polygon newShad : newShadows) {
                boolean contaminated = false;
                for (int i = 0; i < parent.getContShadows().size(); i++) {
                    oldShad = parent.getContShadows().get(i);


                    for (int j = 0; j < newShad.getPoints().size(); j += 2) {
                        tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j + 1));
                        if (newShad != null && polygonContain(oldShad, newShad) && contaminated == false) {

                            contaminated = true;

                        }
                    }

                    for (int k = 0; k < oldShad.getPoints().size(); k += 2) {
                        tmpPoint2 = new Point2D(oldShad.getPoints().get(k), oldShad.getPoints().get(k + 1));
                        if (contaminated == false && newShad != null && polygonContain(oldShad, newShad)) {

                            contaminated = true;
                        }
                    }
                }
                if (contaminated == true && newShad != null) {
                    contShadows.add(newShad);
                }
                if (contaminated == false && newShad != null) {


                    if (!clearedShad.contains(newShad)) {

                        clearedShad.add(newShad);
                    }
                }

            }

            int Recontaminations = 0;
            if (parent.clearedShad != null && parent.clearedShad.size() > 0) {
                for (int i = 0; i < parent.getClearedShadows().size(); i++) {
                    oldShad = parent.getClearedShadows().get(i);
                    for (Polygon newShad : contShadows) {
                        boolean recontaminated = false;
                        for (int j = 0; j < newShad.getPoints().size(); j += 2) {
                            //  tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j + 1));
                            if (polygonContain(oldShad, newShad)) {
                                Recontaminations++;
                                recontaminated = true;

                            }
                        }

                        for (int k = 0; k < oldShad.getPoints().size(); k += 2) {
                            tmpPoint2 = new Point2D(oldShad.getPoints().get(k), oldShad.getPoints().get(k + 1));
                            if (polygonContain(oldShad, newShad) && recontaminated == false) {
                                Recontaminations++;
                                recontaminated = true;
                            }
                        }

                    }
                }
            }

            //System.out.println("recont= " + Recontaminations + " number of shadows= " + contShadows.size());
            double size = Integer.MAX_VALUE;
            double area = 0;
            double x = 0;
            Line tmp;
            for (Polygon poly : this.contShadows) {
                x = polygonArea(polyToPoints(poly));
              //  if (area > x) {
                    area += x;
               // }
             //  area+=x;


                size = (polyToPoints(poly).size());


            }

            // System.out.println("here " + area);
            //score = Recontaminations * penalty + contShadows.size() * multiplyer + size/10;
            //score = contShadows.size() + area / 10000000 +Recontaminations*penalty;
                score = (contShadows.size() + area / 10000000);
            // System.out.println("score= " + score);
            /*
            for (int i = 0; i < contShadows.size(); i++) {
                System.out.println("cont shadow = " + i + " = " + contShadows.get(i));
            }
            */
        }

        if (newShadows.size() == 0) {
            System.out.println("Hold on");
            System.out.println("For environment");
            for (Double point : map.getBorderPolygon().getPoints()) {
                System.out.print(point + ", ");
            }

            System.out.println("\nAt Points");
            for (WayPoint point : currLocations) {
                System.out.println(point.getCoord());
            }

        }

        //score = -Recontaminations;
        createString();


    }


    public int getDepth() {
        return depth;
    }

    public boolean sameLocations(ArrayList<WayPoint> parent, ArrayList<WayPoint> child) {

        int count = 0;
        for (WayPoint a : parent) {
            count = 0;
            for (WayPoint b : child) {
                if (a.coordinate == b.coordinate) {
                    count++;

                }
            }
            if (count == parent.size()) {
                return true;
            }
        }
        return false;
    }


    public void addChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }

        List<List<WayPoint>> listy = new ArrayList<>();
        for (WayPoint l : this.getWayPoints()) {
            listy.add(l.getConnected());
        }


        List<List<WayPoint>> result = cartesianProduct(listy);


        for (int i = 1; i < result.size(); i++) {
            List<WayPoint> list1 = result.get(i);
             ArrayList<WayPoint> wayPL = new ArrayList<>();

                for (WayPoint wayP : list1) {
                    wayPL.add(wayP);
                }

                //root = new TreeNode(startPoints, new ShadowGraph(map, startPoints, true).getShadows());

                if (this.getMap() == null) {
                    System.out.println("Map Issue for: " + this);
                    System.exit(76);
                }
                ShadowGraph shad = new ShadowGraph(this.getMap(), wayPL, true);
                addChild(new TreeNode(this, wayPL, shad.getShadows()));

        }

        /*
        System.out.println("For this node => " + this);
        System.out.println("CHILDREN:");
        for (TreeNode child : children) {
            System.out.println("new child");
            System.out.print(child);
        }
        System.out.println("-------------------------");
        */

    }

    protected static List<List<WayPoint>> cartesianProduct(List<List<WayPoint>> lists) {
        List<List<WayPoint>> resultLists = new ArrayList<List<WayPoint>>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<WayPoint>());
            return resultLists;
        } else {
            List<WayPoint> firstList = lists.get(0);
            List<List<WayPoint>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (WayPoint condition : firstList) {
                for (List<WayPoint> remainingList : remainingLists) {
                    ArrayList<WayPoint> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }


    public void addParent(TreeNode child, TreeNode parent) {
        child.parent = parent;
    }

    public void addChild(TreeNode node) {
        if(node.getWayPoints() != this.getWayPoints()) {
            children.add(node);
            createString();
        }
    }

    public ArrayList<Polygon> getContShadows() {
        return contShadows;
    }

    public ArrayList<Polygon> getClearedShadows() {
        return clearedShad;
    }

    public ArrayList<WayPoint> getWayPoints() {
        return currPositions;
    }

    public void createString() {
        StringBuilder stringB = new StringBuilder();
        stringB.append("TreeNode with depth = " + depth);
        stringB.append("\nHas: " + contShadows.size() + " contaminated shadows");
        stringB.append("\nWaypoints: ");

        for (WayPoint wayP : currPositions) {
            stringB.append("\n" + wayP.getCoord() + " Connections: " + wayP.getConnected().size());
        }
        stringB.append("\nNumber of CLEAR-Shadows => " + clearedShad.size());
        stringB.append("\nNumber of CONT-Shadows => " + contShadows.size());
        if (children != null) {
            stringB.append("\nNumber of Children: " + children.size());
        }

        /*
        if (parent == null) {
            stringB.append("\nFOR ROOT");
            stringB.append("\nContaminated Shadows");
            for (int k = 0; k < contShadows.size(); k++) {
                stringB.append("\nContaminated Shadow number: " + (k + 1));
                for (Point2D point : polyToPoints(contShadows.get(k))) {
                    stringB.append("\n" + point);
                }
                stringB.append("\n-----------------------\n");
            }
        }
        */

        stringB.append("\n\tScore => " + this.getScore());
        stringB.append("\n---------------\n");
        retString = stringB.toString();
        if (contShadows.size() == 0) {
            gameover = true;
        }
    }

    public void setWayPoints(ArrayList<WayPoint> points) {
        this.currPositions = new ArrayList<>();

        //System.out.println("Given = " + points);
        this.currPositions = points;
        createString();
    }

    public String toString() {

        return retString;
    }

    public MapRepresentation getMap() {
        return map;
    }

    public ArrayList<TreeNode> getchildren() {
        if (children != null) {
            return children;
        } else
            return null;
    }

    public double getScore() {
        return score;
    }

    public TreeNode getParent() {
        if (parent != null) {
            return parent;
        } else {
            return null;
        }

    }


}
