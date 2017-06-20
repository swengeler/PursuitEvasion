package shadowPursuit;

import entities.Tree;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;


/**
 * Created by jonty on 16/06/2017.
 */
public class TreeNode {

    ArrayList<Point2D> agentPositions;
    ArrayList<WayPoint> possiblePoints;
    ArrayList<WayPoint> currPositions;
    ArrayList<Polygon> contShadows, clearedShad;
    final int penalty = -100;
    final int clearedScore = 1;
    final int multiplyer = 20;


    int score;


    ArrayList<TreeNode> children;
    ArrayList<Polygon> currShadows;
    TreeNode parent;



    //root
    public TreeNode(ArrayList<WayPoint> currLocations, ArrayList<Polygon> contaminated){
        this.parent = null;
        this.children = new ArrayList<>();
        this.currPositions=currLocations;
        this.contShadows = contaminated;
        this.clearedShad = null;
    }

    public TreeNode(TreeNode parent, ArrayList<WayPoint> currLocations, ArrayList<Polygon> newShadows){
        this.parent = parent;
        this.currPositions=currLocations;
        clearedShad=null;
        contShadows= new ArrayList<>();

        Polygon oldShad;
        Point2D tmpPoint, tmpPoint2;
        clearedShad = new ArrayList<>();

        for(int i = 0; i < parent.getContShadows().size(); i++)  {
            oldShad = parent.getContShadows().get(i);
            for(Polygon newShad : newShadows)   {
                boolean contaminated=false;
                for(int j = 0;  j < newShad.getPoints().size(); j+=2)  {
                    tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j+1));
                    if(oldShad.contains(tmpPoint) && newShad!=null && contShadows.contains(newShad)==false)  {
                        contShadows.add(newShad);
                        contaminated=true;

                    }
                }

                for(int k = 0; k < oldShad.getPoints().size(); k+=2)   {
                    tmpPoint2 = new Point2D(oldShad.getPoints().get(k), oldShad.getPoints().get(k+1));
                    if(newShad.contains(tmpPoint2)&& contaminated==false&& newShad!=null && contShadows.contains(newShad)==false) {
                      contShadows.add(newShad);
                        contaminated=true;
                    }
                }
                if(contaminated==false && newShad != null && contShadows.contains(newShad)==false){
                    System.out.print("SIZE => " + clearedShad.size());
                    clearedShad.add(newShad);

                }
            }
        }

        int Recontaminations=0;
        if(parent.clearedShad != null &&  parent.clearedShad.size() > 0) {
            for (int i = 0; i < parent.getClearedShadows().size(); i++) {
                oldShad = parent.getClearedShadows().get(i);
                for (Polygon newShad : contShadows) {
                    boolean contaminated = false;
                    for (int j = 0; j < newShad.getPoints().size(); j += 2) {
                        tmpPoint = new Point2D(newShad.getPoints().get(j), newShad.getPoints().get(j + 1));
                        if (oldShad.contains(tmpPoint) ) {
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
        score = Recontaminations * penalty - contShadows.size()*multiplyer;
        System.out.println("score= " + score);

        for(int i=0; i<contShadows.size();i++){
            System.out.println("cont shadow = " + i + " = " + contShadows.get(i));
        }
        //score = -Recontaminations;



    }


    public String toString()    {
        return new String("---------------------------------\nTreeNode with positions => " + getWayPoints().get(0).getCoord() + "\nAND SCORE => " + score + "\n---------------------------------\n");
    }



    public void addChild(TreeNode child){
        if(children == null)    {
            children = new ArrayList<>();
        }
        children.add(child);
    }



    public void addParent(TreeNode child,TreeNode parent){
        child.parent=parent;
    }

    public ArrayList<Polygon> getContShadows(){
        return contShadows;
    }

    public ArrayList<Polygon> getClearedShadows(){
        return clearedShad;
    }

    public ArrayList<WayPoint> getWayPoints()   {
        return currPositions;
    }



}
