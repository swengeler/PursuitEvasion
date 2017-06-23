package shadowPursuit;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;


/**
 * Created by jonty on 16/06/2017.
 */
public class TreeNode {

    ArrayList<WayPoint> currPositions;
    ArrayList<Polygon> contShadows, clearedShad;
    final int penalty = -100;
    final int clearedScore = 1;
    final int multiplyer = 20;
    String retString;

    ArrayList<int[]> allCom;

    int depth;


    int score;


    ArrayList<TreeNode> children;
    ArrayList<Polygon> currShadows;
    TreeNode parent;


    //TestNode
    public TreeNode(ArrayList<WayPoint> currLocations) {
        this.parent = null;
        this.children = new ArrayList<>();
        this.currPositions = currLocations;

        this.contShadows = null;

        this.clearedShad = null;
        depth = 1;
        createString();

    }

    //root
    public TreeNode(ArrayList<WayPoint> currLocations, ArrayList<Polygon> contaminated) {
        this.parent = null;
        this.children = new ArrayList<>();
        this.currPositions = currLocations;
        this.contShadows = contaminated;
        this.clearedShad = null;
        depth = 1;
        createString();
    }

    public TreeNode(TreeNode parent, ArrayList<WayPoint> currLocations, ArrayList<Polygon> newShadows) {
        this.parent = parent;
        this.currPositions = currLocations;
        clearedShad = null;
        contShadows = new ArrayList<>();

        Polygon oldShad;
        Point2D tmpPoint, tmpPoint2;
        clearedShad = new ArrayList<>();
        this.depth = parent.getDepth() + 1;

        if(newShadows != null) {
            for (int i = 0; i < parent.getContShadows().size(); i++) {
                oldShad = parent.getContShadows().get(i);
                for (Polygon newShad : newShadows) {
                    boolean contaminated = false;
                    for (int j = 0; j < newShad.getPoints().size(); j += 2) {
                        tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j + 1));
                        if (oldShad.contains(tmpPoint) && newShad != null && contShadows.contains(newShad) == false) {
                            contShadows.add(newShad);
                            contaminated = true;

                        }
                    }

                    for (int k = 0; k < oldShad.getPoints().size(); k += 2) {
                        tmpPoint2 = new Point2D(oldShad.getPoints().get(k), oldShad.getPoints().get(k + 1));
                        if (newShad.contains(tmpPoint2) && contaminated == false && newShad != null && contShadows.contains(newShad) == false) {
                            contShadows.add(newShad);
                            contaminated = true;
                        }
                    }
                    if (contaminated == false && newShad != null && contShadows.contains(newShad) == false) {
                        System.out.print("SIZE => " + clearedShad.size());
                        clearedShad.add(newShad);

                    }
                }
            }

            int Recontaminations = 0;
            if (parent.clearedShad != null && parent.clearedShad.size() > 0) {
                for (int i = 0; i < parent.getClearedShadows().size(); i++) {
                    oldShad = parent.getClearedShadows().get(i);
                    for (Polygon newShad : contShadows) {
                        boolean contaminated = false;
                        for (int j = 0; j < newShad.getPoints().size(); j += 2) {
                            tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j + 1));
                            if (oldShad.contains(tmpPoint)) {
                                Recontaminations++;
                                contaminated = true;

                            }
                        }

                        for (int k = 0; k < oldShad.getPoints().size(); k += 2) {
                            tmpPoint2 = new Point2D(oldShad.getPoints().get(k), oldShad.getPoints().get(k + 1));
                            if (newShad.contains(tmpPoint2) && contaminated == false) {
                                Recontaminations++;
                                contaminated = true;
                            }
                        }

                    }
                }
            }

            System.out.println("recont= " + Recontaminations + " number of shadows= " + contShadows.size());
            score = Recontaminations * penalty - contShadows.size() * multiplyer;
            System.out.println("score= " + score);

            for (int i = 0; i < contShadows.size(); i++) {
                System.out.println("cont shadow = " + i + " = " + contShadows.get(i));
            }
        }
        //score = -Recontaminations;
        createString();


    }

    public int getDepth() {
        return depth;
    }


    public void addChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        ArrayList<ArrayList<WayPoint>> test = permute(this.currPositions);
        System.out.println("children size= " + test.size());


        for (int i = 0; i < test.size(); i++) {
            for(int j=0; j<test.get(i).size();j++){
                System.out.println("permute object =" + i + " j= " + j + " child = " + test.get(i).get(j));
            }


            addChild(new TreeNode(this,test.get(i), null));
        }

        System.out.println("CHILDREN:");
        for (TreeNode child : children) {
            System.out.println("new child");
            System.out.print(child);
        }

    }




    public ArrayList<ArrayList<WayPoint>> permute(ArrayList<WayPoint> currLocations) {
        System.out.println("\n\nINPUT");
        for(WayPoint wayP : currLocations)  {
            System.out.println(wayP.getCoord());
            for(WayPoint wayPP : wayP.getConnected())  {
                System.out.println("\t" + wayPP.getCoord());
            }
            System.out.println();
        }




        ArrayList<ArrayList<WayPoint>> b;
        ArrayList<ArrayList<WayPoint>> a;
        a = new ArrayList<>();
        b = new ArrayList<>();
        ArrayList<WayPoint> c = new ArrayList<>();
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        int count5 = 0;
        int count6 = 0;



        for (int i = 0; i < currLocations.get(0).getConnected().size(); i++) {
            count1++;
            //    System.out.println(a.size());
            //    System.out.println(a.get(i).size());
            //   if(a.get(i) == null)    {
            a.add(new ArrayList<>());
            //  a.set(i, new ArrayList<>());
            //    }
            a.get(i).add(currLocations.get(0).getConnected().get(i));

            System.out.println("\n\tA= " + currLocations.get(0).getConnected().get(i).getCoord() + " at point= " + i);


        }





        for (int i = 1; i < currLocations.size(); i++) {
            count2++;
            for (int j = 0; j < a.size(); j++) {
                count6++;
                for (int k = 0; k < currLocations.get(i).getConnected().size(); k++) {
                    count3++;
                    int number= (j * currLocations.get(i).getConnected().size()) + k;
                   //
                    // b.add(new ArrayList<>());
                  /*  for(int q=0; q<a.get(j).size();q++){
                        b.get(count3).add(a.get(j).get(q));
                    }
                    */
                    System.out.println("-------------jonty here--------------");
                    System.out.println("number= " + number);
                    System.out.println(currLocations.get(i).getConnected().get(k).getCoordinate());
                    b.add(a.get(j));
                    //b.set(number, a.get(j));
                    System.out.println("BEFORE => ");
                    for(int g = 0; g < b.get(number).size(); g++)   {
                        System.out.println(b.get(number).get(g).getCoord());
                    }
                    System.out.println("----------");

                    if (!b.get(number).contains(currLocations.get(i).getConnected().get(k)) && b.get(number).size() < this.getWayPoints().size()) {

                       // b.get(number).set(number,currLocations.get(i).getConnected().get(k));
                        b.get(number).add(i,currLocations.get(i).getConnected().get(k));
                    }else if(!b.get(number).contains((currLocations.get(i).getConnected().get(k)))){

                        b.get(number).remove(b.get(number).size()-1);
                        b.get(number).add(i,currLocations.get(i).getConnected().get(k));


                    }else System.exit(1111);

                    System.out.println("AFTER => ");
                    for(int g = 0; g < b.get(number).size(); g++)   {
                        System.out.println(b.get(number).get(g).getCoord());
                    }
                    System.out.println("---------");



                }

            }  a=b;
            b = null;



        }

        System.out.println("size= " + a.size());
        System.out.println("count1= " + count1);
        System.out.println("count2= " + count2);
        System.out.println("count6= " + count6);
        System.out.println("count3= " + count3);
        System.out.println("count4= " + count4);
        System.out.println("count5= " + count5);


        return a;
    }


    public void addParent(TreeNode child, TreeNode parent) {
        child.parent = parent;
    }

    public void addChild(TreeNode node) {
        children.add(node);
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
        stringB.append("\nWaypoints: ");

        for (WayPoint wayP : currPositions) {
            stringB.append("\n" + wayP.getCoord() + " Connections: " + wayP.getConnected().size());
        }
        stringB.append("\n---------------\n");
        retString = stringB.toString();
    }

    public void setWayPoints(ArrayList<WayPoint> points) {
        this.currPositions = new ArrayList<>();

        System.out.println("Given = " + points);
        this.currPositions = points;
        createString();
    }

    public String toString() {
        StringBuilder stringB = new StringBuilder();
        stringB.append("TreeNode with depth = " + depth);
        stringB.append("\nWaypoints: ");

        for (WayPoint wayP : currPositions) {
            stringB.append("\n" + wayP.getCoord() + " Connections: " + wayP.getConnected().size());
        }
        return stringB.toString();
    }

}
